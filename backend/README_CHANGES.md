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
