# LekseRadar API — для фронтенда

Базовый адрес: **`http://localhost:8080/api/v1`**
Живая документация (можно вызывать запросы прямо в браузере): **http://localhost:8080/swagger-ui.html**

Демо-вход: `demo@lekseradar.no` / `demo1234`

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
