# Проверка main-test-4.0 (7 ноября 2026, 20:20)

Сборка: `main` + `PythonAPI` (2839973) + `JavaAPI-setup` (e3fe0f0) + `Frontend-setup` (e088523).
Без `main-test-3.0` и без тестовых данных. Конфликт при слиянии один: `.gitignore` (оставлены `.env` и `creds*.json`).

## Итог

Все части собираются, все тесты зелёные, бэкенд стартует с базой V1–V22 и collector, все 40 запросов фронта
есть в API бэкенда. Для запуска в облаке осталось настроить окружение (см. [TODO.md](TODO.md)).

## Что проверено

| Часть | Проверка | Результат |
|---|---|---|
| База | чистый Postgres 16 → `database/migrate.py` | V1–V22 ✓, повтор ничего не делает |
| База | `ai-content/load/load.py` | 3 промпта (tutor, quiz_generator, answer_checker); материалов нет |
| Скрипты | `bash -n` для `init.sh`, `migrate.sh`; `.gitattributes` держит LF | ✓ |
| collector | `pytest` | 23 из 23 (Canvas, Teams, itslearning, InSchool, Classroom, HTTP-сервис) |
| Бэкенд | `mvn package` | 95 тестов, 0 упало |
| Бэкенд | интеграционные тесты с `TEST_DB_URL` на базе V22 | 95 из 95, пропущенных нет |
| Бэкенд + collector + база | запуск вместе | `/actuator/health` → `UP`, в логе «collector: связь есть, ключ подходит» |
| Фронт | `pnpm test` | 114 из 114 |
| Фронт | `pnpm build` | ✓ (предупреждение: бандл > 500 кБ) |
| Фронт ↔ бэк | каждый вызов из `frontend/src/api/endpoints.ts` сверен с OpenAPI бэкенда (путь + метод) | 40 из 40 есть |

### Живые запросы к собранному бэкенду

| Запрос | Ответ |
|---|---|
| `POST /auth/register` → код в логе (`MAIL_DEV_LOG_CODES=true`) → `POST /auth/register/verify` | 200, токен |
| `GET /users/me`, `/homework`, `/dashboard`, `/history`, `/subjects`, `/quizzes`, `/flashcards/stats`, `/chats` | 200 |
| `GET /platforms` | 200, 5 платформ: canvas, teams, itslearning, inschool, classroom |
| `POST /homework/sync` без подключений | 200 `{"sources":[],"added":0,"updated":0}` |
| `GET /platforms/classroom/url?redirectUri=http://localhost:5173/settings/platforms` | 200, ссылка на accounts.google.com |
| то же с чужим `redirectUri` | 400 (защита от открытого редиректа) |
| `POST /platforms/canvas` с чужим доменом | 400 «Адрес платформы не поддерживается» (защита от SSRF) |
| `POST /chats` | 200, чат в режиме tutor |
| `GET /platforms` без токена | 401 |

## Что не проверено здесь

- Настоящие платформы и Google: из песочницы к ним нет выхода. Classroom проверил №2 на своём аккаунте (28 заданий за ~6 с).
- Ответы ИИ с настоящим ключом Groq (`ai-content/check/check_ai.py` не запускался).
- Письма через Gmail: коды проверены через лог.
- Сайт в браузере (e2e): фронт проверен тестами и сборкой.

## Найденные проблемы

| # | Где | Что | Важно |
|---|---|---|---|
| 1 | `backend/.env.example`, строка `MAIL_PASSWORD` | вписано значение, похожее на настоящий пароль приложения Gmail | **да**: отозвать и заменить пустым |
| 2 | облачная база Railway | была на V21; V22 накатили 7 ноября в 20:22 | исправлено |
| 3 | `ai-content/materials` | пусто: `GET /subjects` → `[]`, ИИ отвечает без учебных материалов | желательно |
| 4 | `docs/*.md`, `backend/README_CHANGES.md` | в 7 файлах ещё упоминается `main-test-3.0` как рабочая ветка | нет |
| 5 | фронт | бандл больше 500 кБ | нет |
