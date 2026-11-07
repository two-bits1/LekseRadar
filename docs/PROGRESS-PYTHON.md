# Ход работ №3: Python, база, ИИ

Что сделано по [PLAN-PYTHON.md](PLAN-PYTHON.md) в `main-test-3.0`. Обновляется каждые 5 коммитов.

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

## Дальше

- Шаг 3: `Dockerfile`, сервис `collector` в `docker-compose.yml`, раздел в README сборщика.
- Шаг 4: проверка ИИ с ключом Groq — нужен ключ от Ильи.
- Шаг 5: деплой — нужен выбор хостинга и доступ к нему.
