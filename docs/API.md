# LekseRadar API — для фронтенда

Базовый адрес: **`http://localhost:8080/api/v1`**
Живая документация (можно вызывать запросы прямо в браузере): **http://localhost:8080/swagger-ui.html**

Пробных пользователей нет: аккаунт создаётся через `POST /auth/register`, вход через `POST /auth/login`.

---

## Все URL бэкенда

🔓 — без токена, 🔒 — нужен `Authorization: Bearer <token>`

| # | Метод | URL | Что делает | Ответ |
|---|---|---|---|---|
| 1 | POST | 🔓 `/api/v1/auth/register` | регистрация | 201 `AuthResponse` |
| 2 | POST | 🔓 `/api/v1/auth/login` | вход | 200 `AuthResponse` |
| 3 | GET | 🔒 `/api/v1/users/me` | мой профиль | 200 `User` |
| 4 | PATCH | 🔒 `/api/v1/users/me` | изменить имя / язык / часовой пояс / напоминания на почту (`emailReminders`) | 200 `User` |
| 5 | PUT | 🔒 `/api/v1/users/me/password` | сменить пароль | 204 |
| 45 | POST | 🔒 `/api/v1/users/me/reminders/test` | пробное письмо-напоминание о ДЗ на почту из профиля | 200 `{email, count}`, 429, 503 |
| 6 | GET | 🔒 `/api/v1/chats?archived=` | список чатов | 200 `Chat[]` |
| 7 | POST | 🔒 `/api/v1/chats` | создать чат | 201 `Chat` |
| 8 | GET | 🔒 `/api/v1/chats/{chatId}` | один чат | 200 `Chat` |
| 9 | PATCH | 🔒 `/api/v1/chats/{chatId}` | переименовать / тема / архив | 200 `Chat` |
| 10 | DELETE | 🔒 `/api/v1/chats/{chatId}` | удалить чат | 204 |
| 11 | GET | 🔒 `/api/v1/chats/{chatId}/messages` | история сообщений | 200 `Message[]` |
| 12 | POST | 🔒 `/api/v1/chats/{chatId}/messages` | сообщение ИИ (тьютор отвечает, квиз-чат составляет квиз) | 201 `ChatReply` |
| 13 | GET | 🔒 `/api/v1/subjects` | все предметы | 200 `Subject[]` |
| 14 | GET | 🔒 `/api/v1/quizzes` | мои квизы | 200 `QuizSummary[]` |
| 15 | GET | 🔒 `/api/v1/quizzes/progress` | прогресс по предметам и темам | 200 `QuizProgress[]` |
| 16 | POST | 🔒 `/api/v1/quizzes` | начать квиз (готовый или новый от ИИ) | 201 `Quiz` |
| 17 | GET | 🔒 `/api/v1/quizzes/{quizId}` | квиз с вопросами | 200 `Quiz` |
| 18 | POST | 🔒 `/api/v1/quizzes/{quizId}/answers` | ответ на вопрос | 200 `AnswerResult` |
| 19 | POST | 🔒 `/api/v1/quizzes/{quizId}/finish` | завершить квиз | 200 `{score}` |
| 20 | POST | 🔒 `/api/v1/quiz-sets/{setId}/start` | начать квиз по набору (из квиз-чата) | 201 `Quiz` |
| 21 | POST | 🔒 `/api/v1/quiz-sets/{setId}/flashcards` | весь квиз в колоду | 200 `Deck` |
| 22 | GET | 🔒 `/api/v1/flashcards?due=` | колода | 200 `Flashcard[]` |
| 23 | GET | 🔒 `/api/v1/flashcards/due` | карточки к повторению | 200 `Flashcard[]` |
| 24 | GET | 🔒 `/api/v1/flashcards/stats` | сводка по колоде | 200 `FlashcardStats` |
| 25 | POST | 🔒 `/api/v1/flashcards/{exerciseId}/review` | оценка карточки 0–5 | 200 `ReviewResult` |
| 26 | POST | 🔒 `/api/v1/flashcards/generate` | карточки от ИИ по теме | 201 `Deck` |
| 27 | GET | 🔓 `/actuator/health` | сервер и база живы? | 200 `{"status":"UP"}` |
| 28 | GET | 🔓 `/swagger-ui.html` | интерактивная документация | страница |
| 29 | GET | 🔓 `/v3/api-docs` | OpenAPI-схема (для генерации клиента) | JSON |

Подробности по каждому URL — ниже. Все адреса сайта (страницы + API) одним списком: [URLS.md](URLS.md).

---

## Страницы фронтенда (React Router)

Это адреса в браузере (`http://localhost:5173/...`), их делает фронтенд. Бэкенд отдаёт только JSON по `/api/v1/...`.
✅ — API уже готов, 🕓 — API будет позже (ДЗ, дашборд и т.д. по `docs/ARCHITECTURE.md`).

