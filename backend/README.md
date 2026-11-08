# LekseRadar — Java API (Spring Boot)

Spring Boot 3.5 · Java 21 · PostgreSQL 16 · Spring Security + JWT · OpenAI · Swagger UI

Бэкенд — это только REST API (JSON). Экраны делает фронтенд (React + Tailwind).
Схему базы бэкенд **не создаёт и не меняет**: таблицы, виды и функции создаются миграциями
из общей папки [`database/`](../database) при первом запуске `docker compose`.

## Что нужно установить

| Что | Зачем | Команда (PowerShell) |
|---|---|---|
| JDK 21 | бэкенд | `winget install EclipseAdoptium.Temurin.21.JDK` |
| Docker Desktop | PostgreSQL | `winget install Docker.DockerDesktop` |
| Node.js 22 LTS | фронтенд | `winget install OpenJS.NodeJS.LTS` |

Maven ставить не нужно: `mvnw` скачает его сам. После установки перезапустите VS Code.

## Запуск

### 1. База (один раз, дальше она работает в фоне)

```bash
docker compose up -d db content collector   # from the repo root: database, AI prompts, collector
```

При первом запуске создаются все таблицы. База пустая, пробных данных нет: ученик появляется только через регистрацию.
Сбросить базу: `docker compose down -v && docker compose up -d`.

### 2. Промпты ИИ в базу (один раз и после каждого изменения промптов)

Промпты лежат в `ai-content/prompts`, база — в `database/` (обе в ветке `PythonAPI`, миграции V1–V20). Без них чат, квизы и карточки от ИИ отвечают `503`.
```bash
pip install -r ai-content/load/requirements.txt
DATABASE_URL=postgresql://lekseradar:<DB_PASSWORD>@localhost:5432/lekseradar python -X utf8 ai-content/load/load.py
```

### 3. Секреты

Скопируйте `backend/.env.example` в `backend/.env` и впишите `OPENAI_API_KEY`, `JWT_SECRET` и `CREDENTIALS_KEY` (`openssl rand -base64 32`).
`.env` в git не попадает.

### 4. Бэкенд

- **VS Code:** F5 (конфигурация `▶ LekseRadar API`) или кнопка **Run** над `main()` в `LekseRadarApplication`.
- **IntelliJ IDEA:** откройте папку `backend` и нажмите ▶ рядом с `main`.
- **Терминал:** `cd backend` → `./mvnw spring-boot:run` (PowerShell: `.\mvnw.cmd spring-boot:run`).

Когда в логе появится `Started LekseRadarApplication`, API работает на **http://localhost:8080**.

## Как проверить

1. http://localhost:8080/actuator/health → `{"status":"UP"}`. Если `DOWN`, база не запущена.
2. http://localhost:8080/swagger-ui.html:
   1. `POST /api/v1/auth/register` → **Try it out** → `{"email":"ola@example.com","password":"hemmelig123","displayName":"Ola"}` → **Execute**.
   2. Скопируйте `token`, нажмите **Authorize** (замок сверху) и вставьте его.
   3. `POST /api/v1/chats` создаст чат, `POST /api/v1/chats/{id}/messages` задаст вопрос настоящему ИИ.
3. Unit-тесты (база не нужна): `./mvnw test`.
4. Все сущности и репозитории на настоящей базе (в транзакции с откатом, в базе ничего не остаётся):
   `TEST_DB_URL=jdbc:postgresql://localhost:5432/lekseradar TEST_DB_USER=lekseradar TEST_DB_PASSWORD=<DB_PASSWORD> ./mvnw test`.
   Без `TEST_DB_URL` этот тест пропускается.

При старте Hibernate сверяет все сущности с таблицами (`ddl-auto: validate`). Если миграции в `database/`
не применены или не совпадают с кодом, бэкенд не запустится и напишет, какая колонка не та.

## API

Базовый адрес `/api/v1`. Все запросы, кроме `/auth/**`, требуют заголовок `Authorization: Bearer <token>`.

### Авторизация
| Метод | URL | Тело | Ответ |
|---|---|---|---|
| POST | `/api/v1/auth/register` | `{email, password, displayName}` | `201 {token, user}` |
| POST | `/api/v1/auth/login` | `{email, password}` | `200 {token, user}` |

### Профиль
| Метод | URL | Тело | Ответ |
|---|---|---|---|
| GET | `/api/v1/users/me` | — | `user` |
| PATCH | `/api/v1/users/me` | `{displayName?, locale?: nb/nn/en/ru, timezone?, emailReminders?}` | `user` |
| PUT | `/api/v1/users/me/password` | `{currentPassword, newPassword}` | `204` |
| POST | `/api/v1/users/me/reminders/test` | — | `{email, count}`; пробное письмо о ДЗ на 7 дней вперёд, 3 раза в час, `503` без почты |

