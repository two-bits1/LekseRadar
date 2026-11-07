# План бэкенда (№2): платформы и синхронизация ДЗ

Часть общего плана [PLAN-MVP.md](../PLAN-MVP.md). Итог: ученик подключает свои платформы на сайте, ДЗ попадают в базу
сами, без консоли. Всё остальное (домашка, чат, квизы, дашборд) уже работает в `main-test-3.0`.

Общее время около 8 часов. Работайте в `main-test-3.0`, один коммит на шаг, сообщения коммитов на английском.
Перед каждым коммитом `./mvnw -q package`.

## Что уже есть и что переиспользовать

| Есть | Где | Зачем |
|---|---|---|
| `PlatformConnectionEntity`, `PlatformConnectionRepository` | `model/`, `repository/` | таблица `platform_connections`, поиск `findByUserIdAndPlatform` |
| `PlatformResponseDTO` (+ `from`, `notConnected`) | `dto/platform/` | ответ `GET /platforms`, менять не нужно |
| `SourceStatusResponseDTO`, `SyncResponseDTO` | `dto/homework/` | ответ `/homework/sync`, в `SyncResponseDTO` добавить `added`, `updated` |
| `AssignmentRepository`, `HomeworkService` | | список и статусы ДЗ |
| `RateLimiter` (`tryAcquire(key, limit, window)`) | `security/` | лимит на sync и подключения |
| `UserEventRepository` | | событие `homework_synced` |
| `RestClient` | как в `service/ai/OpenAiService` | клиент к `collector` |
| `@CurrentUser UserEntity user` | `security/CurrentUser` | текущий ученик в контроллере |
| `ApiException(HttpStatus, message)` и наследники | `exception/` | ответы с ошибкой в общем формате |

> ⚠️ **Не возвращайте 401** из эндпоинтов платформ. Фронт на любой 401 (кроме `/auth/*`) выкидывает ученика на страницу входа.
> Если платформа не приняла токен, это **422**.

## Шаг 1. Шифрование доступов (1–2 ч)

Файлы:
- `security/CredentialsCipher.java` (`@Component`)
- `config/AppConfig.java`: добавить `Credentials(String key)` → `app.credentials.key: ${CREDENTIALS_KEY}`
- `backend/.env.example`: `CREDENTIALS_KEY=` с подсказкой `openssl rand -base64 32`
- `test/.../security/CredentialsCipherTest.java`

```java
public byte[] encrypt(JsonNode creds)      // JSON → AES-256-GCM
public JsonNode decrypt(byte[] data)        // обратно; чужой ключ или испорченные байты → IllegalStateException
```

- Формат в базе: `nonce (12 байт) || ciphertext+tag`. Nonce новый на каждое шифрование (`SecureRandom`).
- `Cipher.getInstance("AES/GCM/NoPadding")`, `GCMParameterSpec(128, nonce)`.
- Ключ: base64 → ровно 32 байта, иначе бэкенд не стартует с понятной ошибкой (как сейчас с `JWT_SECRET`).
- Тесты: туда-обратно; два шифрования одного JSON дают разные байты; другой ключ → исключение; испорченный байт → исключение.

Коммит: `feat(security): AES-GCM cipher for platform credentials`.

## Шаг 2. Клиент к `collector` (1 ч)

`collector` пишет №3 (Илья), контракт: `homework-collector/COLLECTOR-API.md`. До его готовности тестируйте на заглушке.

Файлы:
- `AppConfig`: `Collector(String url, String key)` → `COLLECTOR_URL` (по умолчанию `http://localhost:8090`), `COLLECTOR_KEY`
- `service/collector/CollectorClient.java`

```java
CollectResult collect(Map<PlatformEnum, JsonNode> credentials)   // POST /collect
JsonNode teamsStart()                                             // POST /login/teams/start
Optional<JsonNode> teamsPoll(String deviceCode)                   // POST /login/teams/poll, пусто = ещё ждём
String itslearningUrl(String baseUrl)                             // GET  /login/itslearning/url
JsonNode itslearningExchange(String baseUrl, String codeOrUrl)    // POST /login/itslearning/exchange

record CollectResult(JsonNode assignments,                        // массив как есть → в sync_assignments()
                     Map<String, Source> sources, Map<String, JsonNode> updatedCredentials) {}
record Source(boolean ok, int count, String error, boolean needsReconnect) {}
```

