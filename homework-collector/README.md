# homework-collector: сбор ДЗ со всех платформ

Python-модуль LekseRadar, который забирает домашние задания из **Canvas**, **Microsoft Teams (Assignments)**,
**itslearning** и **Visma InSchool** и приводит их к одному виду. Эндпоинты подключает бэкенд: здесь только
функции, без своего сервера и без хранения данных.

## Как вызвать

```python
from homework import collect

result = collect({
    "canvas": {"base_url": "https://canvas.instructure.com", "token": "..."},
    "teams": {"access_token": "...", "refresh_token": "...", "expires_at": 1762500000},
    "itslearning": {"base_url": "https://sms.itslearning.com", "refresh_token": "..."},
    "inschool": {"host": "ohg-vgs.inschool.visma.no", "cookies": {"JSESSIONID": "...", "Authorization": "..."}},
    "classroom": {"access_token": "ya29..."},
})

result.model_dump(mode="json")   # готовый JSON для ответа бэкенда
```

Любую платформу можно не передавать. Ошибка одной платформы не ломает остальные.

Проверка из консоли: `python -m homework --creds creds.json` (добавь `--json` для JSON).

Загрузить ДЗ ученика в базу, пока нет `POST /homework/sync`: `python -m homework.to_db --email … --creds creds.json`
(нужен `DATABASE_URL`). Войти в Teams или itslearning и записать токены: `python -m homework.login teams|itslearning --creds creds.json`.
Пошагово: [docs/REAL-DATA.md](../docs/REAL-DATA.md).

## HTTP-сервис для бэкенда (`collector`)

Java вызывает сборщик по HTTP: `homework/server.py` (FastAPI). Контракт и примеры: [COLLECTOR-API.md](COLLECTOR-API.md).

```bash
COLLECTOR_KEY=$(openssl rand -hex 32) uvicorn homework.server:app --port 8090   # или сервис collector в database/docker-compose.yml
```

- Все запросы, кроме `/health`, с заголовком `X-Internal-Key`. Без ключа (≥ 16 символов) сервис не стартует.
- Ничего не хранит, тела запросов не логирует. Наружу не публиковать: только сеть бэкенда.
- Задания из ответа бэкенд пишет в базу функцией `sync_assignments()` (миграция V21).

## Единый формат задания

| поле | значение |
|------|----------|
| `source` | `canvas` / `teams` / `itslearning` / `inschool` / `classroom` |
| `id` | id в исходной платформе |
| `title`, `course` | название и предмет |
| `due` | дедлайн, ISO 8601 в UTC, или `null` |
| `status` | `todo` / `overdue` / `submitted` / `graded` |
| `url` | ссылка на задание в платформе |
| `description` | текст задания, если платформа его отдаёт |
| `kind` | `assignment` / `quiz` / `exam` / `discussion` / `task` |

Ответ `collect()`: `assignments` (сначала просроченные, дальше по дедлайну), `sources` (по каждой платформе:
`ok`, `count`, `error`, `needs_reconnect`) и `updated_credentials`. **Бэкенд сохраняет
`updated_credentials` вместо старых**: там обновлённые токены Teams и itslearning и cookie InSchool.
`needs_reconnect: true` значит, что пользователю нужно войти в эту платформу заново.

## Откуда берутся учётные данные

| Платформа | Способ | Что делает пользователь | Официально? |
|-----------|--------|-------------------------|-------------|
| Canvas | личный токен, REST `/api/v1/planner/items` | Account → Settings → «+ New Access Token», вставляет токен и адрес Canvas | да |
| Teams | Microsoft Graph `/education/me/assignments`, вход по коду устройства | `teams.start_device_login()` → вводит код на microsoft.com/devicelogin → `teams.poll_device_login()` | да, но школа может запретить согласие |
| itslearning | REST `restapi/personal/tasks/v1`, OAuth-клиент мобильного приложения | `itslearning.authorize_url()` → вход через Feide → `itslearning.exchange_code(адрес itsl-itslearning://login?code=...)` | нет (серая зона) |
| InSchool | внутренний API веб-приложения по cookie | входит через ID-porten, cookie сессии передаются в `inschool` | нет |
| Google Classroom | официальный Classroom API | вход через Google и обновление токена делает бэкенд, сюда приходит готовый `access_token` | да |

Для Teams нужно приложение в Entra ID: мультитенантное («Accounts in any organizational directory»),
с включённым «Allow public client flows» и делегированными правами `EduAssignments.ReadBasic`,
`Team.ReadBasic.All`, `User.Read`. Его id передаётся в `client_id` или в переменной `TEAMS_CLIENT_ID`.

## Структура

```
homework/
├── collect.py        collect(): все платформы параллельно → один список
├── server.py         HTTP-сервис collector для бэкенда (COLLECTOR-API.md)
├── to_db.py          CLI: собрать ДЗ ученика и записать в базу (sync_assignments)
├── login.py          CLI: вход в Teams и itslearning, токены → creds.json
├── models.py         Assignment, Credentials, CollectResult
└── sources/          по файлу на платформу: fetch() + normalize() + функции входа
tests/                тесты на подменённых ответах платформ (pytest)
```

## Запуск тестов

```
pip install -r requirements.txt
pytest -q
```