| Страница | Путь | Доступ | Что на странице | Какие API вызывает |
|---|---|---|---|---|
| Главная / дашборд | `/` | 🔒 | приветствие, последние чаты, карточки к повторению, (позже) ДЗ | ✅ `GET /users/me`, ✅ `GET /chats`, ✅ `GET /flashcards/stats` · 🕓 `GET /dashboard` |
| Настройки | `/settings` | 🔒 | только фронтенд (своего API нет) | — |
| Статус | `/status` | 🔓 | карта сайта, только фронтенд (своего API нет) | — |
| Конфиденциальность | `/privacy` | 🔓 | политика конфиденциальности, только фронтенд | — |
| Условия | `/terms` | 🔓 | условия использования, только фронтенд | — |
| Вход | `/login` | 🔓 | email + пароль | ✅ `POST /auth/login` |
| Регистрация | `/register` | 🔓 | email, пароль, имя | ✅ `POST /auth/register` |
| Профиль | `/profile` | 🔒 | имя, язык, часовой пояс, смена пароля, выход | ✅ `GET/PATCH /users/me`, ✅ `PUT /users/me/password` |
| Список чатов | `/chat` | 🔒 | чаты, кнопка «Новый чат», вкладка «Архив» | ✅ `GET /chats?archived=false/true`, ✅ `POST /chats` |
| Чат | `/chat/:chatId` | 🔒 | история, поле ввода, «Тьютор печатает…», переименовать / в архив / удалить | ✅ `GET /chats/{id}`, ✅ `GET/POST /chats/{id}/messages`, ✅ `PATCH/DELETE /chats/{id}` |
| Домашка | `/homework` | 🔒 | ДЗ со всех платформ, кнопка «Обновить» | 🕓 `GET /homework`, 🕓 `POST /homework/sync` |
| Квиз: выбор | `/quiz` | 🔒 | предмет, тема, уровень, мои квизы | ✅ `GET /subjects`, ✅ `POST /quizzes`, ✅ `GET /quizzes` |
| Квиз: прохождение | `/quiz/:quizId` | 🔒 | вопрос, ответ, пояснение, итог, «Учить карточками» | ✅ `GET /quizzes/{id}`, ✅ `POST /quizzes/{id}/answers`, ✅ `POST /quizzes/{id}/finish`, ✅ `POST /quiz-sets/{setId}/flashcards` |
| Флеш-карты | `/flashcards` | 🔒 | карточки к повторению, оценка 0–5, «Карточки от ИИ» | ✅ `GET /flashcards/due`, ✅ `POST /flashcards/{id}/review`, ✅ `POST /flashcards/generate`, ✅ `GET /flashcards/stats` |
| Прогресс | `/progress` | 🔒 | прогресс по предметам и темам | ✅ `GET /quizzes/progress`, ✅ `GET /flashcards/stats` · 🕓 `GET /dashboard` |
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
    <Route path="/quiz" element={<QuizStart />} />
    <Route path="/quiz/:quizId" element={<Quiz />} />
    <Route path="/flashcards" element={<Flashcards />} />
    <Route path="/progress" element={<Progress />} />
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
  // 401 на /auth/login = неверный пароль: показываем ошибку, а не перекидываем на /login
  if (res.status === 401 && !path.startsWith("/auth/")) { localStorage.removeItem("token"); location.href = "/login"; }
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
  locale: "nb" | "nn" | "en" | "ru" | "uk";   // язык интерфейса
  timezone: string;                    // "Europe/Oslo"
  createdAt: string;                   // ISO-8601, UTC: "2026-11-07T09:40:00Z"
  lastLoginAt: string | null;
};

type AuthResponse = { token: string; user: User };

type Chat = {
  id: number;
  title: string;          // "Ny samtale" (переименуется по первому вопросу), "Ny samtale: тема" или "Quiz: тема"
  topic: string | null;
  mode: "tutor" | "quiz"; // задаётся при создании и не меняется
  subject: string | null; // название предмета
  language: string | null;// свой язык чата ("ru", "nb"…); null — ИИ пишет на языке сайта (user.locale)
  level: "beginner" | "intermediate" | "advanced";
  assignmentId: number | null; // чат по домашнему заданию
  quizSetId: number | null;    // только у чата quiz
  archived: boolean;
  createdAt: string;
  updatedAt: string;      // последнее сообщение или изменение чата (по нему сортируется список)
};

type Message = {
  id: number;
  role: "student" | "tutor";
  content: string;        // у tutor — Markdown (рендерить, например, react-markdown)
  model: string | null;   // какая модель ответила (только у tutor)
  createdAt: string;
};