- Заголовок `X-Internal-Key: COLLECTOR_KEY` на каждый запрос. Таймаут 60 с (`collect` ходит на 4 платформы).
- `collector` недоступен → `new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Сервис сбора ДЗ недоступен")`.
- В логи не писать тела запросов: там токены.

Коммит: `feat(collector): HTTP client for the homework collector service`.

## Шаг 3. API платформ (3 ч)

Файлы: `controller/PlatformController.java` (`@RequestMapping("/api/v1/platforms")`, `@Tag(name = "7. Платформы")`),
`service/PlatformService.java`, DTO в `dto/platform/`.

| # | Метод и путь | Тело | Ответ | Что делает |
|---|---|---|---|---|
| 35 | `GET /platforms` | — | `PlatformResponseDTO[]` (всегда 4) | подключённые через `from`, остальные `notConnected` |
| 36 | `PUT /platforms/canvas` | `CanvasConnectRequestDTO(@NotBlank baseUrl, @NotBlank token)` | `PlatformResponseDTO` | проверить → сохранить → sync |
| 37 | `PUT /platforms/inschool` | `InschoolConnectRequestDTO(@NotBlank host, @NotEmpty Map<String,String> cookies)` | `PlatformResponseDTO` | так же |
| 38 | `POST /platforms/teams/start` | — | `TeamsStartResponseDTO(userCode, verificationUri, deviceCode, interval, expiresIn)` | |
| 39 | `POST /platforms/teams/poll` | `TeamsPollRequestDTO(@NotBlank deviceCode)` | `{"status":"pending"}` или `{"status":"connected","platform":{…}}` | при `connected` сохранить → sync |
| 40 | `GET /platforms/itslearning/url?baseUrl=` | — | `{"url": "…"}` | |
| 41 | `POST /platforms/itslearning` | `ItslearningConnectRequestDTO(@NotBlank baseUrl, @NotBlank codeOrUrl)` | `PlatformResponseDTO` | exchange → сохранить → sync |
| 42 | `DELETE /platforms/{platform}` | — | 204 | удалить подключение; ДЗ остаются |

Логика `PlatformService.connect(userId, platform, JsonNode creds)`:
1. **Проверка адреса** (против SSRF, сервер не должен ходить куда скажет ученик):
   - только `https://`, без порта и userinfo;
   - домен из списка: `*.instructure.com`, `*.itslearning.com`, `*.inschool.visma.no` + `app.platforms.extra-canvas-hosts`
     (переменная `CANVAS_HOSTS`, через запятую, для своих Canvas вроде `canvas.uio.no`);
   - иначе 400 «Адрес платформы не поддерживается».
2. **Проверка доступа**: `collector.collect({platform: creds})`. `ok == false` → 422 с `error` из ответа.
3. **Сохранение**: upsert в `platform_connections` (`findByUserIdAndPlatform` → новая или обновить `credentialsEnc`),
   `needsReconnect = false`, `lastError = null`. Если `updatedCredentials` пришли, шифровать их, а не исходные.
4. **ДЗ сразу**: assignments из шага 2 → `sync_assignments()` (см. шаг 4), чтобы не ходить на платформу второй раз.

Лимит: 10 подключений за 10 минут на ученика (`RateLimiter`, ключ `"platform:"+userId`) → 429.
`PlatformEnum` в пути: неизвестное значение → 400 (как `status` в `HomeworkController`).

Тесты: `PlatformServiceTest` с замоканным `CollectorClient`: хороший Canvas сохраняется зашифрованным; `http://` и чужой домен → 400;
`ok:false` → 422 и ничего не сохранено; повторное подключение обновляет ту же строку.

Коммиты: по одному на `GET`+`DELETE`, Canvas+InSchool, Teams, itslearning.

## Шаг 4. Синхронизация (2 ч)

`POST /api/v1/homework/sync` в `HomeworkController` → `HomeworkSyncService.sync(userId)`:

1. Все подключения ученика без `needsReconnect` → `decrypt` → один `collector.collect(...)`.
2. Задания в базу одним вызовом функции от №3 (миграция V21):
   ```sql
   SELECT added, updated FROM sync_assignments(:userId, CAST(:assignments AS jsonb))
   ```
   Нативный запрос в `AssignmentRepository` (`@Query(nativeQuery = true)`). UPSERT на Java писать не нужно.
3. По каждой платформе из `sources`:
   - `ok` → `lastSyncedAt = now`, `lastError = null`;
   - иначе `lastError = error`, `needsReconnect = needsReconnect`.
4. `updatedCredentials` → `encrypt` → `credentialsEnc` той платформы.
5. Событие `homework_synced` в `user_events` с `{"added":…, "updated":…}`.
6. Ответ: `SyncResponseDTO(sources, added, updated)`.

Всё в одной транзакции, кроме вызова `collector` (он до неё, чтобы не держать соединение с базой минуту).

Дополнительно:
- лимит 1 sync в минуту на ученика → 429 с `Retry-After: 60`;
- нет подключений → 200 с пустым `sources` (фронт покажет «подключите платформу»);
- `@EnableScheduling` + `HomeworkSyncScheduler`: раз в 30 минут sync для учеников с `last_login_at` за 7 дней, у кого есть подключения.
  Ошибку одного ученика логировать и идти дальше. Выключается `SYNC_SCHEDULE_ENABLED=false`.

Тесты: `HomeworkSyncServiceTest` (замоканный клиент): токены обновились и сохранены зашифрованными; `needsReconnect` выставлен;
лимит. Интеграционный с `TEST_DB_URL`: после sync задания в `assignments`, повторный sync не дублирует.

Коммиты: `feat(homework): POST /homework/sync`, `feat(homework): scheduled sync every 30 minutes`.

## Шаг 5. Документация (30 мин)

- `docs/API.md`: строки #35–#43 (`#43` = `POST /homework/sync`), раздел «Платформы», 422 в таблице ошибок.
- `database/ENTITIES.md`: в таблице эндпоинтов платформы и sync ✅.
- `backend/.env.example`: `CREDENTIALS_KEY`, `COLLECTOR_URL`, `COLLECTOR_KEY`, `CANVAS_HOSTS`, `SYNC_SCHEDULE_ENABLED`.

Сообщите №1, когда API.md обновлён: фронт сверит типы.

## Шаг 6. Деплой (1 ч)

Выбран Railway: [DEPLOY-RAILWAY.md](DEPLOY-RAILWAY.md), раздел 3. Ниже то же коротко.

Railway или Render, Java 21, `./mvnw package -DskipTests` → `java -jar target/lekseradar-backend-0.1.0.jar`.

| Переменная | Откуда |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | от №3 после деплоя базы |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `CREDENTIALS_KEY` | `openssl rand -base64 32`, **не терять**: без него все подключения придётся делать заново |
| `OPENAI_BASE_URL`, `OPENAI_API_KEY`, `OPENAI_MODEL` | Groq |
| `COLLECTOR_URL`, `COLLECTOR_KEY` | от №3, `collector` только во внутренней сети |
| `CORS_ORIGINS` | адрес фронта на Vercel от №1 |

Проверка: `GET /actuator/health` → `UP`, Swagger открывается, вход работает с фронта на Vercel.

## Шаг 7. Перенос

Код `backend/` из `main-test-3.0` → ветка `JavaAPI-setup` (PR #2).

## Готово, когда

- [ ] `./mvnw package` зелёный, новые тесты проходят.
- [ ] В `platform_connections.credentials_enc` нет токенов открытым текстом (`SELECT encode(credentials_enc,'escape')` → мусор).
- [ ] Canvas подключается через Swagger, после этого `GET /homework` возвращает настоящие ДЗ.
- [ ] Повторный `/homework/sync` не дублирует задания и не сбрасывает «сдано».
- [ ] Ни один эндпоинт платформ не отвечает 401, ошибки платформы → 422.
- [ ] API.md обновлён, бэкенд задеплоен.
