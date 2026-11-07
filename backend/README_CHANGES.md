# Что сделано / What was done

Отчёт обновляется каждые 5 коммитов. / Updated every 5 commits.

---

## Часть 1 / Part 1 — коммиты / commits 1–5

### RU
1. **Maven Wrapper** (`mvnw`, `mvnw.cmd`): Maven устанавливать не нужно, нужен только JDK 21.
2. **Общая схема БД.** Удалены своя миграция `V1__init.sql`, Flyway, H2 и `docker-compose.yml` из backend.
   Таблицы создаёт папка `database/` (общая для команды). Добавлен Swagger UI.
3. **`model/`** — JPA-сущности под таблицы из `database/migrations`:
   `User` (язык, часовой пояс, время входа), `ChatSession` (тема, архив),
   `ChatMessage` (роль `student`/`tutor`, модель ИИ, токены). Старые упражнения убраны: их заменят квизы.
4. **`repository/`** — все репозитории в одном пакете. История для ИИ берётся из вида `chat_context`,
   действия пользователя пишутся в `user_events`.
5. **`dto/`** — запросы и ответы API, по одному record на файл: `auth`, `user`, `chat`, `common`.

### EN
1. **Maven Wrapper** (`mvnw`, `mvnw.cmd`): no Maven install needed, only JDK 21.
2. **Shared DB schema.** Removed own `V1__init.sql` migration, Flyway, H2 and backend `docker-compose.yml`.
   Tables are created by the team's `database/` folder. Added Swagger UI.
3. **`model/`** — JPA entities mapped to `database/migrations` tables:
   `User` (locale, timezone, last login), `ChatSession` (topic, archived),
   `ChatMessage` (`student`/`tutor` role, AI model, tokens). Old exercises removed: quizzes will replace them.
4. **`repository/`** — all repositories in one package. AI history comes from the `chat_context` view,
   user actions are written to `user_events`.
5. **`dto/`** — API requests and responses, one record per file: `auth`, `user`, `chat`, `common`.

---

## Часть 2 / Part 2 — коммиты / commits 6–10

### RU
6. **`exception/`** — ошибки с HTTP-кодом и единый формат ответа `{status, message, fields, timestamp}`.
   Неизвестный URL → 404, неверный параметр → 400 (раньше было 500).
7. **`security/`** — вход по JWT: выдача и проверка токена, фильтр заголовка `Authorization: Bearer`,
   `@CurrentUser`, ответ 401 в JSON. Без страниц входа и сессий: экраны делает фронтенд.
8. **`config/`** — Spring Security (открыты только вход, health и Swagger), CORS для фронта,
   секреты из `backend/.env` (`OPENAI_API_KEY` и `JWT_SECRET` обязательны). Бэкенд не меняет схему БД (`ddl-auto: none`).
9. **ИИ** — только настоящий OpenAI (заглушка удалена). Сохраняются модель и токены,
   тьютор отвечает на языке ученика.
10. **`service/`** — логика: регистрация и вход (время входа, событие `login`), профиль
    (имя, язык, часовой пояс, пароль), чаты (архив, ответы ИИ по истории `chat_context`).

### EN
6. **`exception/`** — errors with HTTP status and one response format `{status, message, fields, timestamp}`.
   Unknown URL → 404, bad parameter → 400 (was 500).
7. **`security/`** — JWT login: token issue/verify, `Authorization: Bearer` filter,
   `@CurrentUser`, JSON 401. No login pages or sessions: the frontend owns all screens.
8. **`config/`** — Spring Security (only login, health and Swagger are public), CORS for the frontend,
   secrets from `backend/.env` (`OPENAI_API_KEY` and `JWT_SECRET` required). Backend never changes the DB schema (`ddl-auto: none`).
9. **AI** — real OpenAI only (mock removed). Model and tokens are stored,
   the tutor answers in the student's language.
10. **`service/`** — business logic: register/login (last login, `login` event), profile
    (name, language, timezone, password), chats (archive, AI replies using `chat_context` history).

---

## Часть 3 / Part 3 — коммиты / commits 11–15

### RU
11. **`controller/`** — REST API по адресу `/api/v1`: вход, профиль, пароль, чаты и сообщения.
    Контроллеры только вызывают сервисы. Проверка сервера: `/actuator/health`.
12. **Тесты** — unit-тесты JWT без базы. Старый тест на H2 удалён.
13. **`backend/README.md`** — установка, запуск базы, `.env`, запуск из VS Code / IntelliJ / терминала.
14. **`docs/API.md`** — документация для фронтенда: все URL, типы TypeScript, коды ошибок, пример `fetch`.
15. **VS Code** — запуск по F5, задачи для базы, бэкенда, тестов и фронтенда.

