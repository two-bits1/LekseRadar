# Все URL LekseRadar

Один список всех адресов сайта: страницы фронтенда, API бэкенда и служебные адреса.
Подробно про запросы, ответы и типы — в [API.md](API.md). Живая документация — http://localhost:8080/swagger-ui.html.

✅ — работает · 🕓 — будет позже · 🔓 — без входа · 🔒 — нужен `Authorization: Bearer <token>`

---

## Адреса при разработке

| Что | Адрес |
|---|---|
| Фронтенд (Vite) | http://localhost:5173 |
| Бэкенд (API) | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI-схема | http://localhost:8080/v3/api-docs |
| Проверка сервера | http://localhost:8080/actuator/health |
| PostgreSQL | `localhost:5432`, база / пользователь / пароль `lekseradar` |

---

## Страницы фронтенда

| Путь | Доступ | Страница | API на странице |
|---|---|---|---|
| `/` | 🔒 | главная: приветствие, последние чаты, карточки к повторению | `GET /users/me`, `GET /chats`, `GET /flashcards/stats` |
| `/login` | 🔓 | вход | `POST /auth/login` |
| `/register` | 🔓 | регистрация | `POST /auth/register` |
| `/profile` | 🔒 | имя, язык, часовой пояс, пароль, выход | `GET/PATCH /users/me`, `PUT /users/me/password` |
| `/chat` | 🔒 | список чатов, «Новый чат», архив | `GET /chats`, `POST /chats` |
| `/chat/:chatId` | 🔒 | чат с тьютором или квиз-чат | `GET /chats/{id}`, `GET/POST /chats/{id}/messages`, `PATCH/DELETE /chats/{id}`; квиз-чат: `POST /quiz-sets/{quizSetId}/start` |
| `/quiz` | 🔒 | новый квиз (предмет, тема, уровень) и мои квизы | `GET /subjects`, `POST /quizzes`, `GET /quizzes` |
| `/quiz/:quizId` | 🔒 | вопрос → ответ → пояснение → итог | `GET /quizzes/{id}`, `POST /quizzes/{id}/answers`, `POST /quizzes/{id}/finish`, `POST /quiz-sets/{setId}/flashcards` |
| `/flashcards` | 🔒 | повторение карточек, «Карточки от ИИ» | `GET /flashcards/due`, `POST /flashcards/{id}/review`, `POST /flashcards/generate`, `GET /flashcards/stats` |
| `/progress` | 🔒 | прогресс по предметам и темам | `GET /quizzes/progress`, `GET /flashcards/stats` · 🕓 `GET /dashboard` |
| `/history` | 🔒 | лента «что я делал» | 🕓 `GET /history` |
| `/homework` | 🔒 | ДЗ со всех платформ | 🕓 `GET /homework`, 🕓 `POST /homework/sync` |
| `/settings/platforms` | 🔒 | подключение Canvas / Teams / itslearning / InSchool | 🕓 `GET /platforms`, 🕓 `POST/DELETE /platforms/{platform}` |
| `/settings` | 🔒 | настройки (только фронтенд) | — |
| `/status` | 🔓 | карта сайта: что уже работает (только фронтенд) | — |
| `/privacy` | 🔓 | политика конфиденциальности (только фронтенд, тексты `frontend/src/legal/`) | — |
| `/terms` | 🔓 | условия использования (только фронтенд, тексты `frontend/src/legal/`) | — |
| `*` | 🔓 | «Страница не найдена» | — |

---

## API бэкенда (`/api/v1`)

### Вход и профиль
| | Метод | URL | Что делает |
|---|---|---|---|
| ✅ 🔓 | POST | `/api/v1/auth/register` | регистрация → токен |
| ✅ 🔓 | POST | `/api/v1/auth/login` | вход → токен |
| ✅ 🔒 | GET | `/api/v1/users/me` | мой профиль |
| ✅ 🔒 | PATCH | `/api/v1/users/me` | имя, язык, часовой пояс |
| ✅ 🔒 | PUT | `/api/v1/users/me/password` | сменить пароль |

### ИИ-чат (тьютор и квиз-чат)
| | Метод | URL | Что делает |
|---|---|---|---|
| ✅ 🔒 | GET | `/api/v1/chats?archived=` | список чатов |
| ✅ 🔒 | POST | `/api/v1/chats` | новый чат (`mode`: `tutor` или `quiz`) |
| ✅ 🔒 | GET | `/api/v1/chats/{chatId}` | один чат |
| ✅ 🔒 | PATCH | `/api/v1/chats/{chatId}` | переименовать, тема, архив |
| ✅ 🔒 | DELETE | `/api/v1/chats/{chatId}` | удалить |
| ✅ 🔒 | GET | `/api/v1/chats/{chatId}/messages` | история |
| ✅ 🔒 | POST | `/api/v1/chats/{chatId}/messages` | сообщение → ответ ИИ (в квиз-чате ИИ составляет квиз) |

