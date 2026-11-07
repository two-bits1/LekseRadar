# LekseRadar API — для фронтенда

Базовый адрес: **`http://localhost:8080/api/v1`**
Живая документация (можно вызывать запросы прямо в браузере): **http://localhost:8080/swagger-ui.html**

Демо-вход: `demo@lekseradar.no` / `demo1234`

---

## Все URL бэкенда

🔓 — без токена, 🔒 — нужен `Authorization: Bearer <token>`

| # | Метод | URL | Что делает | Ответ |
|---|---|---|---|---|
| 1 | POST | 🔓 `/api/v1/auth/register` | регистрация | 201 `AuthResponse` |
| 2 | POST | 🔓 `/api/v1/auth/login` | вход | 200 `AuthResponse` |
| 3 | GET | 🔒 `/api/v1/users/me` | мой профиль | 200 `User` |
| 4 | PATCH | 🔒 `/api/v1/users/me` | изменить имя / язык / часовой пояс | 200 `User` |
| 5 | PUT | 🔒 `/api/v1/users/me/password` | сменить пароль | 204 |
| 6 | GET | 🔒 `/api/v1/chats?archived=` | список чатов | 200 `Chat[]` |
| 7 | POST | 🔒 `/api/v1/chats` | создать чат | 201 `Chat` |
| 8 | GET | 🔒 `/api/v1/chats/{chatId}` | один чат | 200 `Chat` |
| 9 | PATCH | 🔒 `/api/v1/chats/{chatId}` | переименовать / тема / архив | 200 `Chat` |
| 10 | DELETE | 🔒 `/api/v1/chats/{chatId}` | удалить чат | 204 |
| 11 | GET | 🔒 `/api/v1/chats/{chatId}/messages` | история сообщений | 200 `Message[]` |
| 12 | POST | 🔒 `/api/v1/chats/{chatId}/messages` | вопрос ИИ-тьютору | 201 `ChatReply` |
| 13 | GET | 🔓 `/actuator/health` | сервер и база живы? | 200 `{"status":"UP"}` |
| 14 | GET | 🔓 `/swagger-ui.html` | интерактивная документация | страница |
| 15 | GET | 🔓 `/v3/api-docs` | OpenAPI-схема (для генерации клиента) | JSON |

Подробности по каждому URL — ниже.

---

## Страницы фронтенда (React Router)

Это адреса в браузере (`http://localhost:5173/...`), их делает фронтенд. Бэкенд отдаёт только JSON по `/api/v1/...`.
✅ — API уже готов, 🕓 — API будет позже (квизы, ДЗ и т.д. по `docs/ARCHITECTURE.md`).

| Страница | Путь | Доступ | Что на странице | Какие API вызывает |
|---|---|---|---|---|
| Главная / дашборд | `/` | 🔒 | приветствие, последние чаты, (позже) ДЗ и прогресс | ✅ `GET /users/me`, ✅ `GET /chats` · 🕓 `GET /dashboard` |
| Вход | `/login` | 🔓 | email + пароль | ✅ `POST /auth/login` |
| Регистрация | `/register` | 🔓 | email, пароль, имя | ✅ `POST /auth/register` |
| Профиль | `/profile` | 🔒 | имя, язык, часовой пояс, смена пароля, выход | ✅ `GET/PATCH /users/me`, ✅ `PUT /users/me/password` |
| Список чатов | `/chat` | 🔒 | чаты, кнопка «Новый чат», вкладка «Архив» | ✅ `GET /chats?archived=false/true`, ✅ `POST /chats` |
| Чат | `/chat/:chatId` | 🔒 | история, поле ввода, «Тьютор печатает…», переименовать / в архив / удалить | ✅ `GET /chats/{id}`, ✅ `GET/POST /chats/{id}/messages`, ✅ `PATCH/DELETE /chats/{id}` |
| Домашка | `/homework` | 🔒 | ДЗ со всех платформ, кнопка «Обновить» | 🕓 `GET /homework`, 🕓 `POST /homework/sync` |
| Квиз: выбор | `/quiz` | 🔒 | предмет, тема, уровень | 🕓 `GET /subjects`, 🕓 `POST /quizzes` |
| Квиз: прохождение | `/quiz/:quizId` | 🔒 | вопрос, ответ, пояснение, итог | 🕓 `GET /quizzes/{id}`, 🕓 `POST /quizzes/{id}/answers`, 🕓 `POST /quizzes/{id}/finish` |
| Флеш-карты | `/flashcards` | 🔒 | карточки к повторению, оценка 0–5 | 🕓 `GET /flashcards/due`, 🕓 `POST /flashcards/{id}/review` |
| Прогресс | `/progress` | 🔒 | графики по предметам и дням | 🕓 `GET /dashboard` |
| История | `/history` | 🔒 | лента «что я делал» | 🕓 `GET /history` |
| Платформы | `/settings/platforms` | 🔒 | подключить Canvas / Teams / itslearning / InSchool | 🕓 `GET /platforms`, 🕓 `POST/DELETE /platforms/{platform}` |
| Не найдено | `*` | 🔓 | «Страница не найдена» | — |

