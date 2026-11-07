"""Canvas LMS: официальный REST API с личным токеном студента.

Токен: Canvas → Account → Settings → «+ New Access Token». Если кнопки нет, школа запретила токены.
Берём planner (то же, что «To Do» в Canvas): задания, тесты, обсуждения с дедлайнами и статусом сдачи.
"""
from __future__ import annotations

from datetime import timedelta

import httpx

from ..models import Assignment, CanvasCreds, SourceError, open_status, parse_dt, utcnow

DAYS_BACK = 14
DAYS_AHEAD = 60
KINDS = {"assignment": "assignment", "quiz": "quiz", "discussion_topic": "discussion",
         "wiki_page": "task", "planner_note": "task", "calendar_event": "task"}


def fetch(client: httpx.Client, creds: CanvasCreds):
    base = creds.base_url.rstrip("/")
    today = utcnow().date()
    url = f"{base}/api/v1/planner/items"
    params = {"start_date": str(today - timedelta(days=DAYS_BACK)),
              "end_date": str(today + timedelta(days=DAYS_AHEAD)), "per_page": 100}
    items = []
    while url:
        r = client.get(url, params=params, headers={"Authorization": f"Bearer {creds.token}"})
        if r.status_code == 401:
            raise SourceError("Canvas не принял токен: создай новый в Account → Settings", needs_reconnect=True)
        r.raise_for_status()
        items += r.json()
        url = r.links.get("next", {}).get("url")
        params = None  # в ссылке next параметры уже есть
    return normalize(items, base), None


def normalize(items: list[dict], base: str) -> list[Assignment]:
    out = []
    for it in items:
        p = it.get("plannable") or {}
        due = parse_dt(p.get("due_at") or p.get("todo_date") or it.get("plannable_date"))
        sub = it.get("submissions") or {}
        if sub.get("graded"):
            status = "graded"
        elif sub.get("submitted") or sub.get("excused") or (it.get("planner_override") or {}).get("marked_complete"):
            status = "submitted"
        else:
            status = open_status(due)
        link = it.get("html_url")
        out.append(Assignment(
            source="canvas", id=str(it.get("plannable_id")), title=p.get("title") or "(без названия)",
            course=it.get("context_name") or "", due=due, status=status,
            url=(base + link) if link and link.startswith("/") else link,
            kind=KINDS.get(it.get("plannable_type"), "task"),
        ))
    return out