type ChatReply = { chat: Chat; userMessage: Message; assistantMessage: Message };

type Level = "beginner" | "intermediate" | "advanced";
type Subject = { id: number; name: string };

type Question = {
  position: number;              // с 1
  exerciseId: number;
  type: "single_choice" | "multiple_choice" | "text";
  question: string;              // может содержать LaTeX в $...$
  options: string[] | null;      // null у "text"
};

type Quiz = {
  id: number;
  quizSetId: number;             // набор вопросов (общий для всех, кто проходит этот квиз)
  title: string;
  subject: string | null;
  topic: string;
  level: Level;
  status: "in_progress" | "completed" | "abandoned";
  currentPosition: number;       // на каком вопросе остановился ученик
  questions: Question[];         // без правильных ответов
};

type QuizSummary = {
  id: number; quizSetId: number; chatId: number | null; assignmentId: number | null;
  title: string; subject: string | null; topic: string; level: Level;
  status: Quiz["status"]; score: number | null; currentPosition: number; questionCount: number;
  startedAt: string; completedAt: string | null;
};

type QuizProgress = {
  subject: string | null; topic: string; quizzesCompleted: number; quizzesInProgress: number;
  bestScore: number | null; lastScore: number | null; lastActivity: string;
};

type AnswerResult = {
  verdict: "correct" | "partial" | "incorrect";
  explanation: string | null;    // обратная связь: почему так, где ошибка
  correctAnswer: string;         // у multiple_choice — JSON-массив строкой: '["A","C"]'
};

type Flashcard = {
  exerciseId: number; subject: string | null; topic: string;
  front: string;                 // вопрос
  back: string;                  // правильный ответ
  explanation: string | null;
  nextReviewAt: string;
  intervalDays: number;          // SM-2 progress: >= 1 means learned
  reviews: number;               // 0 = new card
  lapses: number;                // times forgotten after it was remembered
  ease: number;                  // 2.5 for a new card, lower = harder to remember
  due: boolean;                  // due for review now
};

type FlashcardStats = { cards: number; dueNow: number; learned: number; reviews: number };
type ReviewResult = { nextReviewAt: string; intervalDays: number };
type Deck = { quizSetId: number; chatId: number | null; generated: boolean; added: number; cards: Flashcard[] };

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
{ "email": "ola@example.com", "password": "hemmelig123" }
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
- `locale` — `nb`, `nn`, `en`, `ru`, `uk`
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
Тело необязательно, все поля тоже.
```json
{ "title": "Algebra", "topic": "lineære funksjoner", "subject": "Matematikk",
  "mode": "tutor", "level": "beginner", "assignmentId": null }
```
- `mode` — `tutor` (по умолчанию) или `quiz`, потом не меняется
- `subject` — название предмета; если такого ещё нет, он создаётся
- `level` — `beginner` / `intermediate` (по умолчанию) / `advanced`
- **201** → `Chat`
- **404** → `assignmentId` не найден (или чужое ДЗ)
- Без `title` чат называется «Ny samtale» (или «Ny samtale: тема») и переименовывается по первому вопросу.
- **Язык.** ИИ отвечает на языке сайта (`user.locale`, меняется в `PATCH /users/me`) и не спрашивает язык в чате.
  `language` у чата — свой язык чата, если он задан; `null` — язык сайта. Квизы составляются на этом же языке.

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
- **503** → промпты ИИ не загружены в базу (настройка сервера). Вопрос при этом не сохраняется.
- Тьютор знает предмет, тему, уровень, текст ДЗ (если чат по ДЗ), слабые темы ученика и учебные материалы.
- Ответ идёт 2–15 секунд: покажите индикатор «Тьютор печатает…».
- **Квиз-чат** (`mode: "quiz"`): сообщение ученика — это просьба («квиз по дробям», «сложнее»).
  ИИ составляет квиз, в ответе `assistantMessage` = «Quizen «…» er klar: 8 spørsmål.», а в `chat.quizSetId` — набор.
  Кнопка «Начать» → `POST /quiz-sets/{chat.quizSetId}/start`.

---

## Откуда ИИ берёт промпты

Тексты промптов лежат в GitHub: `ai-content/prompts/*.md` (ветка `PythonAPI`, там же база `database/`).
`python -X utf8 ai-content/load/load.py` загружает их в таблицу `ai_prompts` (новая версия, если текст изменился).
Бэкенд при каждом запросе берёт активную версию из базы, перезапуск не нужен.

| Промпт | Где используется |
|---|---|
| `tutor` | чат `mode: "tutor"` |
| `quiz_generator` | `POST /quizzes`, `POST /flashcards/generate`, сообщения в чате `mode: "quiz"` |
| `answer_checker` | `POST /quizzes/{id}/answers` для вопросов `text` |