**Проверено вручную** на PostgreSQL из `database/`: вход демо-ученика, профиль, чаты, архив, удаление,
чужой чат → 404, ошибки валидации на русском, события в `user_events`. ИИ отвечает 502, пока на счету OpenAI нет денег.

### EN
11. **`controller/`** — REST API under `/api/v1`: login, profile, password, chats and messages.
    Controllers only call services. Health check: `/actuator/health`.
12. **Tests** — JWT unit tests, no database needed. Old H2 test removed.
13. **`backend/README.md`** — install, start DB, `.env`, run from VS Code / IntelliJ / terminal.
14. **`docs/API.md`** — frontend reference: every URL, TypeScript types, error codes, `fetch` example.
15. **VS Code** — F5 to run, tasks for DB, backend, tests and frontend.

**Manually verified** against PostgreSQL from `database/`: demo login, profile, chats, archive, delete,
foreign chat → 404, validation errors, `user_events` rows. AI returns 502 until the OpenAI account has credits.

---

## Часть 4 / Part 4 — коммиты / commits 16–20

### RU
16. **`docs/API.md`: таблица всех URL** бэкенда одним списком.
17. **`docs/API.md`: страницы фронтенда** (`/`, `/login`, `/profile`, `/chat`…) и какие API они вызывают.
18. **Groq** как бесплатный ИИ в `.env.example`: код менять не нужно, только `OPENAI_BASE_URL`, ключ и модель.
19. **`docs/API.md`: исправлен пример `fetch`**: неверный пароль (401 на `/auth/login`) больше не перекидывает на `/login`.
20. **Вернул сущности и репозитории** (`git revert 2b943e1`, по `ENTITIES-PLAN.md`): 20 сущностей, 20 репозиториев,
    9 enum'ов PostgreSQL, интеграционный тест.

### EN
16. **`docs/API.md`: table of all backend URLs.**
17. **`docs/API.md`: frontend pages** (`/`, `/login`, `/profile`, `/chat`…) and the API calls each one makes.
18. **Groq** as a free AI provider in `.env.example`: no code change, only `OPENAI_BASE_URL`, key and model.
19. **`docs/API.md`: fixed `fetch` example**: wrong password (401 on `/auth/login`) no longer redirects to `/login`.
20. **Restored entities and repositories** (`git revert 2b943e1`, per `ENTITIES-PLAN.md`): 20 entities, 20 repositories,
    9 PostgreSQL enums, integration test.

---

## Часть 5 / Part 5 — коммиты / commits 21–26

### RU
21. **DTO для ДЗ и платформ:** `AssignmentResponse`, `SyncResponse`, `SourceStatusResponse`, `PlatformResponse`.
22. **DTO для квизов:** `SubjectResponse`, `StartQuizRequest`, `QuizResponse`, `QuestionResponse` (без правильного ответа),
    `AnswerRequest`, `AnswerResponse`, `FinishResponse`.
23. **DTO для карточек, дашборда, истории и материалов:** `FlashcardResponse`, `ReviewRequest/Response`,
    `DashboardResponse` и его части, `EventResponse`, `MaterialResponse`.
24. **Имена файлов по папке:** сущности `*Entity`, enum'ы в `model/enums`, части DTO `*Response`,
    ИИ — `AiService`/`OpenAiService`, сообщения ИИ — `dto/ai`, `AppProperties` → `AppConfig`.
25. **Чат на промптах из базы:** `start_chat`, `chat_system_prompt`, `chat_prompt_vars`, материалы в промпте,
    выбор языка (`[[LANGUAGE=xx]]` → язык чата), `prompt_id` в ответе, 503 если промпты не загружены.
    Новые поля чата: `mode`, `subject`, `language`, `level`, `assignmentId`, `quizSetId`. Язык профиля `uk`.
26. **Документация** под код: `docs/API.md`, `backend/README.md`.

**Проверено** на пустой базе V1–V16 (без пробных данных) и настоящем ИИ (Groq):
8 из 8 тестов (5 интеграционных, 3 JWT), после тестов в базе 0 пользователей; `ddl-auto: validate` — все 20 сущностей
совпадают со схемой; вход `demo@…` → 401; регистрация → чат с предметом → тьютор спросил язык → «Русский» →
язык чата `ru`, ответ по-русски по теме, метки вырезаны, `prompt_id` записан.

### EN
21. **Homework and platform DTOs:** `AssignmentResponse`, `SyncResponse`, `SourceStatusResponse`, `PlatformResponse`.
22. **Quiz DTOs:** `SubjectResponse`, `StartQuizRequest`, `QuizResponse`, `QuestionResponse` (no correct answer),
    `AnswerRequest`, `AnswerResponse`, `FinishResponse`.
