# Как запустить и проверить весь LekseRadar (ветка main-test-3.0)

## 1. База, промпты и тестовые данные (Docker)
```bash
cd database
docker compose --profile testdata up -d   # миграции V1–V20 → промпты из ai-content → тестовые данные
# без тестовых данных: docker compose up -d
# сброс: docker compose --profile testdata down -v && docker compose --profile testdata up -d
```
- Сервис `content` сам загружает промпты и учебные файлы (`ai-content/load/load.py`) и завершается.
- Сервис `testdata` создаёт ученика `test@lekseradar.no` / `LekseRadar2026` с 16 ДЗ по 10 предметам, 5 готовыми квизами
  и материалами: [database/testdata/README.md](../database/testdata/README.md).
- База открыта только на `127.0.0.1:5432`.

Без Docker: примените `database/migrations/*.sql` по порядку, затем
```bash
pip install -r ai-content/load/requirements.txt
DATABASE_URL=postgresql://lekseradar:lekseradar@localhost:5432/lekseradar python ai-content/load/load.py
psql "postgresql://lekseradar:lekseradar@localhost:5432/lekseradar" -v ON_ERROR_STOP=1 -f database/testdata/seed.sql
```

## 2. Бэкенд (Java 21)
```bash
cd backend
cp .env.example .env          # вписать OPENAI_API_KEY (Groq бесплатно) и JWT_SECRET
./mvnw spring-boot:run        # http://localhost:8080, Swagger: /swagger-ui.html
```
Проверка: `POST /api/v1/auth/login` с тестовым учеником → токен, `GET /api/v1/homework` → 16 заданий.
Без настоящего ключа всё, кроме ответов ИИ, работает: готовые квизы из тестовых данных ИИ не вызывают.

## 3. Фронтенд
```bash
cd frontend
pnpm install
pnpm dev                      # http://localhost:5173
```
Все страницы на API бэкенда через proxy Vite (`/api` → `localhost:8080`). Тесты: `pnpm test`.

## 4. Сбор ДЗ (Python)
```bash
cd homework-collector
pip install -r requirements.txt
python -m homework --creds creds.json --json   # настоящие платформы; формат creds — в homework-collector/README.md
python -m pytest -q
```

## Что проверено

См. [README-TEST.md](../README-TEST.md).
