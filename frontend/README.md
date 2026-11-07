# LekseRadar — фронтенд

React 19 + Vite 8 + Tailwind 4 + React Router 7. Интерфейс на норвежском (bokmål).
Контракт с бэкендом — `docs/API.md`: адреса страниц и все URL `/api/v1` во фронтенде совпадают с ним (это проверяют тесты).

## Запуск

```bash
pnpm install
pnpm dev          # http://localhost:5173
```

Бэкенд должен работать на `http://localhost:8080` (как запустить — `docs/RUN.md`).
Vite проксирует `/api` и `/actuator` на `:8080`, поэтому CORS в dev настраивать не нужно.

Демо-вход: `demo@lekseradar.no` / `demo1234` — аккаунт создаёт `seed.sql` бэкенда. Без бэкенда дальше `/login` не пустит.

## Скрипты

| Команда | Что делает |
|---|---|
| `pnpm dev` | dev-сервер с proxy на бэкенд |
| `pnpm test` | тесты (vitest): контракт URL с API.md, клиент API, тема |
| `pnpm build` | проверка типов (`tsc`) + сборка в `dist/` |
| `pnpm typecheck` | только проверка типов |

CI (`.github/workflows/frontend.yml`) запускает `pnpm test` и `pnpm build` на каждый push/PR с изменениями во `frontend/`.

## Деплой (Vercel)

1. В Vercel задать переменную `VITE_API_URL` — полный адрес API, например `https://api.example.no/api/v1` (пример в `.env.example`).
2. На бэкенде добавить домен Vercel в `CORS_ORIGINS`. Сейчас разрешён только `http://localhost:5173`.

Без этих двух шагов сайт на Vercel откроется, но вход работать не будет.

## Где что лежит

| Файл | Что внутри |
|---|---|
| `src/paths.ts` | адреса страниц — как в API.md, меняются только здесь |
| `src/api/endpoints.ts` | все URL бэкенда (#1–#13 из API.md) |
| `src/api/client.ts` | fetch-обёртка: `/api/v1`, Bearer-токен, 401 → `/login` |
| `src/api/types.ts` | типы `User`, `Chat`, `Message`, `ApiError` и тела запросов |
| `src/auth.tsx` | вход/выход, токен, `RequireAuth` для 🔒-страниц |
| `src/forms.tsx` | поле формы с ошибкой, приведение ошибок к `ApiError` |
| `src/pages/` | страницы: `Account` (вход, профиль), `Chats` (ИИ-чат), `Dashboard`, `Learning` (карточки, квиз), `Other` |
| `src/theme.ts` | светлая / тёмная / авто-тема (тёмная вечером) |
| `COLORS.md` | палитра и почему она такая |

## Что работает на API, а что пока демо

| Страница | Статус |
|---|---|
| `/login`, `/register` | API |
| `/profile` — имя, язык, часовой пояс, пароль, выход | API |
| `/chat`, `/chat/:chatId` — ИИ-чат, архив, удаление | API |
| `/` — приветствие и последние чаты | API; задания и прогресс — демо |
| `/homework`, `/quiz`, `/flashcards`, `/progress`, `/history`, `/settings/platforms` | демо или «Kommer snart» — ждут эндпоинтов из `docs/ARCHITECTURE.md`, раздел 5 |

## Известные ограничения (нужен бэкенд)

- **Повтор после 502 дублирует вопрос.** API.md предлагает «Повторить» = тот же `POST /chats/{id}/messages`, а вопрос к этому моменту уже сохранён. Нужен эндпоинт повтора или идемпотентность на бэкенде.
- **«Выйти» только удаляет токен в браузере.** Отозвать JWT на сервере нельзя, он живёт 72 часа.
- **Язык из профиля (`locale`) влияет на ИИ-тьютора**, но интерфейс пока только на bokmål.
