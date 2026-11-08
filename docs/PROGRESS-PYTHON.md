# Ход работ №3: Python, база, ИИ

Что сделано по [PLAN-PYTHON.md](PLAN-PYTHON.md) в `main-test-3.0`. Обновляется каждые 5 коммитов. Хеши ниже — коммиты ветки `main-test-3.0`; в `PythonAPI` эти изменения перенесены отдельными коммитами «(from main-test-3.0)».

## Коммиты 1–5 (7 ноября 2026)

| Коммит | Шаг плана | Что сделано | Проверено |
|---|---|---|---|
| `04b3942` | 1 | [homework-collector/COLLECTOR-API.md](../homework-collector/COLLECTOR-API.md): контракт `collector` для бэкенда, примеры JSON на каждый эндпоинт, что бэкенд делает с ответом | — |
| `c4203f9` | 2 | `V21__sync_assignments.sql`: `sync_assignments(user_id, jsonb)` → `(added, updated)` | чистая база V1–V21 + `testdata`: новые → `(2,0)`, повтор → обновление, «сдано» не откатывается, пустое описание не затирает, дубль id в ответе не ломает, `[]` и `NULL` → `(0,0)` |
| `e4bdc06` | 2 | `homework.to_db` пишет через `sync_assignments()`, свой SQL удалён | два запуска подряд: 2 новых, затем 2 обновлено |
| `47f4f0a` | 2 | `database/README.md`, `database/ENTITIES.md`: V21 и раздел «Домашка» в функциях базы с вызовом из Java | — |
| `d3fd59d` | 3 | `homework/server.py`: FastAPI `collector` (`/collect`, вход Teams и itslearning, ключ `X-Internal-Key`), 12 тестов | `pytest` 18 из 18 |

**Для №2 (бэкенд) уже можно:** писать `CollectorClient` по `COLLECTOR-API.md` и вызывать
`SELECT added, updated FROM sync_assignments(:userId, CAST(:json AS jsonb))`.

Локально `collector` запускается так:
```bash
cd homework-collector && pip install -r requirements.txt
COLLECTOR_KEY=$(openssl rand -hex 32) uvicorn homework.server:app --port 8090
```

## Коммиты 6–10

| Коммит | Шаг плана | Что сделано | Проверено |
|---|---|---|---|
| `0239d3f` | 3 | `homework-collector/Dockerfile` (без root, healthcheck, без access-log), сервис `collector` в `docker-compose.yml` на `127.0.0.1:8090` | образ собран, `/health` → ok, без ключа 401, с ключом 200, в логах нет запросов |
| `a93a39e` | 3 | README сборщика: раздел «HTTP-сервис», структура; `docs/RUN.md`: collector и V21 | — |
| `8db93b8` | 5 | `database/migrate.sh`: докатывает только новые миграции, учёт в `schema_migrations`, `--baseline`, `--status`; `init.sh` тоже ведёт учёт | чистая база: V1–V21, повтор ничего не делает; старая база без учёта → просит `--baseline`; база на V20 + `--baseline 20` → применилась V21; Docker-том: 21 запись |
| `69adca3` | 4 | `ai-content/check/check_ai.py`: чат по ДЗ, квиз от ИИ (и `quiz_sets.language`), проверка ответа на ru и nb, таблица для README-TEST | прогон на бэкенде с локальной заглушкой ИИ: все шаги проходят, проверка языка ловит ответ не на том языке |

**Обновить свою локальную базу до V21:**
```bash
database/migrate.sh "postgresql://lekseradar:<DB_PASSWORD>@localhost:5432/lekseradar" --baseline 20
```

**Проверить ИИ с ключом Groq** (ключ в `backend/.env`, бэкенд запущен):
```bash
pip install 'psycopg[binary]'
python ai-content/check/check_ai.py --email test@lekseradar.no --password LekseRadar2026 \
  --database-url postgresql://lekseradar:<DB_PASSWORD>@localhost:5432/lekseradar
```
Таблицу из вывода — в README-TEST.md.

## Коммиты 11–15

| Коммит | Шаг плана | Что сделано | Проверено |
|---|---|---|---|
| `112d1da` | 5 | `migrate.sh`: если нет `psql`, берёт его из Docker | — |
| `043d6a0` | 5 | `database/migrate.py`: те же миграции без `psql` и Docker, только Python | чистая база V1–V21, повтор ничего не делает |
| `ccd542f` | — | itslearning: если вместо токена пришёл не JSON, понятная ошибка вместо `JSONDecodeError` (по просьбе №2) | новый тест, `pytest` 19 из 19 |

## Коммиты 16–21

| Коммит | Шаг | Что сделано | Проверено |
|---|---|---|---|
| `4c3086a` | — | itslearning: не-JSON в списке заданий → понятная ошибка, весь разбор JSON через `_json()` | тест, `pytest` 20 из 20 |
| `819dcec` | Classroom | `V22__classroom.sql`: `classroom` в типе `platform`; README и ENTITIES | чистая база V1–V22 и база на V21 → V22; `sync_assignments` принимает `source: classroom` |
| `d7eb691` | Classroom | collector: `sources/classroom.py` (активные курсы → задания → свои сдачи, 8 потоков), `ClassroomCreds` | 3 теста, `pytest` 23 из 23 |
| `6c79c82` | Windows | `.gitattributes`: `.sh` и `mvnw` всегда LF (иначе на Windows `init.sh` не запускается и база пустая) | — |
| `0e075c6` | Windows | `127.0.0.1` вместо `localhost` в адресах базы для Python (на Windows psycopg зависает на IPv6) | — |
| `4360bc8` | Classroom | `COLLECTOR-API.md`: `classroom` в `/collect`, вход и обновление токена делает бэкенд | — |

Задание от №2: `FOR-PYTHON.md`. Вход Google и `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` только у бэкенда,
у collector новых переменных нет.

## Дальше

- Учебные материалы в `ai-content/materials/<Предмет>/<Тема>/`.