**Правила для страниц 🔒:** нет токена → редирект на `/login`. Любой ответ `401` → удалить токен и тоже на `/login`.
После входа или регистрации → на `/`. «Выйти» = удалить токен из `localStorage` (отдельного API для выхода нет).

```tsx
// пример маршрутов (react-router-dom)
<Routes>
  <Route path="/login" element={<Login />} />
  <Route path="/register" element={<Register />} />
  <Route element={<RequireAuth />}>          {/* нет токена → <Navigate to="/login" /> */}
    <Route path="/" element={<Dashboard />} />
    <Route path="/profile" element={<Profile />} />
    <Route path="/chat" element={<ChatList />} />
    <Route path="/chat/:chatId" element={<Chat />} />
  </Route>
  <Route path="*" element={<NotFound />} />
</Routes>
```

---

## Как работает вход

1. `POST /auth/login` (или `/auth/register`) → в ответе `token`.
2. Сохраните токен (например, в `localStorage`).
3. Во **все** остальные запросы добавляйте заголовок:
   ```
   Authorization: Bearer <token>
   ```
4. Ответ `401` → токен истёк или неверный: отправьте пользователя на экран входа. Токен живёт 72 часа.

```ts
// пример обёртки
const API = "/api/v1"; // в dev через proxy Vite, см. в конце

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem("token");
  const res = await fetch(API + path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  if (res.status === 401) { localStorage.removeItem("token"); location.href = "/login"; }
  if (!res.ok) throw await res.json();          // ApiError, см. «Ошибки»
  return res.status === 204 ? (undefined as T) : res.json();
}
```

---

## Типы

```ts
type User = {
  id: number;
  email: string;
  displayName: string;
  locale: "nb" | "nn" | "en" | "ru";   // язык интерфейса и ИИ-тьютора
  timezone: string;                    // "Europe/Oslo"
  createdAt: string;                   // ISO-8601, UTC: "2026-11-07T09:40:00Z"
  lastLoginAt: string | null;
};

type AuthResponse = { token: string; user: User };

type Chat = {
  id: number;
  title: string;          // "Ny samtale", пока нет первого вопроса
  topic: string | null;
  archived: boolean;
  createdAt: string;
  updatedAt: string;      // время последнего сообщения
};

type Message = {
  id: number;
  role: "student" | "tutor";
  content: string;        // у tutor — Markdown (рендерить, например, react-markdown)
  model: string | null;   // какая модель ответила (только у tutor)
  createdAt: string;
};

type ChatReply = { chat: Chat; userMessage: Message; assistantMessage: Message };

type ApiError = {
  status: number;
  message: string;                        // готовый текст для показа пользователю
  fields: Record<string, string> | null;  // ошибки по полям формы (только 400)
  timestamp: string;
};
```

