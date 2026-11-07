# LekseRadar — Java API (Spring Boot)

Spring Boot 3.5 · Java 21 · PostgreSQL 16 · Flyway · Spring Security + JWT · OpenAI Chat Completions

## Запуск

```bash
docker compose up -d                        # PostgreSQL на localhost:5432
export OPENAI_API_KEY=sk-...                # без ключа ИИ работает в режиме заглушки
mvn spring-boot:run                         # API на http://localhost:8080
```

Тесты (H2 в памяти, PostgreSQL не нужен): `mvn test`

## Переменные окружения

| Переменная | По умолчанию | Описание |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/lekseradar` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `lekseradar` / `lekseradar` | Доступ к БД |
| `JWT_SECRET` | dev-значение | Секрет подписи JWT, ≥ 32 символов. **Обязательно задать в проде** |
| `JWT_TTL_HOURS` | `72` | Время жизни токена |
| `CORS_ORIGINS` | `http://localhost:5173` | Разрешённые origin'ы фронтенда (через запятую) |
| `OPENAI_API_KEY` | — | Ключ OpenAI |
| `OPENAI_MODEL` | `gpt-4o-mini` | Модель |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | Можно указать совместимый прокси |
| `OPENAI_MOCK` | `true` | Без ключа отвечать заглушками (для разработки фронта) |

## API

Все эндпоинты, кроме `/api/auth/**` и `/api/health`, требуют заголовок `Authorization: Bearer <token>`.
Ошибки возвращаются в виде `{"status", "message", "fields", "timestamp"}`; в `fields` ошибки валидации по полям.

### Аутентификация и пользователи
| Метод | Путь | Тело | Ответ |
|---|---|---|---|
| POST | `/api/auth/register` | `{email, password, displayName}` | `201 {token, user}` |
| POST | `/api/auth/login` | `{email, password}` | `{token, user}` |
| GET | `/api/users/me` | — | `{id, email, displayName, createdAt}` |
| PUT | `/api/users/me` | `{displayName?, currentPassword?, newPassword?}` | `user` |

### Чат с ИИ-тьютором (история хранится в БД)
| Метод | Путь | Тело | Ответ |
|---|---|---|---|
| GET | `/api/chats` | — | `[{id, title, subject, createdAt, updatedAt}]` |
| POST | `/api/chats` | `{title?, subject?}` | `201 session` |
| PATCH | `/api/chats/{id}` | `{title?, subject?}` | `session` |
| DELETE | `/api/chats/{id}` | — | `204` |
| GET | `/api/chats/{id}/messages` | — | `[{id, role: USER/ASSISTANT, content, createdAt}]` |
| POST | `/api/chats/{id}/messages` | `{content}` | `{session, userMessage, assistantMessage}` |

Ответ ИИ (`content`) в Markdown. Название чата задаётся по первому вопросу. В ИИ уходят последние 20 сообщений чата.

### Упражнения
| Метод | Путь | Тело / параметры | Ответ |
|---|---|---|---|
| POST | `/api/exercises/generate` | `{subject, topic, difficulty: EASY/MEDIUM/HARD, count: 1..10, type?: MIXED/CHOICE/OPEN}` | `201 [exercise]` |
| GET | `/api/exercises` | `?subject=&page=0&size=20` | `{items, page, size, total}` |
| GET | `/api/exercises/subjects` | — | `["Математика", ...]` |
| GET | `/api/exercises/{id}` | — | `exercise` |
| POST | `/api/exercises/{id}/answer` | `{answer}` | `exercise` с новой попыткой в `attempts[0]` |

`exercise = {id, subject, topic, difficulty, type, question, options, solution, createdAt, attempts: [{id, answer, correct, score, feedback, createdAt}]}`.
`solution` равен `null`, пока нет ни одной попытки. Для `CHOICE` правильность определяется точным совпадением с вариантом, ИИ даёт только объяснение.

### Прогресс
`GET /api/progress?tz=Europe/Moscow` →
`{exercisesGenerated, attempts, correctAttempts, accuracy, averageScore, chatSessions, chatQuestions, streakDays, bySubject: [{subject, attempts, correct, averageScore}], daily: [{date, attempts}] (14 дней)}`

### Служебное
`GET /api/health` → `{status: "ok", aiMode: "openai" | "mock"}`

## Структура

```
src/main/java/com/lekseradar/
  ai/         AiClient, OpenAiClient, MockAiClient, Prompts (все промпты здесь)
  auth/       JWT, регистрация и вход
  user/       профиль
  chat/       сессии и сообщения чата
  exercise/   генерация, ответы, проверка
  progress/   статистика
  config/     Security, CORS, настройки
  common/     обработка ошибок, health
src/main/resources/db/migration/   схема БД (Flyway)
```

Если ИИ-часть будет вынесена в Python-сервис (FastAPI), достаточно сделать ещё одну реализацию `AiClient`,
остальной код менять не нужно.