Промпт не загружен → **503**.

---

## Квизы

### `GET /subjects` — предметы
- **200** → `Subject[]` по алфавиту. Предмет появляется, когда создаётся чат или квиз с новым названием.

### `POST /quizzes` — начать квиз
```json
{ "subject": "Matematikk", "topic": "Brøk", "level": "beginner", "count": 8, "assignmentId": null, "fresh": false }
```
- `subject`, `topic` — обязательны (до 100 символов); `level` — по умолчанию `intermediate`;
  `count` — 1–30, по умолчанию 8; `assignmentId` — квиз по ДЗ (ИИ получит текст задания);
  `fresh: true` — всегда новый квиз от ИИ.
- Сначала ищется **готовый** набор с тем же предметом, темой, уровнем и языком профиля: ответ мгновенный, ИИ не тратится.
  Нет готового (или квиз по ДЗ, или `fresh`) → ИИ составляет новый (3–30 секунд, покажите «ИИ составляет квиз…»).
  Для нового набора создаётся квиз-чат (`QuizSummary.chatId`).
- Если такой квиз уже начат и не закончен — вернётся он же (продолжить с `currentPosition`).
- **201** → `Quiz` · **404** → чужое ДЗ · **502** → ИИ недоступен или дважды прислал неверный формат · **503** → нет промпта

### `POST /quiz-sets/{setId}/start` — начать квиз по набору
Для квиз-чата: `setId` = `chat.quizSetId`. **201** → `Quiz` · **404** → нет набора

### `GET /quizzes` — мои квизы
- **200** → `QuizSummary[]`, новые сверху

### `GET /quizzes/{quizId}` — квиз с вопросами
- **200** → `Quiz` (правильных ответов нет) · **404** → нет такого или чужой

### `POST /quizzes/{quizId}/answers` — ответ
```json
{ "position": 1, "answer": "teller" }
```
- `single_choice` и `text` — строка; `multiple_choice` — JSON-массив **строкой**:
  `{"position": 3, "answer": "[\"1/3\",\"4/12\"]"}` (в JS: `answer: JSON.stringify(["1/3", "4/12"])`).
- Выбор проверяет база мгновенно (`partial` — выбраны не все верные). Текст проверяет ИИ по смыслу (1–5 секунд).
- Отвечать на один вопрос можно несколько раз, в балл идёт последний ответ.
- Неверный ответ сразу попадает во флеш-карты.
- **200** → `AnswerResult` · **400** → нет такого вопроса / квиз завершён / `multiple_choice` не массив

### `POST /quizzes/{quizId}/finish` — завершить
- **200** → `{ "score": 87.5 }` — 0–100, `partial` = половина. Повторный вызов вернёт тот же балл.

### `GET /quizzes/progress` — прогресс
- **200** → `QuizProgress[]` по предметам и темам

---

## Флеш-карты

Карточка — это вопрос квиза: лицевая сторона — вопрос, обратная — ответ и пояснение. Повторение по алгоритму SM-2.

### `GET /flashcards` и `GET /flashcards/due`
- `GET /flashcards` — вся колода, `?due=true` или `GET /flashcards/due` — только те, что пора повторить.
- **200** → `Flashcard[]`, ближайшие сверху

### `POST /flashcards/{exerciseId}/review` — оценка
```json
{ "grade": 4 }
```
- `0–2` — не помню (покажется через 10 минут), `3` — с трудом, `4` — помню, `5` — легко
- **200** → `ReviewResult` · **400** → grade вне 0–5 · **404** → нет такой карточки

### `POST /flashcards/generate` — карточки от ИИ
Тело как у `POST /quizzes`. Готовый набор или новый от ИИ (промпт `quiz_generator`) → все вопросы в колоду.
- **201** → `Deck` (`added` — сколько новых, `cards` — карточки набора)

### `POST /quiz-sets/{setId}/flashcards` — весь квиз в колоду
Кнопка «Учить карточками» после квиза. **200** → `Deck`

### `GET /flashcards/stats`
- **200** → `FlashcardStats` (`learned` — интервал от 3 недель)

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
| 502 | ИИ недоступен или дважды прислал квиз в неверном формате | показать `message` и кнопку «Повторить» |
| 503 | промпты ИИ не загружены в базу (ошибка настройки сервера) | показать `message` |
| 500 | ошибка сервера | «Что-то пошло не так» |

---

## Подключение в Vite (dev)

`vite.config.ts` — запросы `/api/...` уходят на бэкенд, CORS настраивать не нужно:
```ts
server: { proxy: { "/api": "http://localhost:8080" } }
```
Без proxy бэкенд разрешает запросы только с `http://localhost:5173`.
