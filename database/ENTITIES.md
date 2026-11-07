# Сущности LekseRadar (PostgreSQL 16 + Spring Boot)

Все таблицы, перечисления, виды и функции базы и как они отображаются в бэкенде (JPA-сущности, DTO).
Источник правды — миграции `database/migrations/V1–V15`. Бэкенд таблицы не создаёт (`ddl-auto: none`),
поэтому поля сущностей должны совпадать с колонками ниже.

Как база встроена в проект: [общая схема](../docs/project-schema.png), [страницы → API → база](../docs/api-map.png),
SQL для каждого сценария — [README.md](README.md).

## Как читать

- **PostgreSQL** — тип колонки, **Java** — тип поля в JPA-сущности.
- **NULL** — «да», если колонка может быть пустой (в Java — обёртка `Long`, `Integer`, а не `long`).
- **Ключи:** PK — первичный ключ, FK → таблица.колонка и что происходит при удалении
  (CASCADE — удаляется вместе, SET NULL — ссылка обнуляется).
- **Бэкенд:** ✅ сущность уже есть в `backend/.../model`, 🕓 нужно создать, ⚙️ лучше не маппить в JPA,
  а вызывать функцию базы.
- Время везде `timestamptz` → `Instant` (UTC), в JSON — ISO-8601.

## Перечисления (PostgreSQL enum → Java enum)

В базе значения строчными буквами. В Java — enum с конвертером, как уже сделано для `chat_role`
(`MessageRole` + `MessageRoleConverter` + `@ColumnTransformer(write = "?::chat_role")`).
В JSON для фронта отдаём строчные значения, как в базе.

| PostgreSQL | Значения | Java enum | Где используется | Бэкенд |
|---|---|---|---|---|
| `chat_role` | `student`, `tutor`, `system` | `MessageRole` | `chat_messages.role` | ✅ |
| `chat_mode` | `tutor`, `quiz` | `ChatMode` | `chat_sessions.mode`, `chat_mode_prompts.mode` | 🕓 |
| `platform` | `canvas`, `teams`, `itslearning`, `inschool` | `Platform` | `platform_connections.platform`, `assignments.source` | 🕓 |
| `assignment_status` | `todo`, `overdue`, `submitted`, `graded` | `AssignmentStatus` | `assignments.status` | 🕓 |
| `exercise_level` | `beginner`, `intermediate`, `advanced` | `ExerciseLevel` | `exercises`, `quizzes`, `quiz_sets`, `chat_sessions.level` | 🕓 |
| `answer_type` | `exact`, `free` | `AnswerType` | `exercises.answer_type` | 🕓 |
| `verdict` | `correct`, `incorrect`, `partial` | `Verdict` | `exercise_attempts.verdict`, `save_quiz_answer()` | 🕓 |
| `quiz_status` | `in_progress`, `completed`, `abandoned` | `QuizStatus` | `quizzes.status` | 🕓 |
| `material_source` | `upload`, `url`, `platform`, `teacher`, `seed` | `MaterialSource` | `materials.source` | 🕓 |

Типы вопросов квиза — не enum, а текст с CHECK: `exercises.question_type` = `single_choice` | `multiple_choice` | `text`
(Java: `QuestionType`).
