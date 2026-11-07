"""Google Classroom: официальный REST API с OAuth-токеном ученика.

Вход через Google делает бэкенд (у него секрет клиента) и сам обновляет access_token по refresh_token,
сюда приходит уже живой токен. Берём активные курсы → задания курса → свои сдачи.
Документация: https://developers.google.com/workspace/classroom/reference/rest
"""
from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone

import httpx

from ..models import Assignment, ClassroomCreds, SourceError, open_status, parse_dt, utcnow

API = "https://classroom.googleapis.com/v1"
DAYS_BACK = 30          # задания со сроком старше месяца не показываем
NO_DUE_DAYS_BACK = 90   # задания без срока — если менялись за последние 3 месяца
MAX_COURSES = 60
KINDS = {"ASSIGNMENT": "assignment", "SHORT_ANSWER_QUESTION": "quiz", "MULTIPLE_CHOICE_QUESTION": "quiz"}


def fetch(client: httpx.Client, creds: ClassroomCreds):
    if not creds.access_token:
        raise SourceError("Войдите в Google Classroom заново", needs_reconnect=True)
    headers = {"Authorization": f"Bearer {creds.access_token}"}
    courses = _pages(client, f"{API}/courses", {"studentId": "me", "courseStates": "ACTIVE"}, "courses", headers,
                     required=True)
    # курсов бывает 30+: по 2 запроса на курс параллельно, иначе сбор идёт полминуты
    with ThreadPoolExecutor(max_workers=8) as pool:
        per_course = list(pool.map(lambda c: _course(client, c, headers), courses[:MAX_COURSES]))
    return [a for items in per_course for a in items], None


def _course(client: httpx.Client, course: dict, headers: dict) -> list[Assignment]:
    cid = course["id"]
    works = _pages(client, f"{API}/courses/{cid}/courseWork", {"courseWorkStates": "PUBLISHED"}, "courseWork", headers)
    if not works:
        return []
    subs = _pages(client, f"{API}/courses/{cid}/courseWork/-/studentSubmissions", {"userId": "me"},
                  "studentSubmissions", headers)
    mine = {s.get("courseWorkId"): s for s in subs}
    now = utcnow()
    out = []
    for w in works:
        a = normalize(w, course, mine.get(w.get("id")))
        changed = parse_dt(w.get("updateTime")) or now
        if (a.due and a.due >= now - timedelta(days=DAYS_BACK)) or (not a.due and changed >= now - timedelta(days=NO_DUE_DAYS_BACK)):
            out.append(a)
    return out


def _pages(client: httpx.Client, url: str, params: dict, key: str, headers: dict, required: bool = False) -> list[dict]:
    items, token = [], None
    while True:
        r = client.get(url, params={**params, "pageSize": 100, **({"pageToken": token} if token else {})}, headers=headers)
        if r.status_code == 401:
            raise SourceError("Google Classroom: войдите заново", needs_reconnect=True)
        if r.status_code in (403, 404):
            if required:
                raise SourceError("Нет доступа к Google Classroom: разрешите доступ к курсам при входе "
                                  "или попросите школу разрешить приложение", needs_reconnect=True)
            return items   # курс, где у ученика нет прав на задания, просто пропускаем
        if r.status_code >= 500:
            raise SourceError(f"Google Classroom не отвечает (HTTP {r.status_code}), попробуйте позже")
        r.raise_for_status()
        try:
            data = r.json()
        except ValueError:
            data = None
        if not isinstance(data, dict):
            raise SourceError("Google Classroom прислал непонятный ответ, попробуйте позже")
        items += data.get(key) or []
        token = data.get("nextPageToken")
        if not token:
            return items


def due_of(work: dict) -> datetime | None:
    """dueDate {year, month, day} + dueTime {hours, minutes} в UTC; без времени — конец дня."""
    d = work.get("dueDate")
    if not d or not d.get("year"):
        return None
    t = work.get("dueTime")
    hours, minutes = (t.get("hours", 0), t.get("minutes", 0)) if t is not None else (23, 59)
    return datetime(d["year"], d["month"], d["day"], hours, minutes, tzinfo=timezone.utc)


def normalize(work: dict, course: dict, sub: dict | None) -> Assignment:
    due = due_of(work)
    state = (sub or {}).get("state")
    if state == "RETURNED" and (sub or {}).get("assignedGrade") is not None:
        status = "graded"
    elif state in ("TURNED_IN", "RETURNED"):
        status = "submitted"
    else:
        status = open_status(due)
    return Assignment(
        source="classroom", id=str(work["id"]), title=work.get("title") or "(без названия)",
        course=course.get("name") or "", due=due, status=status, url=work.get("alternateLink"),
        description=(work.get("description") or "")[:2000], kind=KINDS.get(work.get("workType"), "assignment"),
    )
