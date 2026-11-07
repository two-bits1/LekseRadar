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
| POST | `/api/v1/platforms/teams/poll` | `{deviceCode}` → 202 пока ждём, 200 и сохранить, когда вошёл |
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
2. UPSERT заданий тем же SQL, что в `homework-collector/homework/to_db.py`.
3. `updated_credentials` → зашифровать и сохранить вместо старых.
4. По каждой платформе: `needs_reconnect`, `last_error`, `last_synced_at`.
5. Событие `homework_synced` в `user_events`, ответ `{sources: {...}, added, updated}`.

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
