# Python и база (№3): что сделать, чтобы всё работало и можно было заливать

Бэкенд (`JavaAPI-setup`) уже умеет Google Classroom: ссылку на вход, обмен кода на токены, шифрование
и **обновление** `access_token` по `refresh_token`. Секрет Google есть только у бэкенда.
Проверено на настоящем аккаунте через сайт из `Frontend-setup`: подключение за ~6 с, 28 заданий из ~30 курсов.

Без пунктов 1–2 сайт на «Logg inn med Google» получает `503` («нужна миграция V22» / «обновите collector»),
остальные платформы работают как раньше.

| # | Что | Обязательно |
|---|---|---|
| 1 | Миграция V22 `classroom` | да |
| 2 | Сборщик Classroom в collector + тест | да |
| 3 | `.gitattributes` (на Windows база создаётся пустой) | да |
| 4 | `127.0.0.1` вместо `localhost` в примерах для Python | да, для Windows |
| 5 | `COLLECTOR-API.md` | желательно |
| 6 | Railway: миграции и пересборка collector | при деплое |

> В `frontend/docs/backend-changes.md` раздел 2.3 предлагает обмен кода и ключи Google в collector
> (`/login/classroom/*`, `GOOGLE_CLIENT_ID` у collector). **Это делать не нужно**: всё это уже делает Java.
> Collector только собирает задания по готовому `access_token`.

---

## 1. `database/migrations/V22__classroom.sql`

```sql
-- Google Classroom как пятая платформа: подключение (platform_connections) и источник ДЗ (assignments.source).
-- PostgreSQL 12+: ADD VALUE можно в транзакции, если новое значение не используется в ней же.
ALTER TYPE platform ADD VALUE IF NOT EXISTS 'classroom';
```

- Больше ничего в эту миграцию не писать.
- Обновить таблицу миграций в `database/README.md` и тип `platform` в `database/ENTITIES.md`.
- Проверка: `SELECT enum_range(NULL::platform);` → в конце `classroom`.

## 2. Сборщик Classroom (`homework-collector/homework`)

### `models.py`

```python
Source = Literal["canvas", "teams", "itslearning", "inschool", "classroom"]

class ClassroomCreds(OAuthCreds):
    """Google Classroom: access_token получает и обновляет бэкенд (у него секрет клиента Google)."""

class Credentials(BaseModel):
    ...
    classroom: Optional[ClassroomCreds] = None
```

### `collect.py`

```python
from .sources import canvas, classroom, inschool, itslearning, teams

SOURCES = {"canvas": canvas, "teams": teams, "itslearning": itslearning, "inschool": inschool, "classroom": classroom}
```

### Новый `sources/classroom.py` (проверен на настоящем Classroom)

```python
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
        r.raise_for_status()
        data = r.json()
        items += data.get(key, [])
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
        description=work.get("description") or "", kind=KINDS.get(work.get("workType"), "assignment"),
    )
```

Что делает:
- только **активные** курсы, архивные пропускаются;
- по каждому курсу 2 запроса (задания + свои сдачи), курсы параллельно в 8 потоков;
- статус: `TURNED_IN` → `submitted`, `RETURNED` с оценкой → `graded`, иначе `todo` / `overdue` по сроку;
- срок `dueDate` + `dueTime` (UTC), без времени — 23:59; задания со сроком старше 30 дней не берутся;
- `401` → `needs_reconnect`; курс без доступа (`403`) пропускается.

### Новый `tests/test_classroom.py`

