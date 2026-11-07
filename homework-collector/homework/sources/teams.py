"""Microsoft Teams Assignments через Microsoft Graph (Education API).

Нужно одно приложение в Entra ID (portal.azure.com → App registrations):
- Supported account types: «Accounts in any organizational directory» (школьные аккаунты);
- Authentication → Allow public client flows = Yes (для входа по коду устройства);
- API permissions (Delegated): EduAssignments.ReadBasic, Team.ReadBasic.All, User.Read, offline_access.
Эти права не требуют согласия администратора по умолчанию, но школа может запретить ученикам
давать согласие приложениям — тогда Microsoft вернёт AADSTS65001/90094.

Вход: start_device_login() → пользователь вводит код на microsoft.com/devicelogin → poll_device_login().
"""
from __future__ import annotations

import os
import time

import httpx

from ..models import Assignment, SourceError, TeamsCreds, open_status, parse_dt

AUTHORITY = "https://login.microsoftonline.com/organizations/oauth2/v2.0"
GRAPH = "https://graph.microsoft.com/v1.0"
SCOPES = "offline_access User.Read EduAssignments.ReadBasic Team.ReadBasic.All"


def client_id(creds: TeamsCreds | None = None) -> str:
    cid = (creds.client_id if creds else None) or os.environ.get("TEAMS_CLIENT_ID")
    if not cid:
        raise SourceError("Не задан client_id приложения Entra (TEAMS_CLIENT_ID)")
    return cid


# ---------- вход ----------

def start_device_login(client: httpx.Client, cid: str) -> dict:
    """Возвращает user_code, verification_uri, message (показать пользователю) и device_code."""
    r = client.post(f"{AUTHORITY}/devicecode", data={"client_id": cid, "scope": SCOPES})
    data = r.json()
    if "device_code" not in data:
        raise SourceError(f"Microsoft отказал: {data.get('error_description') or data.get('error')}")
    return data


def poll_device_login(client: httpx.Client, cid: str, device_code: str) -> dict | None:
    """Один опрос. None — пользователь ещё не вошёл; dict — токены (сохранить как TeamsCreds)."""
    r = client.post(f"{AUTHORITY}/token", data={
        "client_id": cid, "device_code": device_code,
        "grant_type": "urn:ietf:params:oauth:grant-type:device_code"})
    data = r.json()
    if data.get("error") in ("authorization_pending", "slow_down"):
        return None
    if "access_token" not in data:
        raise SourceError(f"Вход не удался: {data.get('error_description') or data.get('error')}")
    return _creds(data, cid)


def _creds(tok: dict, cid: str) -> dict:
    return {"access_token": tok["access_token"], "refresh_token": tok.get("refresh_token"),
            "expires_at": int(time.time()) + int(tok.get("expires_in", 3600)) - 60, "client_id": cid}


def _refresh(client: httpx.Client, creds: TeamsCreds) -> dict:
    cid = client_id(creds)
    r = client.post(f"{AUTHORITY}/token", data={
        "client_id": cid, "grant_type": "refresh_token", "refresh_token": creds.refresh_token, "scope": SCOPES})
    data = r.json()
    if "access_token" not in data:
        raise SourceError("Вход в Microsoft истёк, нужно войти заново", needs_reconnect=True)
    return _creds(data, cid)


# ---------- задания ----------

def fetch(client: httpx.Client, creds: TeamsCreds):
    updated = None
    if not creds.access_token or (creds.expires_at or 0) <= time.time():
        if not creds.refresh_token:
            raise SourceError("Нет токена Microsoft, нужно войти", needs_reconnect=True)
        updated = _refresh(client, creds)
        creds = TeamsCreds(**updated)
    h = {"Authorization": f"Bearer {creds.access_token}"}

    assignments, url = [], f"{GRAPH}/education/me/assignments?$expand=submissions"
    while url:
        r = client.get(url, headers=h)
        if r.status_code == 400 and "$expand" in url:  # если expand не поддержан — без статуса сдачи
            url = f"{GRAPH}/education/me/assignments"
            continue
        if r.status_code == 401:
            raise SourceError("Microsoft не принял токен, нужно войти заново", needs_reconnect=True)
        if r.status_code == 403:
            raise SourceError("Нет доступа к заданиям Teams: школа не разрешила EduAssignments для приложения")
        r.raise_for_status()
        body = r.json()
        assignments += body.get("value", [])
        url = body.get("@odata.nextLink")

    classes = {}
    try:  # названия классов: id класса = id команды в Teams
        r = client.get(f"{GRAPH}/me/joinedTeams?$select=id,displayName", headers=h)
        if r.status_code == 200:
            classes = {t["id"]: t["displayName"] for t in r.json().get("value", [])}
    except httpx.HTTPError:
        pass
    return normalize(assignments, classes), updated


def normalize(items: list[dict], classes: dict[str, str]) -> list[Assignment]:
    out = []
    for a in items:
        due = parse_dt(a.get("dueDateTime"))
        subs = a.get("submissions") or []
        sub = (subs[0].get("status") if subs else None) or ""
        if sub == "returned":
            status = "graded"
        elif sub == "submitted":
            status = "submitted"
        else:
            status = open_status(due)
        out.append(Assignment(
            source="teams", id=a["id"], title=a.get("displayName") or "(без названия)",
            course=classes.get(a.get("classId"), "Teams"), due=due, status=status, url=a.get("webUrl"),
            description=((a.get("instructions") or {}).get("content") or "")[:2000],
        ))
    return out
