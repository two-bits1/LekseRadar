"""Главная точка входа: collect(credentials) собирает ДЗ со всех платформ в один список."""
from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone

import httpx

from . import demo as demo_data
from .models import Assignment, CollectResult, Credentials, SourceError, SourceResult, utcnow
from .sources import canvas, inschool, itslearning, teams

SOURCES = {"canvas": canvas, "teams": teams, "itslearning": itslearning, "inschool": inschool}
STATUS_ORDER = {"overdue": 0, "todo": 1, "submitted": 2, "graded": 3}
FAR = datetime.max.replace(tzinfo=timezone.utc)


def _one(name, creds, client):
    try:
        items, updated = SOURCES[name].fetch(client, creds)
        return name, items, SourceResult(ok=True, count=len(items)), updated
    except SourceError as e:
        return name, [], SourceResult(ok=False, error=str(e), needs_reconnect=e.needs_reconnect), None
    except httpx.HTTPError as e:
        return name, [], SourceResult(ok=False, error=f"{name}: сеть или сервер недоступны ({e})"), None
    except Exception as e:  # одна сломанная платформа не должна ронять всю ленту
        return name, [], SourceResult(ok=False, error=f"{name}: {type(e).__name__}: {e}"), None


def sort_key(a: Assignment):
    return (STATUS_ORDER[a.status], a.due or FAR)


def collect(credentials: Credentials | dict | None = None, *, demo: bool | None = None,
            client: httpx.Client | None = None) -> CollectResult:
    """credentials — учётные данные платформ (что есть). demo=True — для платформ без данных
    подставить демо-задания. Возвращает общий список (сначала просроченные, потом по дедлайну),
    статус каждого источника и обновлённые токены, которые бэкенд должен сохранить."""
    creds = Credentials.model_validate(credentials or {})
    use_demo = creds.demo if demo is None else demo
    own = client is None
    client = client or httpx.Client(timeout=30, follow_redirects=False)
    assignments, sources, updated = [], {}, {}
    try:
        live = [(n, getattr(creds, n)) for n in SOURCES if getattr(creds, n) is not None]
        with ThreadPoolExecutor(max_workers=4) as pool:
            for name, items, res, upd in pool.map(lambda x: _one(x[0], x[1], client), live):
                assignments += items
                sources[name] = res
                if upd:
                    updated[name] = upd
    finally:
        if own:
            client.close()
    if use_demo:
        for name in SOURCES:
            if name not in sources:
                items = demo_data.demo(name)
                assignments += items
                sources[name] = SourceResult(ok=True, count=len(items), demo=True)
    assignments.sort(key=sort_key)
    return CollectResult(generated_at=utcnow(), assignments=assignments, sources=sources,
                         updated_credentials=updated)
