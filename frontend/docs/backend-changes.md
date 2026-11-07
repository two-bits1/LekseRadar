# Бэкенд: что сделать, чтобы фронт `Frontend-setup` работал полностью

Сверено с кодом веток `JavaAPI-setup` (db45af8, Java) и `PythonAPI` (homework-collector).

| | Что | Состояние на бэкенде | Что делать |
|---|---|---|---|
| 1 | Регистрация с кодом из письма (#1, #44) | готово | только настроить почту (раздел 1) |
| 2 | Платформы Canvas / Teams / itslearning / InSchool (#35–#42) | готово | только настроить переменные (раздел 1) |
| 3 | «Oppdater» на `/homework` (#43) | готово | — |
| 4 | Google Classroom (#45, #46) | **готово** (миграция V22, Java) | ключи Google на сервере (раздел 2.4) |
| 5 | Ачивки в профиле (#47) | таблицы есть, API нет | раздел 3, по желанию |

Если на сервере Classroom нет (404 / 405 / 501), карточка пишет «Google Classroom er klart her, men serveren støtter det ikke ennå».
На 400 (устаревшая или чужая ссылка входа, нет `code`) фронт показывает `message` бэкенда.

---

## 1. Настройка (без этого не работают пункты 1–3)

`backend/.env`. Ни один из этих ключей **не коммитить**:

| Переменная | Зачем | Без неё |
|---|---|---|
| `JWT_SECRET` | вход | бэкенд не стартует |
| `CREDENTIALS_KEY` | шифрование доступов платформ (`openssl rand -base64 32`) | платформы не сохраняются |
| `COLLECTOR_URL`, `COLLECTOR_KEY` | сервис сбора ДЗ, ключ одинаковый у бэкенда и collector | 503 «Henting av lekser er nede» |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | письма с кодом (Gmail: пароль приложения) | регистрация отвечает 503 |
| `MAIL_DEV_LOG_CODES=true` | только для своего компьютера: код печатается в лог вместо письма | — |
| `CORS_ORIGINS` | `http://localhost:5173` и адрес фронта на Vercel через запятую | браузер блокирует запросы |
| `TEAMS_CLIENT_ID` (у collector) | вход в Teams по коду | 422 «Не задан client_id…», ручной ввод токена работает |

Проверка: `GET /actuator/health` → `UP`, Swagger `http://localhost:8080/swagger-ui.html`, разделы «1. Авторизация», «6. Домашка», «8. Платформы».

---

## 2. Google Classroom

### Как это выглядит для ученика (фронт уже сделан)

```
Фронт                                     Java                                        collector        Google
──────                                    ────                                        ─────────        ──────
«Logg inn med Google»
 GET /platforms/classroom/url  ────────►  проверить redirectUri, собрать ссылку
   ?redirectUri=<origin>/settings/platforms
                               ◄────────  { url }
 window.location = url  ─────────────────────────────────────────────────────────────────────────►  вход, согласие
 /settings/platforms?code=…&state=…  ◄───────────────────────────────────────────────────────────  redirect
 POST /platforms/classroom  ───────────►  проверить state, code → токен у Google  ─────────────────►  token
   { code, state, redirect_uri }          collect({classroom: токен}) ─────────────►  /collect ───►  Classroom API
                               ◄────────  PlatformConnection (ДЗ уже в базе, ~6 с)
```

Google Device Flow для Classroom не подходит: scope `classroom.*` там запрещены. Поэтому вход сделан как у itslearning:
адрес входа выдаёт сервер, а код Google возвращает на фронт.

### Контракт (фронт шлёт ровно это)

| # | Запрос | Тело / параметры | Ответ |
|---|---|---|---|
| 45 | `GET /api/v1/platforms/classroom/url` 🔒 | `?redirectUri=http://localhost:5173/settings/platforms` | 200 `{ "url": "https://accounts.google.com/o/oauth2/v2/auth?…" }` |
| 46 | `POST /api/v1/platforms/classroom` 🔒 | `{ "code": "4/0A…", "state": "…", "redirect_uri": "http://localhost:5173/settings/platforms" }` | 200 `PlatformConnection` |
| 35 | `GET /api/v1/platforms` | — | теперь **5** строк, пятая `{ "platform": "classroom", … }` |
| 42 | `DELETE /api/v1/platforms/classroom` | — | 204 |
| 43 | `POST /api/v1/homework/sync` | — | в `sources` может быть `{ "platform": "classroom", … }` |

Ошибки, как у других платформ:
- `400`: неверный `redirectUri` / `state` / `code`;
- `422`: Google не принял код, в `message` текст от Google (фронт покажет «Plattformen godtok ikke tilgangen. …»);
- `429`: лимит подключений;
- `502`: Google недоступен;
- `503`: collector недоступен.

**Не отдавать 401**: на 401 фронт разлогинивает ученика.

### 2.1. База: миграция `database/migrations/V22__classroom.sql`

```sql
-- Google Classroom как пятая платформа (ДЗ и подключения)
ALTER TYPE platform ADD VALUE IF NOT EXISTS 'classroom';
```

`ALTER TYPE … ADD VALUE` нельзя использовать в той же транзакции, где новое значение уже применяется. В этой миграции больше ничего не писать.
Если где-то стоит `CHECK (source IN (…))` вместо enum, его тоже расширить.

### 2.2–2.3. Java и collector: сделано иначе, чем предлагалось

Бэкенд №2 сделал Classroom по-своему. Контракт для фронта (#45, #46) остался ровно как в таблице выше.

- **Java** сама выдаёт ссылку на вход Google, меняет `code` на ключи, хранит их зашифрованными и обновляет токен.
- **collector** только собирает задания по уже готовому токену, ключи Google ему **не нужны**.
- Предложенные здесь раньше `OAuthState`, методы `CollectorClient.classroomUrl/classroomExchange` и
  `/login/classroom/*` в collector **не нужны**.

Ошибки, которые фронт показывает:

| Код | Когда | Что видит ученик |
|---|---|---|
| 400 | `redirectUri` не из `CORS_ORIGINS` + `/settings/platforms`; метка старше 10 минут или чужая; нет `code` | `message` бэкенда |
| 422 | Google не принял код, или при входе сняли галочки доступа | «Plattformen godtok ikke tilgangen.» + `message` |
| 429 | больше 10 подключений за 10 минут | «For mange forsøk…» |
| 502 | Google не ответил | «Plattformen svarer ikke…» |
| 503 | нет ключей Google, миграции V22 или collector без Classroom | «Henting av lekser er nede…» |

После возврата от Google подключение идёт ~6 с, всё это время карточка показывает «Kobler til Google Classroom … henter lekser».
После успеха появляются «Tilkoblet» и ссылка «Se lekser».

### 2.4. Google Cloud Console (один раз)

1. Проект → **APIs & Services → Library → Google Classroom API → Enable**.
2. **OAuth consent screen**: External, scope `classroom.courses.readonly` и `classroom.coursework.me.readonly`.
   Пока приложение в режиме Testing, добавить тестовых учеников в «Test users».
3. **Credentials → Create OAuth client ID → Web application**. Authorized redirect URIs:
   - `http://localhost:5173/settings/platforms`;
   - `https://<адрес на Vercel>/settings/platforms`.
4. `GOOGLE_CLIENT_ID` и `GOOGLE_CLIENT_SECRET` записать в env **бэкенда** (`backend/.env`, на Railway в Variables), не в репозиторий.
5. Домен фронта на Vercel должен совпадать с Redirect URI и с `CORS_ORIGINS`: сейчас ожидается `https://lekseradar.vercel.app`.
   Если выдан другой, его нужно поменять и в Google, и на Railway.

Школьные Google-аккаунты (Workspace for Education): админ школы может запретить сторонние приложения.
Тогда Google вернёт ошибку, а фронт покажет «Innloggingen hos Google ble avbrutt» или текст 422.

### 2.5. Проверка вручную

1. `docker compose up -d` (база с V22 + collector), бэкенд с `GOOGLE_*`, фронт `pnpm dev`.
2. Войти → `/settings/platforms` → Google Classroom → «Logg inn med Google» → войти тестовым учеником.
3. Возврат на сайт: карточка «Tilkoblet», `GET /platforms` содержит `classroom`.
4. `/homework`: задания с подписью «Google Classroom».
5. Отозвать доступ на https://myaccount.google.com/permissions → «Oppdater» на `/homework` →
   строка «Google Classroom: … Koble til på nytt».

---

## 3. Ачивки (по желанию, для профиля)

Таблицы `achievements` и `user_achievements` уже есть (V6), `AchievementEntity` и `UserAchievementEntity` тоже, API нет.

| # | Запрос | Ответ |
|---|---|---|
| 47 | `GET /api/v1/achievements` 🔒 | 200 `Achievement[]`: все из справочника, порядок как в V6 |

```ts
type Achievement = {
  code: string;               // first_chat | first_quiz | perfect_quiz | streak_7 | homework_zero | connected_all
  title: string;              // из V6
  description: string;
  icon: string;               // chat | quiz | star | fire | check | radar
  unlockedAt: string | null;  // null — ещё закрыта
};
```

Выдача (`unlock`, повтор ничего не делает):

| `code` | Когда |
|---|---|
| `first_chat` | первое сообщение тьютору |
| `first_quiz` | первый законченный квиз |
| `perfect_quiz` | квиз на 100 % |
| `streak_7` | 7 дней активности подряд |
| `homework_zero` | нет `overdue` |
| `connected_all` | успешный `verifyAndSave` любой платформы |

Описания в V6 по-русски, поэтому фронт покажет свои тексты по `code` на nb/en/ru, а `title` / `description` возьмёт как запасные.
