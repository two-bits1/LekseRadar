# LekseRadar

Сайт для школьника: собирает домашние задания со всех школьных платформ в одном месте и помогает
их понять. ИИ-репетитор объясняет тему по шагам, но не делает домашку за ученика. ИИ-генератор
делает квизы по заданию, ошибки из квизов становятся флеш-картами, прогресс виден на дашборде.

Хакатон Oslo Sprint, 6–8 ноября 2026. Ветка `main-test-1.0` — тестовая сборка всех частей перед слиянием в `main`.

![Схема работы проекта](docs/project-schema.png)

## Функции

✅ работает в main-test-1.0 · 🟡 часть готова · 🕓 ещё не сделано

| # | Функция | Статус | Где |
|---|---|---|---|
| 1 | Сбор ДЗ с Canvas, Teams, itslearning, Visma InSchool | 🟡 сбор и демо-режим готовы, бэкенд ещё не вызывает | `homework-collector/` |
| 2 | Помощь с подключением платформ (токены, вход) | 🟡 функции входа готовы, страницы нет | `homework-collector/`, `/settings/platforms` |
| 3 | Вход и профиль | ✅ | `backend/`, `/login`, `/profile` |
| 4 | ИИ-репетитор по готовым промптам, выбор языка, материалы для обучения | 🟡 чат работает; промпты и материалы в базе, бэкенд пока берёт свой промпт | `backend/`, `ai-content/`, `/chat` |
| 5 | Квизы по ДЗ: ИИ делает JSON, квиз хранится и используется снова | 🟡 база готова, эндпоинтов нет | `database/`, `/quiz` |
| 6 | Флеш-карты (= вопросы квиза, повторение SM-2) | 🟡 база готова, эндпоинтов нет | `database/`, `/flashcards` |
| 7 | Прогресс, история, ачивки | 🟡 база готова, эндпоинтов нет | `database/`, `/progress` |
| 8 | Напоминания о дедлайнах | 🟡 таблица есть, отправки нет | `database/` |
| 9 | Дашборд | 🟡 страница на демо-данных | `frontend/`, `/` |

## Из чего состоит

| Папка | Кто | Что внутри | Подробнее |
|---|---|---|---|
| `frontend/` | №1 | React 19 + Vite + Tailwind, страницы на демо-данных, деплой на Vercel | — |
| `backend/` | №2 | Java 21, Spring Boot: вход по JWT, профиль, ИИ-чат (OpenAI по ключу из `.env`) | [backend/README.md](backend/README.md), [docs/API.md](docs/API.md) |
| `database/` | №3 | PostgreSQL 16: миграции V1–V15, демо-данные, функции квизов, карточек, прогресса | [database/README.md](database/README.md) |
| `homework-collector/` | №3 | Python: `collect()` собирает ДЗ с 4 платформ в один формат, демо-режим | [homework-collector/README.md](homework-collector/README.md) |
| `ai-content/` | №3 | промпты ИИ, учебные файлы, загрузка в базу, обзор исследований об обучении | [ai-content/prompts/README.md](ai-content/prompts/README.md) |
| `docs/` | все | архитектура, API, схемы, запуск | см. ниже |

## Быстрый старт

```bash
# 1. база (Docker): миграции + демо-ученик demo@lekseradar.no / demo1234
cd database && docker compose up -d && cd ..

# 2. промпты и учебные файлы в базу
pip install -r ai-content/load/requirements.txt
DATABASE_URL=postgresql://lekseradar:lekseradar@localhost:5432/lekseradar python ai-content/load/load.py

# 3. бэкенд: http://localhost:8080, Swagger /swagger-ui.html
cd backend && cp .env.example .env   # вписать OPENAI_API_KEY и JWT_SECRET
./mvnw spring-boot:run

# 4. фронтенд: http://localhost:5173
cd frontend && pnpm install && pnpm dev

# 5. сбор ДЗ в демо-режиме
cd homework-collector && pip install -r requirements.txt && python -m homework --demo
```

Подробно и с проверками: [docs/RUN.md](docs/RUN.md).

## Документация

| Файл | Для кого | Что там |
|---|---|---|
| [docs/API.md](docs/API.md) | фронт | все URL бэкенда, страницы фронта, типы, ошибки |
| [docs/api-map.png](docs/api-map.png) | фронт, бэк | схема: страница → URL → таблицы и функции базы |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | все | как всё работает: сценарии по шагам, какие API ещё нужны, почему так |
| [docs/project-schema.png](docs/project-schema.png) | все | общая схема проекта (вверху этой страницы) |
| [database/README.md](database/README.md) | бэк | таблицы, миграции и SQL для каждого сценария |
| [ai-content/prompts/README.md](ai-content/prompts/README.md) | бэк, №3 | промпты, выбор языка, файл ДЗ, как собрать запрос к ИИ |
| [ai-content/research/metody-obucheniya.md](ai-content/research/metody-obucheniya.md) | №3 | исследования об обучении, на которых построены промпты |
| [README-TEST.md](README-TEST.md) | все | что проверено в main-test-1.0 и 19 слабых мест |

![Страницы, API и база](docs/api-map.png)

## Проверка main-test-1.0 (7 ноября 2026)

База V1–V15 ✅ · Python-тесты 6/6 ✅ · фронтенд собирается ✅ · бэкенд-тесты 3/3 ✅ ·
бэкенд работает на общей базе (вход, профиль, чаты) ✅. Настоящий ответ ИИ не проверялся (нужен ключ).

Главное, что осталось до слияния в `main`:
1. Фронтенд подключить к бэкенду (вход, чат) — №1.
2. Бэкенд: промпты из базы (`chat_system_prompt`), эндпоинты ДЗ, квизов, карточек, дашборда — №2.
3. Обёртка для `collect()` — №3.

Полный список: [README-TEST.md](README-TEST.md).

## Команда

- **№1, Frontend:** React + Tailwind, все страницы, подключение к API бэкенда.
- **№2, Backend:** Spring Boot, вход и пользователи, ИИ-чат, эндпоинты для квизов, карточек, ДЗ и дашборда.
- **№3, Python + ИИ + база:** сбор ДЗ с платформ, PostgreSQL, промпты ИИ и учебные материалы.

<details>
<summary>Заметки с планирования (07.11)</summary>

фронт 
1. Lovable/v0 - claude - github
2. Централ баттон ИИ хелпер
3. Как сделать что б ИИ меняло сам текст в всех блоках(отдельную среду для ии? все решили. Пользователь на фронтенд тыкает на кнопку "разобрать тему"? Берется копируется полностью текст - отправляется в чат гпт - оттуда ДЖСОН и ДЖСОН создается под фреймворк на реакт
4. СОздать структуру сайта скелет
5. Нарисовать в пейнте как примерно хочу
   
бекенд 
1.Mysql / Postgress
2. Spring
3. Security maybe
4. AI integrating

апи 
1. взять токины ии
2. что бы был изначальный внедренный Промт (ии это преподаватель вуза)
3. настройка ии, под ректора и создание качественного промта
4. достать апи

</details>