---

## Авторизация (без токена)

### `POST /auth/register` — регистрация
```json
{ "email": "ann@example.com", "password": "secret123", "displayName": "Ann" }
```
- `password` — от 8 до 100 символов
- **201** → `AuthResponse`
- **400** → неверный email / короткий пароль (`fields`)
- **409** → email уже занят

### `POST /auth/login` — вход
```json
{ "email": "demo@lekseradar.no", "password": "demo1234" }
```
- **200** → `AuthResponse`
- **401** → неверный email или пароль

---

## Профиль

### `GET /users/me` — мой профиль
- **200** → `User`

### `PATCH /users/me` — изменить профиль
Все поля необязательны, меняются только переданные.
```json
{ "displayName": "Ann", "locale": "en", "timezone": "Europe/Oslo" }
```
- **200** → `User`
- **400** → неизвестный `locale` или `timezone`

### `PUT /users/me/password` — сменить пароль
```json
{ "currentPassword": "secret123", "newPassword": "newSecret123" }
```
- **204** — без тела
- **400** → текущий пароль неверный / новый короче 8 символов

---

## ИИ-чат

### `GET /chats` — список чатов
Новые сверху. Параметр `archived`: `true` — только архив, `false` — только активные, не указан — все.
```
GET /chats?archived=false
```
- **200** → `Chat[]`

### `POST /chats` — новый чат
Тело необязательно.
```json
{ "title": "Algebra", "topic": "lineære funksjoner" }
```
- **201** → `Chat`
- Без `title` чат назовётся по первому вопросу.

### `GET /chats/{chatId}` — один чат
- **200** → `Chat`
- **404** → нет такого чата (или он чужой)

### `PATCH /chats/{chatId}` — переименовать / тема / архив
Все поля необязательны. `topic: ""` убирает тему.
```json
{ "title": "Новое имя", "topic": "геометрия", "archived": true }
```
- **200** → `Chat`

### `DELETE /chats/{chatId}` — удалить чат со всеми сообщениями
- **204** — без тела

### `GET /chats/{chatId}/messages` — история
Старые сверху.
- **200** → `Message[]`

### `POST /chats/{chatId}/messages` — задать вопрос ИИ
```json
{ "content": "Hva betyr stigningstallet i y = 2x + 3?" }
```
- `content` — от 1 до 8000 символов
- **201** → `ChatReply` (вопрос + ответ тьютора + обновлённый чат)
- **502** → ИИ недоступен. Вопрос уже сохранён в истории, можно показать кнопку «Повторить».
- Ответ идёт 2–15 секунд: покажите индикатор «Тьютор печатает…».

---

## Служебное

### `GET http://localhost:8080/actuator/health` (без `/api/v1`, без токена)
- `{"status":"UP"}` — сервер и база работают
- `{"status":"DOWN"}` — нет связи с базой

---

## Ошибки

Все ошибки приходят в одном формате `ApiError`:
```json
{
  "status": 400,
  "message": "Проверьте введённые данные",
  "fields": { "email": "Некорректный email", "password": "Пароль должен быть от 8 до 100 символов" },
  "timestamp": "2026-11-07T09:40:00Z"
}
```

| Код | Что значит | Что делать на фронте |
|---|---|---|
| 400 | неверные данные | показать `fields` под полями формы, иначе `message` |
| 401 | нет токена / истёк / неверный логин | на экран входа |
| 404 | не найдено (или чужое) | «Не найдено» |
| 409 | email уже занят | показать `message` у поля email |
| 502 | ИИ недоступен | показать `message` и кнопку «Повторить» |
| 500 | ошибка сервера | «Что-то пошло не так» |

---

## Подключение в Vite (dev)

`vite.config.ts` — запросы `/api/...` уходят на бэкенд, CORS настраивать не нужно:
```ts
server: { proxy: { "/api": "http://localhost:8080" } }
```
Без proxy бэкенд разрешает запросы только с `http://localhost:5173`.