`user = {id, email, displayName, locale, timezone, createdAt, lastLoginAt}`

### ИИ-чат
| Метод | URL | Тело / параметры | Ответ |
|---|---|---|---|
| GET | `/api/v1/chats` | `?archived=true/false`, без параметра — все | `[chat]`, последние сверху |
| POST | `/api/v1/chats` | `{title?, topic?}` или пусто | `201 chat` |
| GET | `/api/v1/chats/{chatId}` | — | `chat` |
| PATCH | `/api/v1/chats/{chatId}` | `{title?, topic?, archived?}` | `chat` |
| DELETE | `/api/v1/chats/{chatId}` | — | `204` |
| GET | `/api/v1/chats/{chatId}/messages` | — | `[message]`, старые сверху |
| POST | `/api/v1/chats/{chatId}/messages` | `{content}` (до 8000 символов) | `201 {chat, userMessage, assistantMessage}` |

`chat = {id, title, topic, mode, subject, language, level, assignmentId, quizSetId, archived, createdAt, updatedAt}`,
`message = {id, role: "student" | "tutor", content, model, createdAt}`. Ответ тьютора приходит в Markdown.

Как чат работает с базой:
- создание — функция `start_chat` (режим `tutor`/`quiz`, предмет, тема, ДЗ; она же пишет `chat_started`);
- промпт — `chat_system_prompt` (активная версия из `ai_prompts`), переменные `{{...}}` — `chat_prompt_vars`,
  `{{materials}}` — `materials_for_prompt` по вопросу;
- история — последние 10 реплик (вид `chat_context`);
- язык ИИ — язык сайта (`users.locale`) или свой язык чата: `{{language}}` = «русский (ru)», `{{language_code}}` = `ru` (V17, V20);
- в `chat_messages.prompt_id` пишется версия промпта;
- чат `quiz`: сообщение ученика → ИИ составляет квиз (как `POST /quizzes`), в ответе `chat.quizSetId`.

### Квизы и флеш-карты
| Метод | URL | Тело / параметры | Ответ |
|---|---|---|---|
| GET | `/api/v1/subjects` | — | `[subject]` |
| GET | `/api/v1/quizzes` | — | `[quizSummary]`, новые сверху |
| GET | `/api/v1/quizzes/progress` | — | `[progress]` по предметам и темам |
| POST | `/api/v1/quizzes` | `{subject, topic, level?, count?, assignmentId?, fresh?}` | `201 quiz` |
| GET | `/api/v1/quizzes/{quizId}` | — | `quiz` (без правильных ответов) |
| POST | `/api/v1/quizzes/{quizId}/answers` | `{position, answer}` | `{verdict, explanation, correctAnswer}` |
| POST | `/api/v1/quizzes/{quizId}/finish` | — | `{score}` 0–100 |
| POST | `/api/v1/quiz-sets/{setId}/start` | — | `201 quiz` |
| POST | `/api/v1/quiz-sets/{setId}/flashcards` | — | `deck` |
| GET | `/api/v1/flashcards` | `?due=true/false` | `[flashcard]` |
| GET | `/api/v1/flashcards/due` | — | `[flashcard]` к повторению |
| GET | `/api/v1/flashcards/stats` | — | `{cards, dueNow, learned, reviews}` |
| POST | `/api/v1/flashcards/{exerciseId}/review` | `{grade: 0–5}` | `{nextReviewAt, intervalDays}` |
| POST | `/api/v1/flashcards/generate` | как у `POST /quizzes` | `201 deck` |

Как ИИ работает с квизами (промпты из `ai-content/prompts`, в базе — `ai_prompts`):
1. `find_quiz_set` — есть готовый набор (предмет + тема + уровень + язык)? Берём его, ИИ не нужен.
2. Нет → `start_chat(mode=quiz)` → промпт `quiz_generator` + `chat_prompt_vars` + `{{materials}}` + `{{count}}` →
   ИИ отвечает JSON → `import_quiz_json`. Неверный JSON — один повтор с текстом ошибки, потом `502`.
3. `start_quiz` → ответы `save_quiz_answer`: выбор проверяет база, `text` — ИИ по промпту `answer_checker`.
4. Неверный ответ → карточка (триггер базы). `finish_quiz` → балл.
5. Флеш-карты — те же вопросы: `add_quiz_set_to_flashcards`, `review_flashcard` (SM-2).

Все адреса сайта (страницы фронтенда + API) — [`docs/URLS.md`](../docs/URLS.md).

