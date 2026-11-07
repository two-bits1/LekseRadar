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
