# LekseRadar

Сайт для школьника: собирает домашние задания со всех школьных платформ в одном месте и помогает
их понять. ИИ-репетитор объясняет тему по шагам, но не делает домашку за ученика. ИИ-генератор
делает квизы по заданию, ошибки из квизов становятся флеш-картами, прогресс виден на дашборде.

Хакатон Oslo Sprint, 6–8 ноября 2026. Ветка `main-test-3.0` — тестовая сборка всех частей перед слиянием в `main`.

![Схема работы проекта](docs/project-schema.png)

## Функции

✅ работает в main-test-3.0 · 🟡 часть готова · 🕓 ещё не сделано

| # | Функция | Статус | Где |
|---|---|---|---|
| 1 | Сбор ДЗ с Canvas, Teams, itslearning, Visma InSchool | 🟡 `collect()` готов, бэкенд его ещё не вызывает; для демо — тестовые данные | `homework-collector/`, `database/testdata/` |
| 2 | Помощь с подключением платформ (токены, вход) | 🕓 функции входа в Python готовы, API и страницы нет | `homework-collector/`, `/settings/platforms` |
| 3 | Вход и профиль | ✅ JWT, защита от перебора пароля | `backend/`, `/login`, `/profile` |
| 4 | Домашка: список, срок, «сдано», помощь по заданию | ✅ | `/homework` |
| 5 | ИИ-репетитор: промпты из базы, язык сайта, текст ДЗ и материалы в промпте | ✅ | `backend/`, `ai-content/`, `/chat` |
| 6 | Квизы: ИИ делает JSON, квиз хранится и используется снова | ✅ | `/quiz` |
| 7 | Флеш-карты (= вопросы квиза, ошибки сами попадают в колоду, SM-2) | ✅ | `/flashcards` |
| 8 | Прогресс и история | ✅ | `/progress`, `/history` |
| 9 | Дашборд | ✅ ДЗ, квизы, карточки, чаты | `/` |
| 10 | Напоминания о дедлайнах, ачивки | 🟡 таблицы есть, отправки и API нет | `database/` |

## Из чего состоит

| Папка | Кто | Что внутри | Подробнее |
|---|---|---|---|
| `frontend/` | №1 | React 19 + Vite + Tailwind, все страницы на API, nb/en/ru, 93 теста, CI, деплой на Vercel | [frontend/README.md](frontend/README.md) |
| `backend/` | №2 | Java 21, Spring Boot: JWT, сущности всех 20 таблиц, чат, квизы, карточки, ДЗ, дашборд, лимиты | [backend/README.md](backend/README.md), [docs/API.md](docs/API.md) |
| `database/` | №3 | PostgreSQL 16: миграции V1–V20, функции квизов, карточек, прогресса; тестовые данные | [database/README.md](database/README.md), [ENTITIES.md](database/ENTITIES.md) |
| `homework-collector/` | №3 | Python: `collect()` собирает ДЗ с 4 платформ в один формат | [homework-collector/README.md](homework-collector/README.md) |
| `ai-content/` | №3 | промпты ИИ, учебные файлы, загрузка в базу, исследования об обучении | [ai-content/prompts/README.md](ai-content/prompts/README.md) |
| `docs/` | все | архитектура, API, схемы, запуск | см. ниже |

## Быстрый старт

```bash
# 1. база + промпты + тестовый ученик test@lekseradar.no / LekseRadar2026 (16 ДЗ по 10 предметам)
cd database && docker compose --profile testdata up -d && cd ..

# 2. бэкенд: http://localhost:8080, Swagger /swagger-ui.html
cd backend && cp .env.example .env   # вписать OPENAI_API_KEY (Groq бесплатно) и JWT_SECRET
./mvnw spring-boot:run

# 3. фронтенд: http://localhost:5173
cd frontend && pnpm install && pnpm dev
```

Подробно и с проверками: [docs/RUN.md](docs/RUN.md).

## Документация

| Файл | Для кого | Что там |
|---|---|---|
| [docs/API.md](docs/API.md) | фронт | все URL бэкенда, страницы фронта, типы, ошибки, лимиты |
| [docs/URLS.md](docs/URLS.md) | все | все адреса сайта и API одним списком |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | все | как всё работает: сценарии по шагам, почему так |
| [database/ENTITIES.md](database/ENTITIES.md) | бэк | все таблицы, enum, виды и функции и как они лежат в JPA-сущностях и DTO |
| [database/README.md](database/README.md) | бэк | миграции и SQL для каждого сценария |
| [database/testdata/README.md](database/testdata/README.md) | все | тестовый ученик и его данные |
| [ai-content/prompts/README.md](ai-content/prompts/README.md) | бэк, №3 | промпты, язык, файл ДЗ, как собрать запрос к ИИ |
| [README-TEST.md](README-TEST.md) | все | что проверено в main-test-3.0, что исправлено и что осталось |

![Страницы, API и база](docs/api-map.png)

## Проверка main-test-3.0 (7 ноября 2026)

База V1–V20 ✅ · Python 6/6 ✅ · фронтенд 93/93 + сборка ✅ · бэкенд 14/14 + интеграционный 5/5 ✅ ·
`docker compose --profile testdata` ✅ · **в браузере: вход тестовым учеником → ДЗ → чат по заданию → квиз → карточки → прогресс** ✅.
Ответы ИИ проверены на локальной заглушке, с настоящим ключом Groq не проверялись.

Что осталось и кто делает: [README-TEST.md](README-TEST.md).

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
