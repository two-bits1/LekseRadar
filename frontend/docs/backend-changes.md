# Бэкенд: что сделать, чтобы фронт `Frontend-setup` работал полностью

Сверено с кодом веток `JavaAPI-setup` (db45af8, Java) и `PythonAPI` (homework-collector).

| | Что | Состояние на бэкенде | Что делать |
|---|---|---|---|
| 1 | Регистрация с кодом из письма (#1, #44) | готово | только настроить почту (раздел 1) |
| 2 | Платформы Canvas / Teams / itslearning / InSchool (#35–#42) | готово | только настроить переменные (раздел 1) |
| 3 | «Oppdater» на `/homework` (#43) | готово | — |
| 4 | **Google Classroom** (#45, #46) | **нет** | раздел 2: миграция, Java, collector |
| 5 | Ачивки в профиле (#47) | таблицы есть, API нет | раздел 3, по желанию |

Пока Classroom не сделан, карточка на `/settings/platforms` пишет
«Google Classroom er klart her, men serveren støtter det ikke ennå»: ответы 400 / 404 / 405 / 501 фронт так и понимает.

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
Фронт                                Java                                   collector                 Google
──────                               ────                                   ─────────                 ──────
«Logg inn med Google»
 GET /platforms/classroom/url  ───►  проверить redirectUri, подписать state
   ?redirectUri=<origin>/settings/platforms   ───────────────────────────►  GET /login/classroom/url
                               ◄───  { url }                       ◄─────  { url }
 window.location = url  ─────────────────────────────────────────────────────────────────────────►  вход, согласие
 /settings/platforms?code=…&state=…  ◄───────────────────────────────────────────────────────────  redirect
 POST /platforms/classroom  ───────►  проверить state
   { code, state, redirect_uri }      ────────────────────────────────►  POST /login/classroom/exchange ─► token
                                      collect({classroom: creds}) ─────►  /collect  ─────────────────►  Classroom API
                               ◄───  PlatformConnection (ДЗ уже в базе)
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

### 2.2. Java

**`model/enums/PlatformEnum.java`**: добавить значение. `GET /platforms` сам начнёт отдавать 5 строк:
`list()` идёт по `PlatformEnum.values()`.

```java
INSCHOOL("inschool"),
CLASSROOM("classroom");
```

**`controller/PlatformController.java`**:

```java
@GetMapping("/classroom/url")
@Operation(summary = "Google Classroom: ссылка для входа",
        description = "redirectUri — адрес фронта /settings/platforms из CORS_ORIGINS. Google вернёт туда ?code=…&state=…, "
                + "фронт отправит их в POST /platforms/classroom")
public UrlResponseDTO classroomUrl(@CurrentUser UserEntity user, @RequestParam String redirectUri) {
    return new UrlResponseDTO(platforms.classroomUrl(user.getId(), redirectUri));
}
```

`@GetMapping("/classroom/url")` должен стоять выше `@PostMapping("/{platform}")`. Пути разные по методу, но так понятнее.
В `parse()` поменять текст ошибки: `"platform: canvas | teams | itslearning | inschool | classroom"`.
В Swagger-примеры `POST /{platform}` добавить:
`@ExampleObject(name = "classroom (код от Google)", value = "{\"code\": \"4/0A…\", \"state\": \"…\", \"redirect_uri\": \"http://localhost:5173/settings/platforms\"}")`.

**`service/PlatformService.java`**:

```java
static final String CLASSROOM_PATH = "/settings/platforms";

/** Ссылка на вход Google: redirectUri только из CORS_ORIGINS + /settings/platforms, state подписан. */
public String classroomUrl(Long userId, String redirectUri) {
    String redirect = allowedRedirect(redirectUri);
    return collector.classroomUrl(redirect, oauthState.issue(userId));
}

// в connect(): новый case в switch
case CLASSROOM -> {
    String code = required(body, "code", 2_000);
    String redirect = allowedRedirect(required(body, "redirect_uri", MAX_URL));
    oauthState.verify(required(body, "state", 500), userId);   // чужой / старый / поддельный → 400
    limit(userId);
    yield null;   // токены выдаст collector, см. ниже
}
// …и после switch, перед verifyAndSave:
if (platform == PlatformEnum.CLASSROOM) {
    return verifyAndSave(userId, platform, collector.classroomExchange(code, redirect));
}

/** Только точное совпадение с origin из app.cors.allowed-origins + /settings/platforms (иначе открытый редирект). */
private String allowedRedirect(String raw) {
    String r = raw == null ? "" : raw.strip();
    boolean ok = allowedOrigins.stream().anyMatch(o -> r.equals(o.replaceAll("/+$", "") + CLASSROOM_PATH));
    if (!ok) throw new BadRequestException("redirect_uri не разрешён");
    return r;
}
```

`allowedOrigins` берётся из `AppConfig.cors().allowedOrigins()`, его же использует `SecurityConfig`.
`code` и `redirect` объявить до `switch`, как сейчас сделано с `code` у itslearning.

**Новый `security/OAuthState.java`**: state без хранения в БД, HMAC-подпись, живёт 10 минут.

```java
@Component
public class OAuthState {
    private static final Duration TTL = Duration.ofMinutes(10);
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public OAuthState(AppConfig cfg) {   // отдельный ключ из JWT_SECRET, чтобы state нельзя было выдать за JWT
        this.key = hmac(cfg.jwt().secret().getBytes(UTF_8), "oauth-state".getBytes(UTF_8));
    }

    public String issue(Long userId) {
        byte[] nonce = new byte[12]; random.nextBytes(nonce);
        String payload = userId + "." + Instant.now().getEpochSecond() + "." + B64.encodeToString(nonce);
        return payload + "." + B64.encodeToString(hmac(key, payload.getBytes(UTF_8)));
    }

    public void verify(String state, Long userId) {
        int dot = state.lastIndexOf('.');
        if (dot < 0) throw new BadRequestException("state");
        String payload = state.substring(0, dot);
        byte[] sig = B64.decode(state.substring(dot + 1));   // IllegalArgumentException → тоже 400
        String[] p = payload.split("\\.");
        if (p.length != 3 || !MessageDigest.isEqual(sig, hmac(key, payload.getBytes(UTF_8)))
                || !p[0].equals(String.valueOf(userId))
                || Instant.ofEpochSecond(Long.parseLong(p[1])).plus(TTL).isBefore(Instant.now())) {
            throw new BadRequestException("Вход через Google устарел или не ваш: начните заново");
        }
    }

    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    // B64 = пара ENC / Base64.getUrlDecoder(); hmac() = Mac.getInstance("HmacSHA256")
}
```

**`service/collector/CollectorClient.java`**: два метода, как у itslearning:

```java
public String classroomUrl(String redirectUri, String state) {
    JsonNode res = call("classroom/url", () -> http.get()
            .uri(u -> u.path("/login/classroom/url").queryParam("redirect_uri", redirectUri).queryParam("state", state).build())
            .retrieve().body(JsonNode.class));
    return res.path("url").asText();
}

public JsonNode classroomExchange(String code, String redirectUri) {
    Map<String, String> body = Map.of("code", code, "redirect_uri", redirectUri);
    return call("classroom/exchange", () -> http.post().uri("/login/classroom/exchange").body(body)
            .retrieve().body(JsonNode.class));
}
```

`call()` уже переводит ошибки collector в 422 / 502 / 503, менять не нужно.
Остальное (`collect`, `verifyAndSave`, шифрование, `HomeworkSyncService`, расписание раз в 30 минут, `DELETE`)
работает для `classroom` без изменений: везде используется `PlatformEnum`.

**Тесты (Java)**:
- `PlatformControllerTest`: `GET /platforms` → 5 строк; `GET /platforms/classroom/url?redirectUri=https://evil.com/x` → 400.
- `OAuthStateTest`: state другого пользователя → 400; просроченный → 400; испорченная подпись → 400; свой свежий → ок.
- `PlatformServiceTest`: `classroom` с подменённым `CollectorClient` → `verifyAndSave` вызван с ответом `classroomExchange`.

### 2.3. collector (Python, `homework-collector/homework`)

**`models.py`**:

```python
Source = Literal["canvas", "teams", "itslearning", "inschool", "classroom"]

class ClassroomCreds(OAuthCreds):
    """Google OAuth: refresh_token обязателен для фонового обновления (access_type=offline)."""

class Credentials(BaseModel):
    ...
    classroom: Optional[ClassroomCreds] = None
```

**`sources/classroom.py`** (новый):

```python
"""Google Classroom: официальный REST API, OAuth 2.0 от имени ученика (только чтение)."""
import os, time
from urllib.parse import urlencode
import httpx
from ..models import Assignment, ClassroomCreds, SourceError, open_status, parse_dt

AUTH = "https://accounts.google.com/o/oauth2/v2/auth"
TOKEN = "https://oauth2.googleapis.com/token"
API = "https://classroom.googleapis.com/v1"
SCOPES = ["https://www.googleapis.com/auth/classroom.courses.readonly",
          "https://www.googleapis.com/auth/classroom.coursework.me.readonly"]
KINDS = {"ASSIGNMENT": "assignment", "SHORT_ANSWER_QUESTION": "quiz", "MULTIPLE_CHOICE_QUESTION": "quiz"}

def _client():
    cid, secret = os.getenv("GOOGLE_CLIENT_ID"), os.getenv("GOOGLE_CLIENT_SECRET")
    if not cid or not secret:
        raise SourceError("Не заданы GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET у collector")
    return cid, secret

def authorize_url(redirect_uri: str, state: str) -> str:
    cid, _ = _client()
    q = {"client_id": cid, "redirect_uri": redirect_uri, "response_type": "code", "scope": " ".join(SCOPES),
         "access_type": "offline", "prompt": "consent", "include_granted_scopes": "true", "state": state}
    return f"{AUTH}?{urlencode(q)}"

def _token(client: httpx.Client, form: dict) -> dict:
    cid, secret = _client()
    r = client.post(TOKEN, data={**form, "client_id": cid, "client_secret": secret})
    if r.status_code >= 500:
        raise SourceError(f"Google не отвечает (HTTP {r.status_code}), попробуйте позже")
    data = r.json() if r.content else {}
    if r.status_code != 200 or "access_token" not in data:
        raise SourceError(f"Google не выдал токен: {data.get('error_description') or data.get('error') or r.status_code}",
                          needs_reconnect=True)
    return {"access_token": data["access_token"], "refresh_token": data.get("refresh_token") or form.get("refresh_token"),
            "expires_at": int(time.time()) + int(data.get("expires_in", 3600)) - 60}

def exchange_code(client, code: str, redirect_uri: str) -> dict:
    return _token(client, {"grant_type": "authorization_code", "code": code, "redirect_uri": redirect_uri})

def fetch(client: httpx.Client, creds: ClassroomCreds):
    updated = None
    if not creds.access_token or (creds.expires_at or 0) <= time.time():
        if not creds.refresh_token:
            raise SourceError("Нет токена Google Classroom, нужно войти", needs_reconnect=True)
        updated = _token(client, {"grant_type": "refresh_token", "refresh_token": creds.refresh_token})
        creds = ClassroomCreds(**updated)
    h = {"Authorization": f"Bearer {creds.access_token}"}

    def get(path, params=None):
        out, token = [], None
        while True:
            r = client.get(f"{API}{path}", headers=h, params={**(params or {}), "pageSize": 100, **({"pageToken": token} if token else {})})
            if r.status_code in (401, 403):
                raise SourceError("Google Classroom не принял доступ, нужно войти заново", needs_reconnect=True)
            r.raise_for_status()
            data = r.json(); out.append(data); token = data.get("nextPageToken")
            if not token: return out

    items = []
    for page in get("/courses", {"studentId": "me", "courseStates": "ACTIVE"}):
        for course in page.get("courses", []):
            cid = course["id"]
            subs = {s["courseWorkId"]: s for p in get(f"/courses/{cid}/courseWork/-/studentSubmissions", {"userId": "me"})
                    for s in p.get("studentSubmissions", [])}
            for p in get(f"/courses/{cid}/courseWork"):
                for w in p.get("courseWork", []):
                    items.append(_normalize(course, w, subs.get(w["id"], {})))
    return items, updated

def _normalize(course, w, sub) -> Assignment:
    d, t = w.get("dueDate"), w.get("dueTime") or {}
    due = parse_dt(f"{d['year']:04d}-{d['month']:02d}-{d['day']:02d}T{t.get('hours', 23):02d}:{t.get('minutes', 59):02d}:00Z") if d else None
    state = sub.get("state")
    status = ("graded" if state == "RETURNED" and sub.get("assignedGrade") is not None
              else "submitted" if state in ("TURNED_IN", "RETURNED") else open_status(due))
    return Assignment(source="classroom", id=str(w["id"]), title=w.get("title") or "(uten tittel)",
                      course=course.get("name") or "", due=due, status=status, url=w.get("alternateLink"),
                      description=w.get("description") or "", kind=KINDS.get(w.get("workType"), "task"))
```

**`collect.py`**:

```python
from .sources import canvas, classroom, inschool, itslearning, teams
SOURCES = {"canvas": canvas, "teams": teams, "itslearning": itslearning, "inschool": inschool, "classroom": classroom}
...
with ThreadPoolExecutor(max_workers=5) as pool:
```

**`server.py`**:

```python
class ClassroomExchange(BaseModel):
    code: str
    redirect_uri: str

@app.get("/login/classroom/url", dependencies=[Depends(internal_key)])
def classroom_url(redirect_uri: str, state: str):
    return {"url": classroom.authorize_url(redirect_uri, state)}

@app.post("/login/classroom/exchange", dependencies=[Depends(internal_key)])
def classroom_exchange(body: ClassroomExchange):
    with make_client() as client:
        return classroom.exchange_code(client, body.code, body.redirect_uri)
```

`SourceError` → 422 уже обрабатывает `source_error()`, сеть → `network_error()`.

**Тесты (pytest, `tests/test_classroom.py`)**, на `httpx.MockTransport`, как остальные:
- `TURNED_IN` → `submitted`;
- `RETURNED` с оценкой → `graded`;
- без сдачи и с прошедшим сроком → `overdue`;
- 401 от API → `needs_reconnect`;
- просроченный `access_token` → обновление через `refresh_token`, новые токены в `updated`.

### 2.4. Google Cloud Console (один раз)

1. Проект → **APIs & Services → Library → Google Classroom API → Enable**.
2. **OAuth consent screen**: External, scope `classroom.courses.readonly` и `classroom.coursework.me.readonly`.
   Пока приложение в режиме Testing, добавить тестовых учеников в «Test users».
3. **Credentials → Create OAuth client ID → Web application**. Authorized redirect URIs:
   - `http://localhost:5173/settings/platforms`;
   - `https://<адрес на Vercel>/settings/platforms`.
4. `GOOGLE_CLIENT_ID` и `GOOGLE_CLIENT_SECRET` записать в env **collector** (`database/docker-compose.yml` → `.env`), не в репозиторий.

Школьные Google-аккаунты (Workspace for Education): админ школы может запретить сторонние приложения.
Тогда Google вернёт ошибку, а фронт покажет «Innloggingen hos Google ble avbrutt» или текст 422.

### 2.5. Проверка вручную

1. `docker compose up -d` (база + collector с `GOOGLE_*`), бэкенд, фронт `pnpm dev`.
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
