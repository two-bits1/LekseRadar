# План №3 (Илья): Python, база, ИИ

Часть общего плана [PLAN-MVP.md](https://github.com/two-bits1/LekseRadar/blob/main-test-3.0/PLAN-MVP.md). Твоя задача: дать бэкенду (№2) всё, чтобы он мог собирать ДЗ
по HTTP и записывать их одной SQL-функцией, проверить ИИ с настоящим ключом и поднять базу и `collector` в облаке.

Общее время около 6 часов. Работай в `main-test-3.0`, один коммит на шаг, сообщения коммитов на английском.
Перед коммитом: `cd homework-collector && python -m pytest -q`, для миграций — прогон на чистом PostgreSQL 16.

Бэкенд ждёт от тебя две вещи, их делай первыми: **контракт `collector` (шаг 1)** и **V21 (шаг 2)**.

## Что уже есть

| Есть | Где |
|---|---|
| `collect(credentials)` → `CollectResult` (assignments, sources, updated_credentials) | `homework-collector/homework/collect.py` |
| Вход: `teams.start_device_login/poll_device_login`, `itslearning.authorize_url/exchange_code` | `homework/sources/` |
| `to_db.py`: UPSERT в `assignments`, не откатывает «сдано» (SQL `UPSERT`) | `homework/to_db.py` |
| `login.py`: вход в Teams и itslearning из консоли | `homework/login.py` |
| Тесты на `httpx.MockTransport` | `tests/test_collect.py` |
| Миграции V1–V20, docker-compose с `content` и `testdata` | `database/` |
| Промпты `tutor`, `quiz_generator`, `answer_checker` | `ai-content/prompts/` |

## Шаг 1. Контракт `collector` (30 мин) 🔗 №2

Файл `homework-collector/COLLECTOR-API.md`. Всё в snake_case, как в моделях Python.

| Метод | Путь | Тело | Ответ |
|---|---|---|---|
| POST | `/collect` | `{"canvas": {…}, "teams": {…}, …}` (= `Credentials`) | `CollectResult` (= `result.model_dump(mode="json")`) |
| POST | `/login/teams/start` | — | `{user_code, verification_uri, device_code, interval, expires_in}` |
| POST | `/login/teams/poll` | `{device_code}` | `{"pending": true}` или `TeamsCreds` |
| GET | `/login/itslearning/url?base_url=` | — | `{"url": "…"}` |
| POST | `/login/itslearning/exchange` | `{base_url, code_or_url}` | `ItslearningCreds` |
| GET | `/health` | — | `{"ok": true}` (без ключа) |

Ошибки: `401` нет или неверный `X-Internal-Key`; `422` неверное тело (FastAPI сам); `400 {"detail": "…", "needs_reconnect": bool}`
на `SourceError` при входе. В `/collect` ошибки платформ не ломают ответ: они в `sources`.

Положи по примеру JSON на каждый эндпоинт. Скинь ссылку №2.

Коммит: `docs(collector): HTTP API contract for the backend`.

## Шаг 2. Миграция V21 `sync_assignments` (1 ч) 🔗 №2

Файл `database/migrations/V21__sync_assignments.sql`:

```sql
-- Записать ДЗ из collect() одним вызовом. items = CollectResult.assignments (JSON-массив).
-- Не откатывает статус «сдано»/«оценено», выставленный на сайте, если платформа ещё показывает todo/overdue.
CREATE FUNCTION sync_assignments(p_user BIGINT, items JSONB)
RETURNS TABLE (added INT, updated INT) LANGUAGE sql AS $$
  WITH up AS (
    INSERT INTO assignments AS a (user_id, source, external_id, title, course, due_at, status, url, description, kind)
    SELECT p_user, (x->>'source')::platform, x->>'id', x->>'title', coalesce(x->>'course', ''),
           (x->>'due')::timestamptz, (x->>'status')::assignment_status, x->>'url',
           coalesce(x->>'description', ''), coalesce(x->>'kind', 'assignment')
    FROM jsonb_array_elements(items) x
    ON CONFLICT (user_id, source, external_id) DO UPDATE SET
      title = EXCLUDED.title, course = EXCLUDED.course, due_at = EXCLUDED.due_at, url = EXCLUDED.url,
      description = CASE WHEN EXCLUDED.description <> '' THEN EXCLUDED.description ELSE a.description END,
      kind = EXCLUDED.kind,
      status = CASE WHEN a.status IN ('submitted', 'graded') AND EXCLUDED.status IN ('todo', 'overdue')
                    THEN a.status ELSE EXCLUDED.status END,
      updated_at = now()
    RETURNING (xmax = 0) AS inserted)
  SELECT count(*) FILTER (WHERE inserted)::int, count(*) FILTER (WHERE NOT inserted)::int FROM up;
$$;
```

Проверить на чистой базе (V1–V21) и поверх `testdata`:
- первый вызов с 2 заданиями → `(2, 0)`, второй тот же → `(0, 2)`;
- задание `submitted` на сайте + `todo` с платформы → остаётся `submitted`;
- пустой массив `'[]'` → `(0, 0)`;
- одинаковые `id` в одном массиве → ошибка `ON CONFLICT … cannot affect row a second time`.
  Если такое бывает у платформы, добавить `SELECT DISTINCT ON (x->>'source', x->>'id')`.

Обновить `database/README.md` (таблица миграций) и `database/ENTITIES.md` (раздел функций, у `assignments`: «UPSERT через `sync_assignments`»).
`to_db.py` перевести на `SELECT * FROM sync_assignments(%s, %s::jsonb)` и удалить свой `UPSERT`.

Коммиты: `feat(db): V21 sync_assignments for upserting collected homework`, `refactor(collector): to_db uses sync_assignments`.

## Шаг 3. Сервис `collector` (2 ч)

Файл `homework-collector/homework/server.py` (FastAPI), запуск `uvicorn homework.server:app --port 8090`.

- `requirements.txt`: `fastapi`, `uvicorn[standard]`.
- Ключ: зависимость, которая сравнивает `X-Internal-Key` с `COLLECTOR_KEY` через `secrets.compare_digest`.
  Без `COLLECTOR_KEY` сервис не стартует.
- `/collect`: `collect(body)` → `model_dump(mode="json")`. `collect` синхронный, поэтому эндпоинт обычный `def`
  (FastAPI сам вынесет его в поток).
- `/login/*`: обёртки над функциями из `sources/teams.py` и `sources/itslearning.py`, `SourceError` → `400`.
- **Не логировать тела** и выключить access-log uvicorn для `/collect` (там токены в теле, но не в URL, так что достаточно не печатать тела).
- `Dockerfile` в `homework-collector/`: `python:3.12-slim`, `pip install -r requirements.txt`, `CMD uvicorn … --host 0.0.0.0 --port 8090`.
- `database/docker-compose.yml`: сервис `collector`, порт `127.0.0.1:8090:8090`, `COLLECTOR_KEY` из `.env`, `TEAMS_CLIENT_ID`.

Тесты `tests/test_server.py` через `fastapi.testclient.TestClient`:
- без ключа и с неверным ключом → 401; `/health` без ключа → 200;
- `/collect` с Canvas на `MockTransport` (подменить `httpx.Client` через `monkeypatch`) → задания в ответе;
- `/login/teams/poll` → `{"pending": true}`, когда Microsoft отвечает `authorization_pending`.

Обновить `homework-collector/README.md`: раздел «HTTP-сервис».

Коммиты: `feat(collector): FastAPI service for the backend`, `chore(compose): collector service`.

## Шаг 4. ИИ с настоящим ключом (30 мин)

1. Ключ Groq: https://console.groq.com/keys → `backend/.env` (`OPENAI_API_KEY`, модель `openai/gpt-oss-120b`). В чат ключ не присылать.
2. Пройти по `README-TEST.md` три сценария на ru и nb (язык меняется в профиле):
   - чат-репетитор по заданию с /homework: ответ на языке профиля, учитывает текст задания и срок;
   - квиз по новой теме (не из testdata): создался, `language` в сохранённом квизе = `ru`/`nb` (V20);
   - текстовый ответ в квизе проверяется (`answer_checker`), вердикт и объяснение разумные.
3. Если ответы плохие, правь промпты в `ai-content/prompts/` и `python ai-content/load/load.py`.
4. Итог одной таблицей в `README-TEST.md` («проверено с настоящим ключом»).

Коммит: `docs(test): results with a real Groq key` (+ `fix(prompts): …`, если правил).

## Шаг 5. Деплой базы и `collector` (1 ч) 🔗 №2

**База**: PostgreSQL 16.
```bash
for f in database/migrations/V*.sql; do echo $f; done | sort -V   # порядок: V1, V2, … V21
for f in $(ls database/migrations/V*.sql | sort -V); do psql "$PROD_URL" -v ON_ERROR_STOP=1 -f "$f"; done
DATABASE_URL="$PROD_URL" python ai-content/load/load.py
```
`testdata` **не** загружать. Строку подключения передать №2 лично, не в общий чат и не в git.

**collector**: из `homework-collector/Dockerfile`, рядом с бэкендом, во внутренней сети (без публичного адреса). Переменные `COLLECTOR_KEY` (`openssl rand -hex 32`), `TEAMS_CLIENT_ID`.
Передать №2 `COLLECTOR_URL` и `COLLECTOR_KEY`.

**Teams** (если нужен): приложение в Entra ID по шагам из [REAL-DATA.md](REAL-DATA.md#microsoft-teams-официальный-api-graph).

Проверка: `curl $COLLECTOR_URL/health` из бэкенда → `{"ok": true}`; снаружи адрес недоступен.

## Шаг 6. Общая проверка и перенос

1. 🔗 С №1 и №2 в браузере: регистрация → Canvas на `/settings/platforms` → ДЗ на «Lekser» → «Oppdater» → чат по заданию.
2. `database/`, `homework-collector/`, `ai-content/` из `main-test-3.0` → ветка `PythonAPI` (PR #9).
3. `README-TEST.md`: итог MVP, что проверено, что осталось.

## Готово, когда

- [ ] `COLLECTOR-API.md` у №2, V21 в ветке и проверена.
- [ ] `pytest` зелёный, включая `test_server.py`.
- [ ] `docker compose up` поднимает базу, промпты и `collector`; `curl localhost:8090/health` → ok.
- [ ] Чат, квиз и проверка ответов работают с ключом Groq на ru и nb, итог в README-TEST.
- [ ] База и `collector` в облаке, `collector` без публичного адреса, секреты только в настройках хостинга.
- [ ] Перенесено в `PythonAPI`.
