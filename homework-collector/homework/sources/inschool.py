"""Visma InSchool (норвежские школы): внутренний API веб-приложения по cookie сессии.

Официального API для учеников нет. После входа через ID-porten/Feide браузер получает cookie
(JSESSIONID, Authorization и др.) — их и передаём. Cookie Authorization живёт ~5 минут, сервер
выдаёт новый в Set-Cookie: обновлённые cookie возвращаются вторым значением fetch().
Как домашку берём контрольные и вурдеринги (EXAM, ASSESSMENT) из расписания на WEEKS недель вперёд.
"""
from __future__ import annotations

from datetime import date, datetime, timedelta, timezone
from zoneinfo import ZoneInfo

import httpx

from ..models import Assignment, InschoolCreds, SourceError, open_status

WEEKS = 6
OSLO = ZoneInfo("Europe/Oslo")


def fetch(client: httpx.Client, creds: InschoolCreds):
    base = f"https://{creds.host}"
    jar = dict(creds.cookies)

    def get(path):
        r = client.get(base + path, headers={
            "Cookie": "; ".join(f"{k}={v}" for k, v in jar.items()),
            "Accept": "application/json", "X-Requested-With": "XMLHttpRequest"})
        for k, v in r.cookies.items():
            jar[k] = v
        if r.status_code in (401, 403) or "json" not in r.headers.get("content-type", ""):
            raise SourceError("Сессия InSchool истекла, нужно войти заново", needs_reconnect=True)
        r.raise_for_status()
        return r.json()

    me = get("/control/permissions/user")
    learner = me["learnerId"]
    monday = date.today() - timedelta(days=date.today().weekday())
    items = []
    for i in range(WEEKS):
        week = get(f"/control/timetablev2/learner/{learner}/fetch/ALL/0/current"
                   f"?forWeek={monday + timedelta(weeks=i):%d/%m/%Y}&extra-info=true&types=EXAM,ASSESSMENT")
        items += week.get("timetableItems", []) if isinstance(week, dict) else []
    changed = jar != creds.cookies
    return normalize(items, base), ({"host": creds.host, "cookies": jar} if changed else None)


def _when(d: str, t: str | None):
    try:
        day = datetime.strptime(d, "%d/%m/%Y") if "/" in d else datetime.fromisoformat(d[:10])
    except (TypeError, ValueError):
        return None
    hh, mm = (t or "08:00").split(":")[:2]
    return day.replace(hour=int(hh), minute=int(mm), tzinfo=OSLO).astimezone(timezone.utc)


def normalize(items: list[dict], base: str) -> list[Assignment]:
    out, seen = [], set()
    for i in items:
        key = f"{i.get('entityId') or i.get('id')}-{i.get('date')}"
        if key in seen:
            continue
        seen.add(key)
        due = _when(i.get("date"), i.get("startTime"))
        out.append(Assignment(
            source="inschool", id=key, title=i.get("label") or "Prøve",
            course=i.get("subject") or "", due=due, status=open_status(due), url=f"{base}/",
            description=str(i.get("extraInfo") or i.get("assessment") or "")[:2000],
            kind="exam" if i.get("type") == "EXAM" else "assignment",
        ))
    return out
