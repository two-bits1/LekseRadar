# Что нужно поменять в Java-бэкенде (для фронта `Frontend-setup`)

Фронт уже работает по `FIX-frontend-auth-platforms` (регистрация с кодом, платформы `POST /platforms/{platform}`,
Teams/itslearning-вход, `POST /homework/sync`). Там бэкенд готов, менять ничего не нужно.

Нового на бэкенде: **Google Classroom**. Без него карточка Classroom на `/settings/platforms` показывает
«Google Classroom er klart her, men serveren støtter det ikke ennå» (фронт так понимает ответы 400 / 404 / 405 / 501).

Ниже ещё одна необязательная вещь: ачивки для профиля.

---

## 1. Google Classroom (обязательно для подключения Classroom)

Вход через Google OAuth 2.0 (authorization code). Device flow у Google для Classroom не подходит:
scope `classroom.*` там не разрешены. Поэтому схема как у itslearning: сервер выдаёт адрес входа, Google возвращает
ученика **на фронт**, фронт отдаёт код серверу.

### Что делает фронт (уже сделано)

1. Кнопка «Logg inn med Google» → `GET /api/v1/platforms/classroom/url?redirectUri=<origin>/settings/platforms`
   → ответ `{ "url": "https://accounts.google.com/o/oauth2/v2/auth?..." }` → `window.location.assign(url)`.
2. Google возвращает на `<origin>/settings/platforms?code=…&state=…` (или `?error=access_denied&state=…`).
3. Фронт сразу убирает `code` из адресной строки и шлёт
   `POST /api/v1/platforms/classroom` с телом `{ "code": "…", "state": "…", "redirect_uri": "<origin>/settings/platforms" }`.
4. Ответ — обычный `PlatformConnection`; карточка становится «Tilkoblet».
5. `GET /platforms` может вернуть и пятую строку `{ "platform": "classroom", … }` — фронт её покажет. Если строки нет, карточка «не подключена».
6. `DELETE /platforms/classroom` → 204, как у остальных.
7. `POST /homework/sync` → в `sources` может быть `{ "platform": "classroom", … }`.

### Что сделать на бэкенде

| # | Запрос | Что делает | Ответ |
|---|---|---|---|
| 45 | `GET /api/v1/platforms/classroom/url?redirectUri=` 🔒 | собрать адрес входа Google | 200 `{ url }` |
| 46 | `POST /api/v1/platforms/classroom` 🔒 `{ code, state, redirect_uri }` | обменять код на токены, проверить, сохранить, записать ДЗ | 200 `PlatformConnection` |

- В `PlatformKey` / проверке `platform:` добавить `classroom` (сейчас ответ `400 "platform: canvas | teams | itslearning | inschool"`).
  Тогда `DELETE /platforms/classroom` и `GET /platforms` заработают тем же кодом.
- **#45**
  - `redirectUri` принимать только из белого списка (`CORS_ORIGINS` + `/settings/platforms`), иначе `400`.
  - Параметры: `client_id`, `redirect_uri`, `response_type=code`, `access_type=offline`, `prompt=consent`
    (иначе Google не отдаст `refresh_token` повторно), `include_granted_scopes=true`.
  - `scope`: `https://www.googleapis.com/auth/classroom.courses.readonly`,
    `https://www.googleapis.com/auth/classroom.coursework.me.readonly`.
  - `state`: подписанная строка (HMAC с секретом сервера) из `userId` + время + случайность, живёт 10 минут.
    Фронт его не разбирает, только возвращает.
- **#46**
  - Проверить `state`: подпись, срок, тот же пользователь. Иначе `400 "state"`.
  - `redirect_uri` из тела должен совпасть с тем, что был в #45 (Google это тоже проверит).
  - Обмен: `POST https://oauth2.googleapis.com/token` (`code`, `client_id`, `client_secret`, `redirect_uri`, `grant_type=authorization_code`).
  - Сохранить `refresh_token` **зашифрованным**, как токены других платформ. `access_token` можно не хранить.
  - Ошибка Google `invalid_grant` → `422` с текстом от Google (фронт покажет «Plattformen godtok ikke tilgangen. …»).
  - Google недоступен → `502`.
- **Сбор ДЗ** (collector, Python):
  - `GET https://classroom.googleapis.com/v1/courses?courseStates=ACTIVE&studentId=me`;
  - по каждому курсу `GET /v1/courses/{id}/courseWork` и `GET /v1/courses/{id}/courseWork/-/studentSubmissions?userId=me`.
  - В `Assignment`:
    - `source = "classroom"`;
    - `title`; `course` = имя курса;
    - `dueAt` из `dueDate` + `dueTime` (UTC);
    - `url = alternateLink`;
    - `kind` из `workType`;
    - `description`;
    - `status`: `TURNED_IN` → `submitted`, `RETURNED` с оценкой → `graded`, иначе `todo`.
  - `refresh_token` отозван → `needsReconnect: true`.
- `Assignment.source` и тип `Platform` расширить значением `classroom`. В БД колонка `source`, проверить CHECK / enum.
- Настройки:
  - `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` в `.env`, **не в репозиторий**;
  - в Google Cloud Console включить Classroom API;
  - в OAuth client добавить Authorized redirect URI: `http://localhost:5173/settings/platforms` и адрес на Vercel.
- Лимит как у остальных: больше 10 подключений за 10 минут → `429`.
- Swagger: раздел «8. Платформы», примеры тел для #45 и #46.

### Проверка

1. Войти на сайт → `/settings/platforms` → Google Classroom → «Logg inn med Google».
2. После входа в Google вернуться на сайт: карточка «Tilkoblet», на `/homework` задания с источником Google Classroom.
3. Отозвать доступ в https://myaccount.google.com/permissions → «Oppdater» на `/homework`: строка
   «Google Classroom: … Koble til på nytt».

---

## 2. Ачивки (по желанию, для профиля)

Таблицы `achievements` и `user_achievements` уже есть (миграция V6), API нет. Фронт хочет показать в профиле
все ачивки: полученные и пока закрытые.

| # | Запрос | Ответ |
|---|---|---|
| 47 | `GET /api/v1/achievements` 🔒 | 200 `Achievement[]`, все из справочника, порядок как в V6 |

```ts
type Achievement = {
  code: string;               // first_chat | first_quiz | perfect_quiz | streak_7 | homework_zero | connected_all
  title: string;              // как в V6 (nb)
  description: string;
  icon: string;               // chat | quiz | star | fire | check | radar
  unlockedAt: string | null;  // null — ещё не получена
};
```

Выдача (`unlock`, повтор ничего не делает), когда:
- `first_chat` — первое сообщение тьютору;
- `first_quiz` — первый законченный квиз;
- `perfect_quiz` — 100 %;
- `streak_7` — 7 дней активности подряд;
- `homework_zero` — нет `overdue`;
- `connected_all` — успешное подключение платформы.

Сейчас описания в V6 по-русски. Фронт покажет свои тексты по `code` на nb/en/ru, а `title` / `description` возьмёт как запасные.