### Квизы (ИИ составляет вопросы и проверяет текстовые ответы)
| | Метод | URL | Что делает |
|---|---|---|---|
| ✅ 🔒 | GET | `/api/v1/subjects` | все предметы |
| ✅ 🔒 | GET | `/api/v1/quizzes` | мои квизы |
| ✅ 🔒 | GET | `/api/v1/quizzes/progress` | прогресс по предметам и темам |
| ✅ 🔒 | POST | `/api/v1/quizzes` | начать квиз: готовый из базы или новый от ИИ |
| ✅ 🔒 | GET | `/api/v1/quizzes/{quizId}` | квиз с вопросами (без ответов) |
| ✅ 🔒 | POST | `/api/v1/quizzes/{quizId}/answers` | ответить на вопрос |
| ✅ 🔒 | POST | `/api/v1/quizzes/{quizId}/finish` | завершить → балл 0–100 |
| ✅ 🔒 | POST | `/api/v1/quiz-sets/{setId}/start` | начать квиз по набору (из квиз-чата) |

### Флеш-карты
| | Метод | URL | Что делает |
|---|---|---|---|
| ✅ 🔒 | GET | `/api/v1/flashcards?due=` | вся колода (`due=true` — только к повторению) |
| ✅ 🔒 | GET | `/api/v1/flashcards/due` | что повторить сейчас |
| ✅ 🔒 | GET | `/api/v1/flashcards/stats` | всего, к повторению, выучено |
| ✅ 🔒 | POST | `/api/v1/flashcards/{exerciseId}/review` | оценка 0–5 → когда показать снова |
| ✅ 🔒 | POST | `/api/v1/flashcards/generate` | карточки от ИИ по предмету и теме |
| ✅ 🔒 | POST | `/api/v1/quiz-sets/{setId}/flashcards` | весь квиз в колоду |

### Будет позже
| | Метод | URL | Что делает |
|---|---|---|---|
| 🕓 🔒 | GET | `/api/v1/dashboard` | сводка для главной и прогресса |
| 🕓 🔒 | GET | `/api/v1/history` | лента событий |
| 🕓 🔒 | GET | `/api/v1/homework` | домашние задания |
| 🕓 🔒 | POST | `/api/v1/homework/sync` | обновить ДЗ с платформ |
| 🕓 🔒 | GET | `/api/v1/platforms` | подключённые платформы |
| 🕓 🔒 | POST / DELETE | `/api/v1/platforms/{platform}` | подключить / отключить платформу |
| 🕓 🔒 | GET / POST | `/api/v1/materials` | учебные материалы |
| 🕓 🔒 | GET | `/api/v1/achievements` | ачивки |

### Служебное (без `/api/v1`)
| | Метод | URL | Что делает |
|---|---|---|---|
| ✅ 🔓 | GET | `/actuator/health` | `{"status":"UP"}` — сервер и база живы |
| ✅ 🔓 | GET | `/swagger-ui.html` | интерактивная документация |
| ✅ 🔓 | GET | `/v3/api-docs` | OpenAPI-схема для генерации клиента |

---

## Готовые константы для фронтенда

Можно скопировать в `src/api/urls.ts`:

```ts
export const API = "/api/v1";

export const URLS = {
  register: `${API}/auth/register`,
  login: `${API}/auth/login`,
  me: `${API}/users/me`,
  password: `${API}/users/me/password`,

  chats: `${API}/chats`,
  chat: (id: number) => `${API}/chats/${id}`,
  chatMessages: (id: number) => `${API}/chats/${id}/messages`,

  subjects: `${API}/subjects`,
  quizzes: `${API}/quizzes`,
  quizProgress: `${API}/quizzes/progress`,
  quiz: (id: number) => `${API}/quizzes/${id}`,
  quizAnswer: (id: number) => `${API}/quizzes/${id}/answers`,
  quizFinish: (id: number) => `${API}/quizzes/${id}/finish`,
  quizSetStart: (setId: number) => `${API}/quiz-sets/${setId}/start`,
  quizSetToFlashcards: (setId: number) => `${API}/quiz-sets/${setId}/flashcards`,

  flashcards: `${API}/flashcards`,
  flashcardsDue: `${API}/flashcards/due`,
  flashcardStats: `${API}/flashcards/stats`,
  flashcardReview: (exerciseId: number) => `${API}/flashcards/${exerciseId}/review`,
  flashcardsGenerate: `${API}/flashcards/generate`,
} as const;

export const PAGES = {
  home: "/",
  login: "/login",
  register: "/register",
  profile: "/profile",
  chats: "/chat",
  chat: (id: number) => `/chat/${id}`,
  quizNew: "/quiz",
  quiz: (id: number) => `/quiz/${id}`,
  flashcards: "/flashcards",
  progress: "/progress",
  history: "/history",
  homework: "/homework",
  platforms: "/settings/platforms",
  // только фронтенд, своего API нет
  settings: "/settings",
  status: "/status",
  privacy: "/privacy",
  terms: "/terms",
} as const;
```