23. **Flashcard, dashboard, history and material DTOs:** `FlashcardResponse`, `ReviewRequest/Response`,
    `DashboardResponse` and its parts, `EventResponse`, `MaterialResponse`.
24. **File names match their folder:** entities `*Entity`, enums in `model/enums`, DTO parts `*Response`,
    AI — `AiService`/`OpenAiService`, AI messages in `dto/ai`, `AppProperties` → `AppConfig`.
25. **Chat uses prompts from the database:** `start_chat`, `chat_system_prompt`, `chat_prompt_vars`, materials in the prompt,
    language selection (`[[LANGUAGE=xx]]` → chat language), `prompt_id` on replies, 503 when prompts are not loaded.
    New chat fields: `mode`, `subject`, `language`, `level`, `assignmentId`, `quizSetId`. Profile locale `uk`.
26. **Docs** match the code: `docs/API.md`, `backend/README.md`.

**Verified** on an empty V1–V16 database (no demo data) with a real AI (Groq):
8/8 tests (5 integration, 3 JWT), 0 users left after tests; `ddl-auto: validate` — all 20 entities match the schema;
`demo@…` login → 401; register → chat with subject → tutor asked for language → "Русский" →
chat language `ru`, answer in Russian on topic, tags stripped, `prompt_id` stored.

---

## Часть 6 / Part 6 — коммиты / commits 27–31

### RU
27. **Имена классов:** все DTO заканчиваются на `DTO`, все enum'ы — на `Enum`.
28. **ИИ делает квизы и флеш-карты.** Промпты берутся из базы (`ai_prompts`), туда их грузит `load.py` из
    `ai-content/prompts` на GitHub:
    - `PromptService` — активный промпт, переменные `{{...}}`, 503 если промпт не загружен;
    - `QuizGeneratorService` — промпт `quiz_generator` → JSON → `import_quiz_json`; при неверном JSON один повтор;
      сначала ищется готовый набор (`find_quiz_set`), чтобы не тратить ИИ; пишутся `prompt_id` и модель;
    - `AnswerCheckerService` — промпт `answer_checker` проверяет текстовые ответы;
    - квизы: `/subjects`, `/quizzes`, `/quizzes/{id}/answers`, `/finish`, `/progress`, `/quiz-sets/{id}/start`;
    - флеш-карты: `/flashcards`, `/due`, `/stats`, `/{id}/review`, `/generate`, `/quiz-sets/{id}/flashcards`;
    - чат `quiz`: сообщение ученика → ИИ составляет квиз, в ответе `chat.quizSetId`;
    - unit-тесты разбора JSON квиза.
29. **`docs/URLS.md`** — все адреса сайта: страницы фронтенда, API, адреса для разработки, готовые TS-константы.
30. **Документация:** квизы, флеш-карты, квиз-чат и откуда берутся промпты — в `docs/API.md` и `backend/README.md`.
31. Этот отчёт.

**Проверено** на базе V1–V16 и настоящем ИИ (Groq `openai/gpt-oss-120b`): 13 из 13 тестов; квиз от ИИ за ~4 с,
тот же квиз второй раз из базы за 0,05 с без ИИ; выбор проверяет база (`correct` / `partial` / `incorrect`),
текст — ИИ («2/3» → `correct`, «vet ikke» → `incorrect` с подсказкой); балл 50 %; ошибки стали карточками;
карточки от ИИ по новой теме; квиз-чат; чужой квиз → 404; тестовые данные удалены, база снова пустая.

### EN
27. **Class names:** every DTO ends with `DTO`, every enum with `Enum`.
28. **AI builds quizzes and flashcards.** Prompts come from the database (`ai_prompts`), loaded by `load.py`
    from `ai-content/prompts` on GitHub:
    - `PromptService` — active prompt, `{{...}}` variables, 503 if the prompt is not loaded;
    - `QuizGeneratorService` — `quiz_generator` prompt → JSON → `import_quiz_json`; one retry on bad JSON;
      ready sets are reused first (`find_quiz_set`) to save AI calls; `prompt_id` and model are stored;
    - `AnswerCheckerService` — `answer_checker` prompt grades text answers;
    - quizzes: `/subjects`, `/quizzes`, `/quizzes/{id}/answers`, `/finish`, `/progress`, `/quiz-sets/{id}/start`;
    - flashcards: `/flashcards`, `/due`, `/stats`, `/{id}/review`, `/generate`, `/quiz-sets/{id}/flashcards`;
    - `quiz` chat: a student message → AI builds a quiz, the reply carries `chat.quizSetId`;
    - unit tests for quiz JSON parsing.
