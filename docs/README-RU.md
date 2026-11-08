# LekseRadar (русская версия)

> English version: [README.md](../README.md).

Сайт, который собирает домашние задания ученика с платформ школы (Canvas, Teams, itslearning, Visma InSchool,
Google Classroom) в одном месте и помогает их сделать: ИИ-тьютор в чате, квизы, флеш-карты, дашборд и прогресс.

Ветка `main-test-4.0` — финальная сборка из трёх PR: `PythonAPI` (#9), `JavaAPI-setup` (#2), `Frontend-setup` (#10).
Тестовых данных нет: ученик создаётся регистрацией. Результаты проверки: [README-TEST.md](../README-TEST.md),
что осталось: [TODO.md](../TODO.md), все правки после проверки командой: [CHANGES.md](../CHANGES.md).

```
браузер ──► frontend (React + Vite) ──/api/v1──► backend (Spring Boot, Java 21) ──► PostgreSQL 16
                                                   │                                  ▲
                                                   ├──► collector (FastAPI) ──► Canvas / Teams / itslearning / InSchool / Classroom
                                                   └──► ИИ (Groq, OpenAI-совместимый API)
```

| Папка | Кто | Что |
|---|---|---|
| `frontend/` | №1 | сайт: вход с кодом из письма, ДЗ, платформы, тьютор, квизы, карточки, прогресс |
| `backend/` | №2 | API `/api/v1`, вход (JWT), шифрование доступов платформ, синхронизация ДЗ, вызов ИИ |
| `database/` | №3 | миграции V1–V26, `migrate.py` (Docker Compose — в корне) |
| `homework-collector/` | №3 | сбор ДЗ с платформ, HTTP-сервис для бэкенда ([COLLECTOR-API.md](../homework-collector/COLLECTOR-API.md)) |
| `ai-content/` | №3 | промпты ИИ, учебные материалы, `load.py`, проверка ИИ `check_ai.py` |
| `docs/` | все | API, архитектура, деплой, планы |

## Запуск на своём компьютере

Нужен только Docker.

```bash
cp .env.example .env          # once; add OPENAI_API_KEY for the AI tutor
docker compose up --build     # site http://localhost:5173, API http://localhost:8080
```

See [README.md](../README.md#getting-started) for details.

Подробнее: [backend/README.md](../backend/README.md), [frontend/README.md](../frontend/README.md),
[database/README.md](../database/README.md), [homework-collector/README.md](../homework-collector/README.md).

## Деплой

| Что | Где | Инструкция |
|---|---|---|
| Фронт | Vercel (`frontend/vercel.json`) | `VITE_API_URL=https://<бэкенд>/api/v1`, адрес Vercel в `CORS_ORIGINS` бэкенда |

## Документы

- [docs/API.md](../docs/API.md) — все эндпоинты; [docs/URLS.md](../docs/URLS.md) — адреса и страницы.
- [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md), [database/ENTITIES.md](../database/ENTITIES.md) — устройство и таблицы.
- [docs/REAL-DATA.md](../docs/REAL-DATA.md) — как подключить настоящие платформы.
