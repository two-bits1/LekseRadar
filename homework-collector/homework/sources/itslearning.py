"""itslearning: REST API (restapi/personal/...) с OAuth-токеном.

Ключи разработчика itslearning выдаёт только партнёрам, поэтому вход идёт тем же OAuth-клиентом,
что и мобильное приложение itslearning (так делают открытые клиенты, например ITSDU).
Это неофициальный путь: для MVP работает, но itslearning может его закрыть.

Вход:
1. authorize_url(base_url) → открыть пользователю (там вход через Feide/школу);
2. в конце браузер переходит на itsl-itslearning://login?code=... (страница не откроется — это нормально,
   адрес видно в DevTools → Network или его ловит фронтенд/расширение);
3. exchange_code(base_url, code_или_весь_адрес) → токены. access_token живёт ~1 ч, refresh_token обновляет его.
"""
from __future__ import annotations

import secrets
import time
import urllib.parse

import httpx

from ..models import Assignment, ItslearningCreds, SourceError, open_status, parse_dt

CLIENT_ID = "10ae9d30-1853-48ff-81cb-47b58a325685"  # клиент мобильного приложения itslearning
REDIRECT_URI = "itsl-itslearning://login"
SCOPE = "SCOPE"  # так запрашивает приложение: доступ ко всему, что видит пользователь
PAGE_SIZE = 100


def authorize_url(base_url: str, state: str | None = None) -> str:
    q = urllib.parse.urlencode({"client_id": CLIENT_ID, "state": state or secrets.token_urlsafe(16),
                                "response_type": "code", "scope": SCOPE, "redirect_uri": REDIRECT_URI})
    return f"{base_url.rstrip('/')}/oauth2/authorize.aspx?{q}"


def exchange_code(client: httpx.Client, base_url: str, code_or_url: str) -> dict:
    code = code_or_url
    if "code=" in code_or_url:
        code = urllib.parse.parse_qs(urllib.parse.urlparse(code_or_url).query)["code"][0]
    return _token(client, base_url, {"grant_type": "authorization_code", "code": code})


def _json(r: httpx.Response):
    """Тело ответа как JSON или None, если itslearning прислал HTML-страницу ошибки, обрезанный ответ и т.п."""
    try:
        return r.json()
    except ValueError:
        return None


def _token(client: httpx.Client, base_url: str, form: dict) -> dict:
    r = client.post(f"{base_url.rstrip('/')}/restapi/oauth2/token", data={"client_id": CLIENT_ID, **form})
    data = _json(r) if r.content else {}
    if not isinstance(data, dict):
        if r.status_code >= 500:
            raise SourceError(f"itslearning не отвечает (HTTP {r.status_code}), попробуйте позже")
        raise SourceError("itslearning не выдал токен: проверьте адрес школы и войдите заново",
                          needs_reconnect=True)
    if "access_token" not in data:
        raise SourceError("itslearning не выдал токен, нужно войти заново", needs_reconnect=True)
    return {"base_url": base_url, "access_token": data["access_token"],
            "refresh_token": data.get("refresh_token") or form.get("refresh_token"),
            "expires_at": int(time.time()) + int(data.get("expires_in", 3600)) - 60}


def fetch(client: httpx.Client, creds: ItslearningCreds):
    base = creds.base_url.rstrip("/")
    updated = None
    if not creds.access_token or (creds.expires_at or 0) <= time.time():
        if not creds.refresh_token:
            raise SourceError("Нет токена itslearning, нужно войти", needs_reconnect=True)
        updated = _token(client, base, {"grant_type": "refresh_token", "refresh_token": creds.refresh_token})
        creds = ItslearningCreds(**updated)
    h = {"Authorization": f"Bearer {creds.access_token}"}

    tasks = []
    for status in ("Active", "Completed"):
        page = 0
        while True:
            r = client.get(f"{base}/restapi/personal/tasks/v1", headers=h, params={
                "PageIndex": page, "PageSize": PAGE_SIZE, "status": status, "deadline": "All"})
            if r.status_code == 401:
                raise SourceError("itslearning не принял токен, нужно войти заново", needs_reconnect=True)
            if r.status_code >= 500:
                raise SourceError(f"itslearning не отвечает (HTTP {r.status_code}), попробуйте позже")
            r.raise_for_status()
            body = _json(r)
            if not isinstance(body, dict):
                raise SourceError("itslearning прислал не список заданий: проверьте адрес школы")
            chunk = body.get("EntityArray") or []
            tasks += [dict(t, _completed=(status == "Completed")) for t in chunk]
            if len(chunk) < PAGE_SIZE or (page + 1) * PAGE_SIZE >= body.get("Total", 0):
                break
            page += 1
    return normalize(tasks), updated


def normalize(tasks: list[dict]) -> list[Assignment]:
    out, seen = [], set()
    for t in tasks:
        tid = str(t.get("TaskId"))
        if tid in seen:
            continue
        seen.add(tid)
        due = parse_dt(t.get("Deadline"))
        done = t.get("_completed") or str(t.get("Status", "")).lower() in ("completed", "finished")
        out.append(Assignment(
            source="itslearning", id=tid, title=t.get("Title") or "(без названия)",
            course=t.get("LocationTitle") or "", due=due, status="submitted" if done else open_status(due),
            url=t.get("Url") or t.get("ContentUrl"), description=(t.get("Description") or "")[:2000],
            kind="assignment" if str(t.get("ElementType", "")).lower() == "assignment" else "task",
        ))
    return out