29. **`docs/URLS.md`** — every site address: frontend pages, API, dev addresses, ready TS constants.
30. **Docs:** quizzes, flashcards, quiz chat and where prompts come from — in `docs/API.md` and `backend/README.md`.
31. This report.

**Verified** on a V1–V16 database with a real AI (Groq `openai/gpt-oss-120b`): 13/13 tests; AI quiz in ~4 s,
the same quiz again from the database in 0.05 s with no AI call; choices graded by the database
(`correct` / `partial` / `incorrect`), text by the AI ("2/3" → `correct`, "vet ikke" → `incorrect` with a hint);
score 50%; wrong answers became flashcards; AI flashcards for a new topic; quiz chat; foreign quiz → 404;
test data removed, the database is empty again.

---

## Часть 7 / Part 7 — коммиты / commits 32–36

### RU
32. **ИИ:** при лимите Groq (429) запрос повторяется, если ждать нужно недолго.
33. **Язык квиза** берётся из `language_code` (V20); ошибки ответа в квизе (V19) — 400, а не 500.
34. **Деплой:** `Dockerfile` и `railway.json` для Railway.
35. **`RAILWAY.md`** — деплой по шагам.
36. **Платформы, сбор ДЗ и регистрация по почте** (по `docs/PLAN-BACKEND.md`) и этот отчёт:
    - в бэкенд перенесены ДЗ, дашборд, история и лимиты из `main-test-3.0`;
    - `CredentialsCipher` — доступы к платформам шифруются AES-256-GCM (`CREDENTIALS_KEY`, обязателен);
    - `CollectorClient` — клиент к `collector` (`COLLECTOR-API.md`): 400 → 422, 502 → 502, остальное → 503, 401 наружу нет;
      при старте пишет в лог, есть ли связь и подходит ли ключ; ключ по умолчанию как в `docker-compose`;
    - `GET/POST/DELETE /platforms/{platform}` (тело как у фронта, snake_case), вход в Teams по коду,
      ссылка для itslearning; проверка адреса против SSRF, лимит 10 подключений за 10 минут;
      при подключении задания сразу пишутся в базу (`sync_assignments`, V21);
    - `POST /homework/sync` (раз в минуту, `Retry-After: 60`) и обновление раз в 30 минут по расписанию;
    - регистрация в два шага: код из 6 цифр на email (Gmail SMTP), `POST /auth/register/verify`;
      лимиты писем, 5 попыток кода, 15 минут;
    - пароль длиннее 72 байт — 400 с понятным текстом (раньше 500); ожидание после 429 от Groq — до 20 с;
    - `docs/FIX-frontend-auth-platforms.md` — что поменять во фронте.

**Проверено:** 73 из 73 тестов (с базой V1–V21); через сайт с настоящими Gmail, Groq и collector — 105 проверок;
код из письма, успешное подключение платформы, расписание и длинные пароли — 21 из 21; `check_ai.py` — 6 из 6;
Docker-образ собирается и запускается.

### EN
32. **AI:** on a Groq rate limit (429) the request is retried when the wait is short.
33. **Quiz language** comes from `language_code` (V20); quiz answer errors (V19) are 400, not 500.
34. **Deploy:** `Dockerfile` and `railway.json` for Railway.
35. **`RAILWAY.md`** — step-by-step deploy.
36. **Platforms, homework sync and email sign-up** (per `docs/PLAN-BACKEND.md`) and this report:
    - homework, dashboard, history and rate limits moved in from `main-test-3.0`;
    - `CredentialsCipher` — platform credentials encrypted with AES-256-GCM (`CREDENTIALS_KEY`, required);
    - `CollectorClient` — client for `collector` (`COLLECTOR-API.md`): 400 → 422, 502 → 502, the rest → 503, never 401;
      logs on startup whether the collector is reachable and the key fits; default key matches `docker-compose`;
    - `GET/POST/DELETE /platforms/{platform}` (body as the frontend sends it, snake_case), Teams device-code login,
      itslearning login link; SSRF address check, 10 connects per 10 minutes;
      homework is saved right on connect (`sync_assignments`, V21);
    - `POST /homework/sync` (once a minute, `Retry-After: 60`) and a scheduled sync every 30 minutes;
    - two-step sign-up: a 6-digit code by email (Gmail SMTP), `POST /auth/register/verify`;
      mail limits, 5 code attempts, 15 minutes;
    - passwords over 72 bytes get a clear 400 (was 500); waiting after a Groq 429 — up to 20 s;
    - `docs/FIX-frontend-auth-platforms.md` — what to change in the frontend.

**Verified:** 73/73 tests (on a V1–V21 database); through the site with real Gmail, Groq and collector — 105 checks;
email code, successful platform connect, scheduled sync and long passwords — 21/21; `check_ai.py` — 6/6;
the Docker image builds and runs.
