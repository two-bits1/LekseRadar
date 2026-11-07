# План: от main-test-3.0 до готового MVP

Состояние на 7 ноября 2026: ветка `main-test-3.0` работает целиком на тестовых данных (что проверено: [README-TEST.md](README-TEST.md)).
Настоящие ДЗ ученика уже можно загрузить из консоли: [docs/REAL-DATA.md](docs/REAL-DATA.md).
Чтобы ученик подключал платформы **сам на сайте** и ДЗ обновлялись без консоли, осталось 8 этапов.

Кто делает: **№1** фронт, **№2** бэкенд (Java), **№3** Илья (Python и база).

| # | Этап | Кто | Время | Зависит от |
|---|---|---|---|---|
| 0 | Загрузка настоящих ДЗ из консоли (`homework.to_db`, `homework.login`) | №3 | ✅ сделано | — |
| 1 | Шифрование доступов к платформам | №2 | 1–2 ч | — |
| 2 | Сервис `collector`: `collect()` и вход в платформы по HTTP | №3 | 2 ч | — |
| 3 | API платформ: подключить, статус, отключить | №2 | 3 ч | 1, 2 |
| 4 | `POST /homework/sync` и обновление по расписанию | №2 | 2–3 ч | 1, 2, 3 |
| 5 | Страница «Plattformer» и кнопка «Oppdater» | №1 | 3 ч | 3, 4 |
| 6 | Проверка с настоящим ключом ИИ | №3 | 30 мин | — |
| 7 | Деплой | все | 2 ч | 1–6 |
| 8 | Перенос в рабочие ветки и PR | все | 1 ч | 1–7 |

Этапы 1, 2 и 6 можно делать параллельно прямо сейчас.

---

## Кто что делает

Подробные планы: **бэкенд [docs/PLAN-BACKEND.md](docs/PLAN-BACKEND.md)**, **фронт [docs/PLAN-FRONTEND.md](docs/PLAN-FRONTEND.md)**, **Python и база [docs/PLAN-PYTHON.md](docs/PLAN-PYTHON.md)**.

Все трое начинают сразу: фронт и бэк работают по контрактам ниже, не дожидаясь друг друга.
Точки встречи отмечены 🔗.

### №3 Илья: Python, база, ИИ (~6 ч)

1. **Контракт `collector`** (30 мин, первым делом). Выложить `homework-collector/COLLECTOR-API.md` с таблицей
   из этапа 2 и примерами JSON. 🔗 После этого №2 пишет клиент к нему.
2. **Миграция V21 `sync_assignments(user_id, jsonb)`** (1 ч). Принимает `assignments` из ответа `collect()`, делает UPSERT
   тем же SQL, что `to_db.py` (не откатывает «сдано»), возвращает `(added, updated)`. Проверить на чистой базе и на `testdata`.
   Бэкенду тогда не нужно писать UPSERT на Java, достаточно одного вызова функции. 🔗 Сказать №2, когда в ветке.
3. **Сервис `collector`** (2 ч): `homework-collector/server.py` на FastAPI, ключ `X-Internal-Key`, тесты на `MockTransport`,
   сервис в `docker-compose.yml` на `127.0.0.1:8090`. `to_db.py` переписать на `sync_assignments()`.
