# Деплой на Railway: база, collector и бэкенд

> **Статус (7 ноября):** Railway собран, но пока не работает целиком — см. [RAILWAY-STATUS.md](RAILWAY-STATUS.md).

Один проект Railway, три сервиса: **Postgres**, **collector** (без публичного адреса) и **backend** (№2).
Фронт отдельно на Vercel ([PLAN-FRONTEND.md](https://github.com/two-bits1/LekseRadar/blob/main-test-3.0/docs/PLAN-FRONTEND.md), шаг 4). Время: около часа.

```
Vercel (фронт) ──https──► backend ──private──► collector ──https──► Canvas / Teams / itslearning / InSchool
                             │
                             └──private──► Postgres
```

Секреты (пароль базы, `COLLECTOR_KEY`, `CREDENTIALS_KEY`, ключ ИИ) вводятся только в Railway → Variables.
Не в git, не в чат.

## 1. Проект и база (№3, 15 мин)

1. https://railway.com → New Project → **Deploy PostgreSQL**. Сервис назовите `Postgres`.
2. Postgres → **Variables**: скопируйте `DATABASE_PUBLIC_URL` (адрес через TCP-прокси, для вашего компьютера).
3. С компьютера, из корня репозитория (`main-test-3.0`):

```bash
export DATABASE_URL='<DATABASE_PUBLIC_URL>'
pip install -r ai-content/load/requirements.txt              # psycopg и др.; psql и Docker не нужны
python database/migrate.py "$DATABASE_URL"                   # V1–V22, учёт в schema_migrations
python ai-content/load/load.py                               # промпты и учебные материалы
python database/migrate.py "$DATABASE_URL" --status          # все ✓
```

**Не** загружайте `database/testdata/seed.sql`: на настоящем сайте тестового ученика быть не должно.

Новые миграции потом докатываются той же командой `python database/migrate.py "$DATABASE_URL"`.

## 2. collector (№3, 15 мин)

1. В проекте **+ Create → GitHub Repo** → `two-bits1/LekseRadar`.
2. Сервис → **Settings**:
   - Name: `collector` (от имени зависит внутренний адрес `collector.railway.internal`);
   - Source → Branch: `main-test-3.0` (после переноса — `PythonAPI`);
   - Source → **Root Directory: `homework-collector`** (там `Dockerfile` и `railway.json`);
   - Networking → **не нажимайте Generate Domain**: публичного адреса у collector быть не должно.
3. **Variables**:

| Переменная | Значение |
|---|---|
| `COLLECTOR_KEY` | `openssl rand -hex 32` у себя, вставить сюда |
| `PORT` | `8090` |
| `HOST` | `::` (приватная сеть Railway работает по IPv6) |
| `TEAMS_CLIENT_ID` | id приложения Entra, если подключаете Teams ([REAL-DATA.md](REAL-DATA.md#microsoft-teams-официальный-api-graph)) |

4. Deploy. В **Deployments → Logs**: `Uvicorn running on http://[::]:8090`, health-check `/health` зелёный.

## 3. Бэкенд (№2, 20 мин)

1. **+ Create → GitHub Repo** → тот же репозиторий, Name `backend`, Root Directory `backend`, ветка как выше.
   Сборка Java 21 через Railpack/Nixpacks по `pom.xml`. Если не взлетит: Start Command
   `java -jar target/lekseradar-backend-0.1.0.jar`, Build Command `./mvnw -q package -DskipTests`.
2. **Variables** (ссылки `${{…}}` Railway подставит сам):

| Переменная | Значение |
|---|---|
| `DB_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DB_USER` | `${{Postgres.PGUSER}}` |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `COLLECTOR_URL` | `http://${{collector.RAILWAY_PRIVATE_DOMAIN}}:8090` |
| `COLLECTOR_KEY` | `${{collector.COLLECTOR_KEY}}` |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `CREDENTIALS_KEY` | `openssl rand -base64 32`. **Сохраните в надёжном месте**: потеряете — все подключения платформ придётся делать заново |
| `OPENAI_BASE_URL` | `https://api.groq.com/openai/v1` |
| `OPENAI_API_KEY` | ключ Groq |
| `OPENAI_MODEL` | `openai/gpt-oss-120b` |
| `CORS_ORIGINS` | адрес фронта на Vercel, например `https://lekseradar.vercel.app` |

`PGHOST` у Postgres — внутренний адрес (`postgres.railway.internal`), так бэкенд ходит в базу без интернета.

3. Networking → **Generate Domain**: это адрес API для фронта (`VITE_API_URL=https://<домен>/api/v1`).

## 4. Проверка

```bash
API=https://<домен бэкенда>
curl $API/actuator/health                                   # {"status":"UP"}
# collector снаружи недоступен: у него нет домена. Изнутри (Railway → backend → Shell, если включено):
# curl http://collector.railway.internal:8090/health        # {"ok":true}
```

Затем с сайта на Vercel: регистрация → подключение Canvas → ДЗ на «Lekser». ИИ:

```bash
python ai-content/check/check_ai.py --api $API/api/v1 --email <ученик> --password <пароль> \
  --database-url "$DATABASE_URL"
```

## Если что-то не так

| Симптом | Причина |
|---|---|
| collector падает с `COLLECTOR_KEY не задан` | переменная не задана или короче 16 символов |
| бэкенд: `Connection refused` к collector | у collector нет `HOST=::` или имя сервиса не `collector` |
| бэкенд не стартует: `Schema-validation: missing …` | в базе не все миграции: `python database/migrate.py "$DATABASE_URL" --status` |
| чат: «промпт не загружен» | не запущен `load.py` на облачной базе |
| фронт: ошибка CORS | `CORS_ORIGINS` не совпадает с адресом Vercel (без `/` в конце) |
