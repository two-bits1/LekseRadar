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
cd database
docker compose up -d
```

При первом запуске создаются все таблицы и демо-ученик **`demo@lekseradar.no` / `demo1234`**.
Сбросить базу: `docker compose down -v && docker compose up -d`.

### 2. Секреты

Скопируйте `backend/.env.example` в `backend/.env` и впишите `OPENAI_API_KEY` и `JWT_SECRET`.
`.env` в git не попадает.

### 3. Бэкенд

- **VS Code:** F5 (конфигурация `▶ LekseRadar API`) или кнопка **Run** над `main()` в `LekseRadarApplication`.
- **IntelliJ IDEA:** откройте папку `backend` и нажмите ▶ рядом с `main`.
- **Терминал:** `cd backend` → `./mvnw spring-boot:run` (PowerShell: `.\mvnw.cmd spring-boot:run`).

Когда в логе появится `Started LekseRadarApplication`, API работает на **http://localhost:8080**.

## Как проверить

1. http://localhost:8080/actuator/health → `{"status":"UP"}`. Если `DOWN`, база не запущена.
2. http://localhost:8080/swagger-ui.html:
   1. `POST /api/v1/auth/login` → **Try it out** → `{"email":"demo@lekseradar.no","password":"demo1234"}` → **Execute**.
   2. Скопируйте `token`, нажмите **Authorize** (замок сверху) и вставьте его.
   3. `GET /api/v1/chats` покажет демо-чат. `POST /api/v1/chats/1/messages` задаст вопрос настоящему ИИ.
3. Unit-тесты (база не нужна): `./mvnw test`.

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
| PATCH | `/api/v1/users/me` | `{displayName?, locale?: nb/nn/en/ru, timezone?}` | `user` |
| PUT | `/api/v1/users/me/password` | `{currentPassword, newPassword}` | `204` |

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

`chat = {id, title, topic, archived, createdAt, updatedAt}`,
`message = {id, role: "student" | "tutor", content, model, createdAt}`. Ответ тьютора приходит в Markdown.
В ИИ уходят последние 10 реплик (вид `chat_context`). Тьютор отвечает на языке ученика (`users.locale`).
Чат без названия получает его по первому вопросу.

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
| 502 | OpenAI недоступен (неверный ключ, лимит, нет сети) |

## Структура

```
src/main/java/com/lekseradar/
  config/       AppProperties, SecurityConfig, CorsConfig, AiConfig, OpenApiConfig
  controller/   REST-контроллеры: принимают запрос и вызывают сервис
  dto/          запросы и ответы API
  exception/    исключения и GlobalExceptionHandler (единый формат ошибок)
  model/        JPA-сущности поверх таблиц из database/migrations
  repository/   доступ к БД (Spring Data + JdbcTemplate для user_events)
  security/     JWT: JwtService, JwtAuthenticationFilter, @CurrentUser, ответ 401
  service/      бизнес-логика
    ai/         AiClient, OpenAiClient, Prompts (все промпты здесь)
```

## Переменные окружения (`backend/.env`)

| Переменная | По умолчанию | Описание |
|---|---|---|
| `OPENAI_API_KEY` | — | **Обязательно.** Ключ OpenAI |
| `JWT_SECRET` | — | **Обязательно.** Секрет подписи JWT, ≥ 32 символов |
| `OPENAI_MODEL` | `gpt-4o-mini` | Модель |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | Совместимый прокси, если нужен |
| `JWT_TTL_HOURS` | `72` | Время жизни токена |
| `DB_URL` | `jdbc:postgresql://localhost:5432/lekseradar` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `lekseradar` / `lekseradar` | Доступ к БД |
| `CORS_ORIGINS` | `http://localhost:5173` | Адрес фронтенда (через запятую, если несколько) |
| `PORT` | `8080` | Порт API |
