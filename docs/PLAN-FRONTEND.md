# План фронта (№1): страница платформ и обновление ДЗ

Часть общего плана [PLAN-MVP.md](../PLAN-MVP.md). Итог: ученик сам подключает Canvas, Teams, itslearning или InSchool
на `/settings/platforms`, а на `/homework` нажимает «Oppdater» и видит свежие ДЗ. Остальные страницы уже на API.

Общее время около 5 часов. Работайте в `main-test-3.0`, один коммит на шаг, сообщения коммитов на английском.
Перед каждым коммитом `pnpm test && pnpm build`.

**Начинать можно сразу**: контракт API ниже, бэкенд (№2) делает его параллельно. Пока его нет, проверяйте на моках
в тестах. Подробности бэкенда: [PLAN-BACKEND.md](PLAN-BACKEND.md), шаг 3.

## Что уже есть

| Есть | Где |
|---|---|
| Маршрут `PATHS.platforms` = `/settings/platforms` → `<Settings platformsOnly/>` (сейчас заглушка с `disabled`) | `main.tsx`, `pages/Other.tsx` |
| `api()` в `api/client.ts`: на 204 возвращает `undefined`; **на 401 разлогинивает** | |
| `asApiError(e, t.common)` и тексты по статусу `t.common.byStatus` | `forms.ts`, `i18n/*` |
| `homeworkApi`, страница `/homework`, ссылка «Koble til plattform» при пустом списке | `pages/Homework.tsx` |
| Словари nb/en/ru, `Dict = typeof nb` (en и ru не соберутся, если забыть ключ) | `i18n/` |
| Стили `.lesson-card`, `.settings-row`, `.field`, `.pill`, `.feedback`, `.btn` | `styles.css` |

## Контракт API

```ts
// types.ts
export type PlatformStatus = { platform: Platform; connected: boolean; needsReconnect: boolean;
  lastSyncedAt: string | null; lastError: string | null };
export type CanvasConnect = { baseUrl: string; token: string };
export type InschoolConnect = { host: string; cookies: Record<string, string> };
export type TeamsStart = { userCode: string; verificationUri: string; deviceCode: string; interval: number; expiresIn: number };
export type TeamsPoll = { status: 'pending' } | { status: 'connected'; platform: PlatformStatus };
export type SourceStatus = { platform: Platform; ok: boolean; count: number; error: string | null; needsReconnect: boolean };
export type SyncResult = { sources: SourceStatus[]; added: number; updated: number };
```

```ts
// endpoints.ts
export const platformsApi = {
  list: () => api<PlatformStatus[]>('/platforms'),                                                         // #35
  canvas: (b: CanvasConnect) => api<PlatformStatus>('/platforms/canvas', json('PUT', b)),                  // #36
  inschool: (b: InschoolConnect) => api<PlatformStatus>('/platforms/inschool', json('PUT', b)),            // #37
  teamsStart: () => api<TeamsStart>('/platforms/teams/start', json('POST')),                               // #38
  teamsPoll: (deviceCode: string) => api<TeamsPoll>('/platforms/teams/poll', json('POST', { deviceCode })), // #39
  itslearningUrl: (baseUrl: string) => api<{ url: string }>(`/platforms/itslearning/url?baseUrl=${encodeURIComponent(baseUrl)}`), // #40
  itslearning: (baseUrl: string, codeOrUrl: string) => api<PlatformStatus>('/platforms/itslearning', json('POST', { baseUrl, codeOrUrl })), // #41
  remove: (p: Platform) => api<void>(`/platforms/${p}`, { method: 'DELETE' }),                             // #42
};
// в homeworkApi:
  sync: () => api<SyncResult>('/homework/sync', json('POST')),                                             // #43
```

Ошибки: 400 неверный адрес или поле, **422 платформа не приняла доступ** (в `message` текст от платформы),
429 слишком часто, 503 сервис сбора недоступен. 401 эти эндпоинты не отдают.

## Шаг 1. Типы, клиент, тесты контракта (1 ч)

- `types.ts`, `endpoints.ts` как выше.
- `api.test.ts`: кейсы #35–#43 (метод, путь, тело), как для #30–#34.
- `i18n`: в `common.byStatus` добавить `422` и `503`:
  - nb: `422: 'Plattformen godtok ikke tilgangen. Sjekk og prøv igjen.'`, `503: 'Henting av lekser er nede akkurat nå.'`
  - en и ru: то же по смыслу.

  Сейчас `503` = «ИИ не настроен»: оставить как есть, а для платформ показывать `error.message` из ответа или новый ключ `t.plat.down`.

Коммит: `feat(api): platforms and homework sync client (#35-#43)`.

## Шаг 2. Страница `/settings/platforms` (3 ч)

Новый файл `pages/Platforms.tsx`, в `main.tsx` маршрут `PATHS.platforms` → `<Platforms/>`.
Из `Other.tsx` убрать блок платформ (на `/settings` оставить ссылку «Plattformer» → `PATHS.platforms`). ICS из списка убрать: бэкенд его не умеет.

При открытии `platformsApi.list()` → 4 карточки `.lesson-card`:

| Состояние | Что видно |
|---|---|
| не подключена | название, одна строка «что это», кнопка «Koble til» → раскрывает форму |
| подключена | `pill` «Tilkoblet», «Sist oppdatert {время}» (формат как `fmt` в `Homework.tsx`, `timeZone: user.timezone`), «Koble fra» |
| `needsReconnect` | `pill` «Må kobles til på nytt» + `lastError` + та же форма подключения |
| `lastError` без reconnect | `feedback danger` с текстом, кнопка «Prøv igjen» = `homeworkApi.sync()` |

Формы по платформам:

**Canvas**
- Поля «Canvas-adresse» (`https://…instructure.com`, подсказка: адрес, с которого вы входите в Canvas) и «Tilgangstoken» (`type="password"`).
- Подсказка со шагами: Konto → Innstillinger → «+ Nytt tilgangstoken».
- Отправка → `platformsApi.canvas` → заменить карточку ответом, показать «Koblet til. Fant N lekser» (N потом из sync, можно без числа).

**Microsoft Teams**
- Кнопка «Koble til» → `teamsStart()` → крупно `userCode` (кнопка «Kopier»), ссылка `verificationUri` (`target="_blank" rel="noopener noreferrer"`).
- Опрос `teamsPoll(deviceCode)` каждые `interval` секунд (`setTimeout`, не `setInterval`), пока `pending`, максимум `expiresIn`.
- `connected` → обновить карточку. Истекло → «Koden gikk ut» + «Prøv igjen». Уход со страницы → остановить опрос (cleanup в `useEffect`).

**itslearning**
- Поле «Skolens itslearning-adresse» → «Logg inn» → `itslearningUrl` → открыть в новой вкладке.
- Текст: «Etter innlogging prøver nettleseren å åpne itsl-itslearning://… Kopier den adressen og lim den inn her.»
- Поле для адреса + «Fullfør» → `platformsApi.itslearning`.
- Пометка `pill` «Uoffisiell»: может перестать работать.

**Visma InSchool**
- Поля «Skoleadresse» (`skole.inschool.visma.no`) и «Cookies» (textarea, формат `JSESSIONID=…; Authorization=…`, разобрать в объект по `;` и `=`).
- Подсказка: DevTools → Application → Cookies. Предупреждение: сессия короткая, её придётся обновлять.

**Общее**
- «Koble fra» → `confirm()` нельзя (в e2e блокирует): вторая кнопка «Ja, koble fra» в той же строке → `remove` → карточка «не подключена».
- Во время запроса кнопки `disabled`, текст «Kobler til …».
- Ошибки через `asApiError`, под формой этой платформы, не наверху страницы.
- Ширина 390 px без горизонтальной прокрутки.
- Секреты не сохранять в `localStorage` и не писать в `console`.

i18n: новый раздел `plat` в nb/en/ru (все тексты выше). Тест `platforms.test.ts`: разбор строки cookies в объект и обратно; выбор состояния карточки.

Коммиты: `feat(platforms): page with Canvas and InSchool`, `feat(platforms): Teams device login`, `feat(platforms): itslearning login`.

## Шаг 3. «Oppdater» на `/homework` (1 ч)

- В шапке `/homework` кнопка «Oppdater» (иконка `RefreshCw`) → `homeworkApi.sync()` → затем `homeworkApi.list()`.
- Во время запроса «Henter lekser …» и `disabled` (sync может идти до минуты).
- После: «N nye, M oppdatert». По каждой `sources[i]` с `ok:false`: строка `feedback danger` с `error`, при `needsReconnect` ссылка «Koble til på nytt» → `PATHS.platforms`.
- 429 → текст из `byStatus` (уже есть).
- Пустой ответ `sources: []` → «Ingen plattformer er koblet til» + ссылка на платформы.
- На главной (`Dashboard.tsx`) ничего не менять: она берёт ДЗ из `/dashboard`.

Коммит: `feat(homework): refresh button with per-platform errors`.

## Шаг 4. Деплой на Vercel (30 мин)

- Root directory `frontend`, build `pnpm build`, output `dist`.
- `VITE_API_URL` = адрес бэкенда от №2 (проверить, что `api/client.ts` его читает; сейчас по умолчанию `/api/v1` через proxy).
- Rewrites для SPA: всё → `/index.html` (`vercel.json`), иначе `/homework` по прямой ссылке даст 404.
- Отдать адрес №2 для `CORS_ORIGINS`.

## Шаг 5. Перенос

Код `frontend/` из `main-test-3.0` → ветка `Frontend-setup`.

## Готово, когда

- [ ] `pnpm test` и `pnpm build` зелёные, контрактные тесты #35–#43 есть.
- [ ] На `/settings/platforms` подключается Canvas с настоящим токеном, карточка «Tilkoblet», на `/homework` появились ДЗ.
- [ ] Teams: код показан, после входа карточка сама стала «Tilkoblet».
- [ ] Неверный токен → понятная ошибка под формой, ученика не выкинуло на вход.
- [ ] «Oppdater» показывает ошибки по платформам и ведёт на повторное подключение.
- [ ] Все тексты на nb/en/ru, 390 px без прокрутки, в консоли нет ошибок.
- [ ] Сайт открыт на Vercel и ходит в задеплоенный бэкенд.
