# collector: HTTP API для бэкенда

Внутренний сервис над `homework`: бэкенд (Java) передаёт расшифрованные доступы и получает ДЗ.
Наружу не открывается. Запуск: `uvicorn homework.server:app --port 8090` (или сервис `collector` в `database/docker-compose.yml`).

- Базовый адрес: `COLLECTOR_URL`, локально `http://localhost:8090`.
- Каждый запрос, кроме `/health`, с заголовком `X-Internal-Key: <COLLECTOR_KEY>`. Нет или неверный → `401`.
- Тела и ответы в JSON, поля в snake_case (как модели в `homework/models.py`).
- Сервис ничего не хранит и не пишет тела запросов в лог.

| Метод | Путь | Тело | Ответ |
|---|---|---|---|
| GET | `/health` | — | `{"ok": true}` (без ключа) |
| POST | `/collect` | `Credentials` | `CollectResult` |
| POST | `/login/teams/start` | `{"client_id": null}` или пусто | `{user_code, verification_uri, device_code, interval, expires_in, message}` |
| POST | `/login/teams/poll` | `{"device_code": "…", "client_id": null}` | `{"pending": true}` или `TeamsCreds` |
| GET | `/login/itslearning/url?base_url=…` | — | `{"url": "…"}` |
| POST | `/login/itslearning/exchange` | `{"base_url": "…", "code_or_url": "…"}` | `ItslearningCreds` |

Ошибки:

| Код | Когда | Тело |
|---|---|---|
| 401 | нет или неверный `X-Internal-Key` | `{"detail": "invalid internal key"}` |
| 400 | вход в платформу не удался (`SourceError`) | `{"detail": "текст для ученика", "needs_reconnect": true}` |
| 422 | неверное тело (проверка FastAPI) | `{"detail": [...]}` |
| 502 | платформа недоступна при входе | `{"detail": "…"}` |

В `/collect` ошибки платформ **не** дают 4xx: ответ всегда `200`, а ошибка лежит в `sources.<платформа>`.

## POST /collect

Передавайте только подключённые платформы. `client_id` у Teams можно не указывать, тогда берётся `TEAMS_CLIENT_ID` сервиса.

```json
{
  "canvas": {"base_url": "https://uio.instructure.com", "token": "7~abc"},
  "teams": {"access_token": "eyJ…", "refresh_token": "0.A…", "expires_at": 1762520000},
  "itslearning": {"base_url": "https://sms.itslearning.com", "access_token": "…", "refresh_token": "…", "expires_at": 1762520000},
  "inschool": {"host": "ohg-vgs.inschool.visma.no", "cookies": {"JSESSIONID": "…", "Authorization": "…"}}
}
```

```json
{
  "generated_at": "2026-11-07T16:00:00Z",
  "assignments": [
    {"source": "canvas", "id": "4211", "title": "Oblig 3: Integraler", "course": "MAT1100",
     "due": "2026-11-12T22:59:00Z", "status": "todo", "url": "https://uio.instructure.com/courses/1/assignments/4211",
     "description": "Løs oppgave 1–5 …", "kind": "assignment"}
  ],
  "sources": {
    "canvas": {"ok": true, "count": 1, "error": null, "needs_reconnect": false},
    "inschool": {"ok": false, "count": 0, "error": "Сессия InSchool истекла, нужно войти заново", "needs_reconnect": true}
  },
  "updated_credentials": {
    "teams": {"access_token": "eyJ…new", "refresh_token": "0.A…new", "expires_at": 1762523600, "client_id": "…"}
  }
}
```

Что делает бэкенд с ответом:
- `assignments` целиком → `SELECT * FROM sync_assignments(:userId, CAST(:json AS jsonb))` (миграция V21);
- `sources.<p>` → `platform_connections`: `ok` → `last_synced_at = now()`, `last_error = null`; иначе `last_error`, `needs_reconnect`;
- `updated_credentials.<p>` → зашифровать и сохранить **вместо** старых, иначе refresh-токен скоро перестанет работать.

## Teams: вход по коду устройства

```http
POST /login/teams/start
→ 200 {"user_code": "ABCD-EFGH", "verification_uri": "https://microsoft.com/devicelogin",
       "device_code": "GAQABAAEAAA…", "interval": 5, "expires_in": 900,
       "message": "To sign in, use a web browser to open the page https://microsoft.com/devicelogin and enter the code ABCD-EFGH"}
```

Ученику показать `user_code` и `verification_uri`. `device_code` не показывать, он нужен для опроса.
Опрашивать раз в `interval` секунд, не дольше `expires_in`:

```http
POST /login/teams/poll   {"device_code": "GAQABAAEAAA…"}
→ 200 {"pending": true}                       ещё не вошёл
→ 200 {"access_token": "…", "refresh_token": "…", "expires_at": 1762523600, "client_id": "…"}   вошёл: сохранить как доступ teams
→ 400 {"detail": "Вход не удался: …", "needs_reconnect": false}                                  отказ или код истёк
```

## itslearning

```http
GET /login/itslearning/url?base_url=https://sms.itslearning.com
→ 200 {"url": "https://sms.itslearning.com/oauth2/authorize.aspx?client_id=…&redirect_uri=itsl-itslearning%3A%2F%2Flogin&…"}

POST /login/itslearning/exchange  {"base_url": "https://sms.itslearning.com", "code_or_url": "itsl-itslearning://login?code=XYZ&state=…"}
→ 200 {"base_url": "https://sms.itslearning.com", "access_token": "…", "refresh_token": "…", "expires_at": 1762523600}
→ 400 {"detail": "itslearning не выдал токен, нужно войти заново", "needs_reconnect": true}
```

## Canvas и InSchool

Отдельного входа нет: бэкенд принимает от ученика токен Canvas или cookie InSchool, проверяет их одним `POST /collect`
только с этой платформой (`sources.<p>.ok`) и сохраняет.

## Проверка вручную

```bash
export COLLECTOR_KEY=dev-key
uvicorn homework.server:app --port 8090
curl localhost:8090/health
curl -s localhost:8090/collect -H "X-Internal-Key: $COLLECTOR_KEY" -H 'Content-Type: application/json' -d @creds.json
```