### Служебное
`GET /actuator/health` → `{"status": "UP" | "DOWN"}`, без токена.

### Ошибки
```json
{"status": 400, "message": "Проверьте введённые данные", "fields": {"email": "Некорректный email"}, "timestamp": "..."}
```
| Код | Когда |
|---|---|
| 400 | невалидное тело или параметр (`fields` — ошибки по полям) |
| 401 | нет токена, токен неверный или истёк, неверный логин или пароль |
| 404 | объект не найден или принадлежит другому пользователю; неизвестный URL |
| 409 | email уже занят |
| 502 | ИИ недоступен (неверный ключ, лимит, нет сети) или дважды прислал квиз в неверном формате |
| 503 | промпты не загружены в базу (`ai-content/load/load.py`) |

## Структура

```
src/main/java/com/lekseradar/
  config/       *Config: AppConfig (настройки app.*), SecurityConfig, CorsConfig, AiConfig, OpenApiConfig
  controller/   *Controller: REST-контроллеры, принимают запрос и вызывают сервис
  dto/          *DTO (LoginRequestDTO, ChatResponseDTO…): запросы и ответы API по разделам (auth, user, chat, quiz, homework…)
    ai/         AiMessageRequestDTO, AiResponseDTO — сообщения для модели ИИ
  exception/    *Exception и GlobalExceptionHandler (единый формат ошибок)
  model/        *Entity: JPA-сущности, по одной на таблицу из database/migrations (20 штук)
    enums/      *Enum: Java-enum'ы для PostgreSQL-типов (chat_mode, platform, quiz_status…)
    converter/  *Converter: enum ↔ значение в базе
  repository/   *Repository: доступ к БД, вызовы функций базы (start_quiz, save_quiz_answer…)
  security/     JWT: JwtService, JwtAuthenticationFilter, @CurrentUser, ответ 401
  service/      *Service: бизнес-логика
                PromptService (промпты из ai_prompts), QuizGeneratorService (ИИ → квиз),
                AnswerCheckerService (ИИ проверяет текст), QuizService, FlashcardService, ChatService
    ai/         AiService (интерфейс), OpenAiService (OpenAI-совместимый API: OpenAI, Groq…)
```

Промпты в коде не хранятся: они в таблице `ai_prompts` (загружает `ai-content/load/load.py`).

## Переменные окружения (`backend/.env`)

| Переменная | По умолчанию | Описание |
|---|---|---|
| `OPENAI_API_KEY` | — | **Обязательно.** Ключ OpenAI |
| `JWT_SECRET` | — | **Обязательно.** Секрет подписи JWT, ≥ 32 символов |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | — | **Обязательно для регистрации.** Gmail и пароль приложения (https://myaccount.google.com/apppasswords): с него уходят коды подтверждения |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_FROM` | `smtp.gmail.com` / `587` / `MAIL_USERNAME` | Другой SMTP, адрес отправителя |
| `MAIL_DEV_LOG_CODES` | `false` | Без почты печатать код в лог (только разработка) |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | — | Google Classroom: OAuth-клиент из Google Cloud (Google Auth Platform → Clients). Без них Classroom отвечает 503, остальное работает. Адрес возврата — каждый сайт из `CORS_ORIGINS` + `/settings/platforms`, он же в Authorized redirect URIs |
| `CREDENTIALS_KEY` | — | **Обязательно.** Ключ шифрования доступов к платформам, `openssl rand -base64 32`. Не терять: без него все подключения делать заново |
| `COLLECTOR_URL` | `http://localhost:8090` | Сервис сбора ДЗ (`homework-collector`), только внутренняя сеть |
| `COLLECTOR_KEY` | — | Общий ключ с collector (`X-Internal-Key`) |
| `CANVAS_HOSTS` | — | Свои Canvas вне `*.instructure.com`, через запятую (`canvas.uio.no`) |
| `SYNC_SCHEDULE_ENABLED` | `true` | Обновлять ДЗ раз в 30 минут |
| `REMINDERS_ENABLED` | `true` | Раз в 15 минут письмо о несданном ДЗ со сроком в ближайшие 24 часа (нужны `MAIL_*`) |
| `OPENAI_MODEL` | `gpt-4o-mini` | Модель |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | Совместимый прокси, если нужен |
| `JWT_TTL_HOURS` | `72` | Время жизни токена |
| `DB_URL` | `jdbc:postgresql://localhost:5432/lekseradar` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `lekseradar` / `lekseradar` | Доступ к БД |
| `CORS_ORIGINS` | `http://localhost:5173` | Адрес фронтенда (через запятую, если несколько) |
| `PORT` | `8080` | Порт API |
