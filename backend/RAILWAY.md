# Бэкенд на Railway: что сделать (№2)

База и `collector` уже работают в проекте Railway **distinguished-radiance**: Postgres (миграции V1–V21 и промпты
загружены) и `collector` (без публичного адреса). Осталось добавить сервис `backend`. Время: 20–30 минут.
Подробная общая инструкция: [docs/DEPLOY-RAILWAY.md](../docs/DEPLOY-RAILWAY.md), раздел 3.

```
Vercel (фронт) ──https──► backend ──private──► collector
                             └──────private──► Postgres
```

Секреты (`JWT_SECRET`, `CREDENTIALS_KEY`, ключ Groq) вводятся только в Railway → Variables. Не в git и не в чат.

## 1. Создать сервис

1. Попросите Илью добавить вас в проект Railway (Project Settings → Members) или делайте это вместе.
2. На схеме проекта: **+ Create → GitHub Repo → two-bits1/LekseRadar**.
3. Сервис → **Settings**:
   - Name: `backend`;
   - Source → Branch: `main-test-3.0` (после переноса кода — `JavaAPI-setup`);
   - Source → Root Directory: `backend`.

Сборка идёт по `backend/Dockerfile` (Maven + Java 21 → `eclipse-temurin:21-jre`), health-check `/actuator/health`
из `backend/railway.json`. Первая сборка 3–5 минут.

## 2. Variables

Raw Editor, вставить целиком. Строки `${{…}}` Railway подставит сам, менять их не нужно:

```
DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DB_USER=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
COLLECTOR_URL=http://${{collector.RAILWAY_PRIVATE_DOMAIN}}:8090
COLLECTOR_KEY=${{collector.COLLECTOR_KEY}}
OPENAI_BASE_URL=https://api.groq.com/openai/v1
OPENAI_MODEL=openai/gpt-oss-120b
CORS_ORIGINS=http://localhost:5173
```

Добавить вручную:

| Переменная | Значение |
|---|---|
| `JWT_SECRET` | `openssl rand -base64 48` у себя в терминале |
| `OPENAI_API_KEY` | ключ Groq (у Ильи) |
| `CREDENTIALS_KEY` | `openssl rand -base64 32`, когда будет шаг 1 из [PLAN-BACKEND.md](../docs/PLAN-BACKEND.md). **Сохраните в надёжном месте**: потеряете — все подключения платформ придётся делать заново |

`PORT` задавать не нужно: Railway передаёт его сам, бэкенд читает `server.port: ${PORT:8080}`.

## 3. Публичный адрес

Settings → Networking → **Generate Domain**. Это адрес API:

- фронту на Vercel: `VITE_API_URL=https://<домен>/api/v1`;
- когда фронт задеплоен, `CORS_ORIGINS=https://<адрес-на-vercel>` (без `/` в конце, несколько адресов через запятую).

## 4. Проверка

```bash
API=https://<домен>
curl $API/actuator/health          # {"status":"UP"}
open $API/swagger-ui/index.html    # Swagger
```

Затем регистрация и вход через Swagger или фронт. Проверка ИИ (делает Илья):
`python ai-content/check/check_ai.py --api $API/api/v1 --email … --password …`.

## Что ещё нужно в коде для настоящих данных

По [PLAN-BACKEND.md](../docs/PLAN-BACKEND.md): шифрование `CREDENTIALS_KEY`, `CollectorClient`
(контракт [homework-collector/COLLECTOR-API.md](../homework-collector/COLLECTOR-API.md)), API `/platforms`,
`POST /homework/sync`. После каждого пуша в ветку Railway пересобирает бэкенд сам.

Ошибки платформ отдавайте как **422**, не 401: фронт на любой 401 выкидывает ученика на вход.

## Если что-то не так

| Симптом | Что делать |
|---|---|
| сборка падает на `mvn package` | открыть Build Logs; локально `./mvnw -q package -DskipTests` должно проходить |
| `Schema-validation: missing …` | в базе не хватает миграции: Илья запускает `python3 database/setup_cloud_db.py` |
| `Connection refused` к базе | `DB_URL` вставлен не как ссылка `${{Postgres.…}}`, или сервис базы назван не `Postgres` |
| `Connection refused` к collector | сервис называется не `collector` или у него нет `HOST=::` |
| health-check не проходит, `JWT_SECRET` в логах | переменная не задана |
| фронт: ошибка CORS | `CORS_ORIGINS` не совпадает с адресом Vercel |
| чат: «промпт не загружен» | Илья заново запускает `setup_cloud_db.py` (он же грузит промпты) |