```python
"""Google Classroom на httpx.MockTransport: курсы → задания → сдачи → наш формат."""
from datetime import timedelta

import httpx

from homework.collect import collect
from homework.models import utcnow

SOON = utcnow() + timedelta(days=3)
OLD = utcnow() - timedelta(days=200)


def handler(request: httpx.Request) -> httpx.Response:
    assert request.headers["Authorization"] == "Bearer ya29.test"
    path = request.url.path
    if path == "/v1/courses":
        assert request.url.params["studentId"] == "me" and request.url.params["courseStates"] == "ACTIVE"
        return httpx.Response(200, json={"courses": [{"id": "c1", "name": "Бази даних"}, {"id": "c2", "name": "Закритий курс"}]})
    if path == "/v1/courses/c1/courseWork":
        if "pageToken" not in request.url.params:
            return httpx.Response(200, json={"nextPageToken": "p2", "courseWork": [
                {"id": "w1", "title": "Лаба 1", "workType": "ASSIGNMENT", "alternateLink": "https://classroom.google.com/w1",
                 "description": "SQL JOIN", "dueDate": {"year": SOON.year, "month": SOON.month, "day": SOON.day},
                 "dueTime": {"hours": 21}},
                {"id": "w2", "title": "Тест", "workType": "MULTIPLE_CHOICE_QUESTION",
                 "dueDate": {"year": SOON.year, "month": SOON.month, "day": SOON.day}}]})
        return httpx.Response(200, json={"courseWork": [
            {"id": "w3", "title": "Старе", "dueDate": {"year": OLD.year, "month": OLD.month, "day": OLD.day}},
            {"id": "w4", "title": "Без строку", "updateTime": utcnow().isoformat()}]})
    if path == "/v1/courses/c1/courseWork/-/studentSubmissions":
        return httpx.Response(200, json={"studentSubmissions": [
            {"courseWorkId": "w1", "state": "TURNED_IN"},
            {"courseWorkId": "w2", "state": "RETURNED", "assignedGrade": 9}]})
    if path.startswith("/v1/courses/c2/"):
        return httpx.Response(403, json={"error": {"status": "PERMISSION_DENIED"}})
    return httpx.Response(404)


def test_classroom_assignments_and_statuses():
    with httpx.Client(transport=httpx.MockTransport(handler)) as client:
        res = collect({"classroom": {"access_token": "ya29.test", "refresh_token": "1//r", "expires_at": 1}}, client=client)
    assert res.sources["classroom"].ok, res.sources["classroom"].error
    by_id = {a.id: a for a in res.assignments}
    assert set(by_id) == {"w1", "w2", "w4"}            # старое (200 дней назад) отброшено, закрытый курс пропущен
    assert by_id["w1"].status == "submitted" and by_id["w1"].due.hour == 21 and by_id["w1"].course == "Бази даних"
    assert by_id["w2"].status == "graded" and by_id["w2"].kind == "quiz" and by_id["w2"].due.hour == 23
    assert by_id["w4"].status == "todo" and by_id["w4"].due is None
    assert all(a.source == "classroom" for a in res.assignments)


def test_expired_token_asks_to_reconnect():
    with httpx.Client(transport=httpx.MockTransport(lambda r: httpx.Response(401))) as client:
        res = collect({"classroom": {"access_token": "old"}}, client=client)
    assert not res.sources["classroom"].ok and res.sources["classroom"].needs_reconnect
```

`python -m pytest -q` → у меня 22 из 22 (20 старых + 2 новых).

`server.py`, `Dockerfile`, переменные collector — **без изменений**.

## 3. `.gitattributes` в корне репозитория

```
# Скрипты для Linux-контейнеров всегда с LF.
# С CRLF на Windows `docker compose up` создаёт пустую базу: init.sh «cannot execute: required file not found».
*.sh   text eol=lf
mvnw   text eol=lf
*.cmd  text eol=crlf
```

Проверено на свежей `main-test-3.0`: на Windows (`core.autocrlf=true`) база поднимается без таблиц.
После добавления у себя один раз: `git rm --cached -r . -q && git reset --hard` (или удалить и заново склонировать).

## 4. `127.0.0.1` вместо `localhost` для Python-инструментов

На Windows `psycopg` с `localhost` **зависает** (пробует IPv6, а Docker держит базу только на `127.0.0.1`);
с `127.0.0.1` — мгновенно. Поменять примеры в `database/migrate.py` (docstring), `database/README.md`,
`ai-content/load/load.py` и README-TEST:

```
postgresql://lekseradar:lekseradar@127.0.0.1:5432/lekseradar
```

## 5. `homework-collector/COLLECTOR-API.md`

В пример тела `POST /collect` добавить:

```json
"classroom": {"access_token": "ya29…", "refresh_token": "1//…", "expires_at": 1762520000}
```

и строку: «classroom — токен выдаёт и обновляет бэкенд; collector только читает Classroom API».

## 6. Railway

1. Облачная база: `python database/migrate.py "<DATABASE_URL>"` — должна дойти до V22.
2. Collector: пересобрать после пунктов 1–2. Новых переменных не нужно.

## Как проверить всё вместе

1. `cd database && docker compose up -d` (база с V22 + collector), бэкенд из `JavaAPI-setup`
   с `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` в `backend/.env`, фронт из `Frontend-setup` (`pnpm dev`).
2. Сайт → настройки → «Plattformer» → Google Classroom → «Logg inn med Google» → аккаунт из Test users.
3. Через ~6 с карточка «Tilkoblet», на «Lekser» задания с подписью «Google Classroom».
4. «Oppdater» → `updated` = числу заданий, дублей нет.

Вопросы — к №2.
