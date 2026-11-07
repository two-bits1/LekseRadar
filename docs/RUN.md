# Как запустить и проверить весь LekseRadar (ветка main-test-2.0)

В main-test-2.0: `main-test-1.0` + свежие `JavaAPI-setup` и `Frontend-setup`.

## 1. База (PostgreSQL 16, Docker)
```bash
cd database
docker compose up -d          # миграции V1–V14 + демо-данные
# сброс: docker compose down -v && docker compose up -d
```
Демо-ученик: `demo@lekseradar.no` / `demo1234`.

## 2. Промпты и учебные файлы в базу
```bash
pip install -r ai-content/load/requirements.txt
DATABASE_URL=postgresql://lekseradar:lekseradar@localhost:5432/lekseradar python ai-content/load/load.py
```
Без этого шага чаты отвечают ошибкой «нет промпта» (так и задумано).

## 3. Бэкенд (Java 21)
```bash
cd backend
cp .env.example .env          # вписать OPENAI_API_KEY и JWT_SECRET
./mvnw spring-boot:run        # http://localhost:8080, Swagger: /swagger-ui.html
```
Проверка: `POST /api/v1/auth/login` с демо-учеником → токен, `GET /api/v1/chats` → демо-чат.

## 4. Фронтенд
```bash
cd frontend
pnpm install
pnpm dev                      # http://localhost:5173
```
Вход, профиль и чат идут на бэкенд через proxy Vite (`/api` → `localhost:8080`), остальные страницы пока демо.
Тесты: `pnpm test`.

## 5. Сбор ДЗ (Python)
```bash
cd homework-collector
pip install -r requirements.txt
python -m homework --demo     # демо-ДЗ со всех 4 платформ
python -m pytest -q
```

## Что проверено

См. [README-TEST.md](../README-TEST.md).