4. **Ключ Groq** (30 мин): чат, квиз от ИИ, проверка ответов по README-TEST. Итог в README-TEST.
5. **Деплой базы и `collector`** (1 ч): миграции → `load.py`, без `testdata`. Выдать №2 строку подключения и `COLLECTOR_KEY`.
6. **Перенос** `database/`, `homework-collector/`, `ai-content/` в `PythonAPI` (PR #9).

### №2 бэкенд: Java (~8 ч)

1. **`CredentialsCipher`** (1–2 ч), этап 1. `CREDENTIALS_KEY` в `.env.example`.
2. **`CollectorClient`** (1 ч): HTTP-клиент к `collector` по `COLLECTOR-API.md` (`COLLECTOR_URL`, `COLLECTOR_KEY`).
   До готовности сервиса тестировать на заглушке, как ИИ в e2e.
3. **API платформ** (3 ч), этап 3: контроллер, сервис, DTO, проверка доменов против SSRF. 🔗 Строки в `docs/API.md` для №1.
4. **`POST /homework/sync`** (2 ч), этап 4: расшифровать → `collector /collect` → `SELECT * FROM sync_assignments(...)` →
   сохранить `updated_credentials` и статусы платформ. Лимит 1/мин, `@Scheduled` раз в 30 минут.
5. **Деплой бэкенда** (1 ч) с переменными из этапа 7.
6. **Перенос** `backend/` в `JavaAPI-setup` (PR #2).

### №1 фронт: React (~5 ч)

1. **Типы и клиент** (1 ч) по контракту ниже, моки в `api.test.ts`. Можно начинать сейчас, бэк не нужен.
2. **`/settings/platforms`** (3 ч), этап 5: 4 карточки (Canvas, Teams, itslearning, InSchool), подключение и отключение.
3. **«Oppdater» на `/homework`** (1 ч): `POST /homework/sync`, ошибки по платформам, «Koble til på nytt».
4. **Деплой на Vercel** с `VITE_API_URL`. 🔗 Дать №2 адрес для `CORS_ORIGINS`.
5. **Перенос** `frontend/` в `Frontend-setup`.

### Контракт фронт ↔ бэк (чтобы начать без ожидания)

```jsonc
// GET /api/v1/platforms
[{"platform": "canvas", "connected": true, "needsReconnect": false,
  "lastSyncedAt": "2026-11-07T15:00:00Z", "lastError": null}, …4 шт.]

// PUT /api/v1/platforms/canvas   {"baseUrl": "https://uio.instructure.com", "token": "…"}        → 200 как элемент списка
// PUT /api/v1/platforms/inschool {"host": "ohg-vgs.inschool.visma.no", "cookies": {"JSESSIONID": "…"}} → 200
// POST /api/v1/platforms/teams/start → {"userCode": "ABCD-EFGH", "verificationUri": "https://microsoft.com/devicelogin",
//                                      "deviceCode": "…", "interval": 5}
// POST /api/v1/platforms/teams/poll {"deviceCode": "…"} → {"status": "pending"} | {"status": "connected", "platform": {…}}
// GET  /api/v1/platforms/itslearning/url?baseUrl=… → {"url": "https://…/oauth2/authorize.aspx?…"}
// POST /api/v1/platforms/itslearning {"baseUrl": "…", "codeOrUrl": "itsl-itslearning://login?code=…"} → 200
// DELETE /api/v1/platforms/{platform} → 204
// Ошибки: 400 неверные данные или домен, 422 платформа не приняла доступ, 429 слишком часто, 503 collector недоступен.
// Не 401: фронт на 401 разлогинивает ученика.

// POST /api/v1/homework/sync → 200
{"added": 3, "updated": 9,
 "sources": [{"platform": "canvas", "ok": true, "count": 12, "error": null, "needsReconnect": false},
             {"platform": "inschool", "ok": false, "count": 0, "error": "Сессия InSchool истекла…", "needsReconnect": true}]}
```

### Порядок по времени

| Час | №3 Илья | №2 бэк | №1 фронт |
|---|---|---|---|
| 0–1 | контракт `collector`, V21 | `CredentialsCipher` | типы, клиент, моки |
| 1–3 | `server.py`, compose | `CollectorClient`, API платформ | `/settings/platforms` |
| 3–5 | ключ Groq, `to_db` на V21 | API платформ, `/homework/sync` | страница платформ, «Oppdater» |
| 5–6 | 🔗 общая проверка в браузере: подключить Canvas → ДЗ на «Lekser» | | |
| 6–8 | деплой базы и `collector` | деплой бэка | деплой фронта |
| 8 | 🔗 перенос по веткам, чек-лист «MVP готов» | | |

---

## 0. Загрузка из консоли ✅

Сделано в этой ветке:
- `python -m homework.to_db --email … --creds creds.json`: `collect()` → UPSERT в `assignments`, новые токены → обратно в `creds.json`;
- `python -m homework.login teams|itslearning`: вход и запись токенов;
- инструкция по каждой платформе: [docs/REAL-DATA.md](docs/REAL-DATA.md).

SQL из `to_db.py` (`UPSERT`) переносится в этап 4 как есть: он не откатывает «сдано», выставленное на сайте.

## 1. Шифрование доступов (№2)

Колонка `platform_connections.credentials_enc` уже есть (bytea), кода шифрования нет.

- `security/CredentialsCipher`: AES-256-GCM, случайный nonce 12 байт, в базу `nonce || ciphertext`.
  Что шифруем: JSON того объекта, который `collect()` ждёт для платформы (`CanvasCreds`, `TeamsCreds` …).
- Ключ из `CREDENTIALS_KEY` (32 байта в base64, `openssl rand -base64 32`). Без ключа бэкенд не стартует.
  Добавить в `backend/.env.example`.
- Тест: шифрование и расшифровка, чужой ключ → ошибка, два шифрования одного текста различаются.

Готово, когда: в базе нет ни одного токена открытым текстом, тест зелёный.

## 2. Сервис `collector` (№3)

Java не умеет вызывать Python-функцию напрямую. Обёртка: маленький FastAPI в `homework-collector/server.py`,
только для бэкенда, наружу не открыт.

| Метод | Путь | Тело → ответ |
|---|---|---|
| POST | `/collect` | `Credentials` → `CollectResult` (как `collect()`) |
| POST | `/login/teams/start` | → `{user_code, verification_uri, device_code, interval, expires_in}` |
| POST | `/login/teams/poll` | `{device_code}` → `{pending: true}` или `TeamsCreds` |
| GET | `/login/itslearning/url?base_url=` | → `{url}` |
| POST | `/login/itslearning/exchange` | `{base_url, code_or_url}` → `ItslearningCreds` |
| GET | `/health` | → `{ok: true}` |

- Каждый запрос с заголовком `X-Internal-Key` = `COLLECTOR_KEY`, иначе 401.
- Доступы не логировать и не хранить: сервис без состояния.
- `docker-compose.yml`: сервис `collector` на `127.0.0.1:8090`. Бэкенд: `COLLECTOR_URL`, `COLLECTOR_KEY`.
- Тесты: `httpx.MockTransport`, как в `tests/test_collect.py`.

Это меняет прежнее правило «Python только функции, без своего сервера». Если команда против, запасной вариант:
бэкенд пишет доступы во временный файл и запускает `python -m homework --creds <файл> --json` процессом (без входа в Teams и itslearning с сайта).

## 3. API платформ (№2)

| Метод | Путь | Что делает |
|---|---|---|
| GET | `/api/v1/platforms` | 4 платформы: `connected`, `needsReconnect`, `lastSyncedAt`, `lastError`. Без токенов |
| PUT | `/api/v1/platforms/canvas` | `{baseUrl, token}` → проверка одним `collect` → сохранить |
| PUT | `/api/v1/platforms/inschool` | `{host, cookies}` → так же |
| POST | `/api/v1/platforms/teams/start` | → `{userCode, verificationUri, deviceCode}` |
| POST | `/api/v1/platforms/teams/poll` | `{deviceCode}` → `{status: pending}` пока ждём, `{status: connected}` и сохранить, когда вошёл |
| GET | `/api/v1/platforms/itslearning/url?baseUrl=` | ссылка для входа |
| POST | `/api/v1/platforms/itslearning` | `{baseUrl, codeOrUrl}` → сохранить |
| DELETE | `/api/v1/platforms/{platform}` | удалить подключение; ДЗ этой платформы остаются |

- `baseUrl` и `host` только `https://` и только известные домены (`*.instructure.com`, `*.itslearning.com`,
  `*.inschool.visma.no` и свои адреса Canvas из списка в настройках), иначе это SSRF через наш сервер.
- `PlatformResponseDTO` уже есть, проверить поля.
- Добавить строки в `docs/API.md` и контрактные тесты фронта.

## 4. Синхронизация (№2)

`POST /api/v1/homework/sync`:
1. Расшифровать все подключения ученика → один запрос `collector /collect`.
2. UPSERT заданий: `SELECT * FROM sync_assignments(:userId, :assignmentsJson)` (V21 от №3, тот же SQL, что в `to_db.py`).
3. `updated_credentials` → зашифровать и сохранить вместо старых.
4. По каждой платформе: `needs_reconnect`, `last_error`, `last_synced_at`.
5. Событие `homework_synced` в `user_events`, ответ `{sources: [...], added, updated}`.

Плюс:
- лимит 1 синхронизация в минуту на ученика (`RateLimiter` уже есть);
- `@Scheduled` раз в 30 минут для учеников, которые заходили за последние 7 дней; первая синхронизация сразу после подключения платформы;
- задания, пропавшие с платформы, не удалять (ученик мог вести по ним чат).

Готово, когда: подключили Canvas на сайте → через минуту ДЗ на «Lekser» без консоли.

## 5. Фронт (№1)

- `/settings/platforms` вместо заглушки: карточка на платформу со статусом и временем последнего обновления.
  - Canvas: поля адрес и токен, подсказка, где взять токен.
  - Teams: кнопка «Koble til» → показать код и ссылку → опрашивать `poll` раз в `interval` секунд.
  - itslearning: ссылка для входа + поле «вставьте адрес itsl-itslearning://…».
  - InSchool: поля для cookie и предупреждение, что сессия короткая.
  - «Koble fra» с подтверждением.
- `/homework`: кнопка «Oppdater» → `POST /homework/sync`, под ней ошибки по платформам и ссылка «Koble til på nytt».
- Пустая домашка ведёт на `/settings/platforms` (уже так).
- Тексты nb/en/ru, контрактные тесты для новых эндпоинтов.

## 6. Настоящий ключ ИИ (№3)

`backend/.env`: `OPENAI_API_KEY` от Groq. Проверить по сценарию README-TEST:
- чат-репетитор по заданию, ответ на языке профиля;
- квиз по новой теме от ИИ, поле `language` = код языка (V20);
- проверку текстовых ответов (`answer_checker`).
Записать в README-TEST, что прошло.

## 7. Деплой (все)

Выбран Railway, пошагово: [docs/DEPLOY-RAILWAY.md](docs/DEPLOY-RAILWAY.md).

| Часть | Где | Переменные |
|---|---|---|
| База | Neon / Railway PostgreSQL 16 | применить `database/migrations`, затем `load.py`, **без** `testdata` |
| Бэкенд | Railway / Render (Java 21) | `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `CREDENTIALS_KEY`, `OPENAI_*`, `COLLECTOR_URL`, `COLLECTOR_KEY`, `CORS_ORIGINS` |
| collector | рядом с бэкендом, только внутренняя сеть | `COLLECTOR_KEY`, `TEAMS_CLIENT_ID` |
| Фронт | Vercel | `VITE_API_URL` |

Все секреты только в настройках хостинга, не в git.

## 8. Перенос и PR (все)

Сейчас всё лежит только в `main-test-3.0`. Разложить по веткам (или один PR из `main-test-3.0`, как решит команда):
- `database/`, `homework-collector/`, `ai-content/` → `PythonAPI` (PR #9);
- `backend/` → `JavaAPI-setup` (PR #2);
- `frontend/` → `Frontend-setup`.

---

## MVP готов, когда

- [ ] Новый ученик регистрируется, подключает хотя бы Canvas на сайте и видит свои ДЗ без консоли.
- [ ] ДЗ обновляются сами, ошибка платформы видна и чинится повторным входом.
- [ ] Токены в базе зашифрованы, `collector` недоступен снаружи.
- [ ] Чат, квиз и проверка ответов работают с настоящим ключом ИИ.
- [ ] Сайт открыт по публичному адресу, тестового ученика там нет.
- [ ] Все тесты зелёные (фронт, бэкенд, `pytest`), README-TEST обновлён.

## Можно после MVP

Из [README-TEST.md](README-TEST.md), раздел «Что осталось»:
- лимиты в Redis или таблице вместо памяти;
- отзыв JWT (версия токена у пользователя);
- тексты ошибок бэкенда на nb/en;
- поиск материалов по курсу задания, а не только по предмету;
- API ачивок и напоминаний;
- CI для бэкенда, Python и миграций.
