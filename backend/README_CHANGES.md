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
