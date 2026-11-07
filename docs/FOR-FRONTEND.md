# Фронтенд: что изменить, чтобы всё работало и можно было заливать

Сверено с `Frontend-setup` (04d9a52) и бэкендом `JavaAPI-setup`. Проверено вживую через сайт из `Frontend-setup`:
регистрация с кодом, платформы, «Oppdater» и **Google Classroom** работают. Вход через Google подключил
28 заданий за ~6 с. Ниже то, что ещё нужно поправить.

| # | Что | Обязательно |
|---|---|---|
| 1 | Ошибка 400 у Classroom показывается как «ещё не поддерживается» | да |
| 2 | `classroom` нет в `PLATFORM` и в типе `Assignment.source` | да |
| 3 | «Kobler til…» после возврата от Google (~6 с тишины) | желательно |
| 4 | Перенести фронт в `main-test-3.0` (там страница платформ — заглушка) | да, для общей ветки |
| 5 | `frontend/docs/backend-changes.md` устарел | да |
| 6 | Vercel: переменные и домен | при деплое |

---

## 1. Ошибки Classroom (`src/components/Platforms.tsx`)

Сейчас любые `400 / 404 / 405 / 501` превращаются в «Google Classroom er klart her, men serveren støtter det ikke ennå».
Бэкенд Classroom уже умеет, и `400` теперь означает настоящую ошибку ученика, например «Ссылка для входа устарела…».

```ts
// было: [400, 404, 405, 501]
const classroomError = (err: unknown, t: Dict) =>
  isApiError(err) && [404, 405, 501].includes(err.status) ? t.platforms.classroomNotReady : platformError(err, t);
```

Что отвечает бэкенд (показывать `message`):

| Код | Когда |
|---|---|
| 400 | `redirectUri` не из `CORS_ORIGINS` + `/settings/platforms`; метка устарела (больше 10 минут) или чужая; нет `code` |
| 422 | Google не принял код, или при входе сняли галочки доступа к курсам и заданиям |
| 429 | больше 10 подключений за 10 минут |
| 502 | Google не ответил |
| 503 | на сервере нет ключей Google, нет миграции V22 или collector без Classroom (`message` объясняет) |

## 2. `classroom` в ДЗ

**`src/api/types.ts`**: задания тоже бывают из Classroom.

```ts
export type Assignment = {
  id: number; source: PlatformKey;   // было: Platform (4 значения), а бэкенд отдаёт и 'classroom'
  // …
};
```

**`src/pages/Homework.tsx`, строка 17**: в `PLATFORM` нет `classroom` (в `NAME` он есть). Из-за этого у заданий
Classroom не видно платформы в строке, а «Lag quiz» на задании без курса падает на `undefined.slice`.

```ts
const PLATFORM: Record<Assignment['source'], string> = {
  canvas: 'Canvas', teams: 'Teams', itslearning: 'itslearning', inschool: 'Visma InSchool', classroom: 'Google Classroom',
};
```

Тест в `homework.test.ts`: задание `{ source: 'classroom', course: '' }` → «Lag quiz» берёт `subject: 'Google Classroom'`.

## 3. «Kobler til…» после возврата от Google

После возврата на `/settings/platforms?code=…` подключение идёт около 6 секунд: бэкенд меняет код на ключи, collector
обходит курсы. Всё это время карточка выглядит неподключённой. Если ученик сразу уходит в «Lekser», список
загружается до того, как задания сохранились.

```tsx
const [cBusy, setCBusy] = useState(false);
// в useEffect возврата, перед platformsApi.connect('classroom', …):
setCBusy(true);
platformsApi.connect('classroom', { code: ret.code, state: ret.state, redirect_uri: redirectUri() })
  .then(c => setList(l => [...l.filter(x => x.platform !== 'classroom'), c]))
  .catch(err => setCErr(classroomError(err, t)))
  .finally(() => setCBusy(false));
// в карточке Classroom: пока cBusy — «Kobler til Google Classroom … henter lekser» и кнопки неактивны;
// после успеха — «Tilkoblet» и ссылка «Se lekser» на /homework.
```

## 4. `main-test-3.0`

В общей ветке нет `src/components/Platforms.tsx`: на `/settings/platforms` стоит заглушка с выключенными полями.
Нужно перенести туда фронт из `Frontend-setup` целиком (платформы, «Oppdater», регистрация с кодом, Classroom).
Иначе на общей ветке ученик не сможет подключить ни одну платформу.

## 5. `frontend/docs/backend-changes.md`

- Строка «Google Classroom (#45, #46) — **нет**» → **готово**.
- Раздел 2.2 (Java) и 2.3 (обмен кода и ключи Google в collector) убрать или пометить «сделано иначе».
  Java сама выдаёт ссылку, меняет код и обновляет токен. Collector только собирает задания, ключи Google ему **не нужны**.
- Контракт (#45, #46) остаётся ровно как в документе: бэкенд принимает именно его.

## 6. Vercel

- `VITE_API_URL=https://<адрес-бэкенда-на-railway>/api/v1`.
- Домен должен быть ровно `https://lekseradar.vercel.app`, тот же, что в Google Cloud (Authorized redirect URIs:
  `https://lekseradar.vercel.app/settings/platforms`) и в `CORS_ORIGINS` бэкенда. Если домен другой, сообщить №2:
  его нужно поменять в Google и на Railway.
- Страница возврата работает и при прямом открытии: `vercel.json` уже отдаёт `index.html` на все пути.

## Как проверить

1. `pnpm test`, `pnpm exec tsc -b`, `pnpm build` — зелёные.
2. Локально (бэкенд + база с V22 + collector с Classroom): настройки → «Plattformer» → Google Classroom →
   «Logg inn med Google» → после возврата «Kobler til…», через ~6 с «Tilkoblet».
3. «Lekser»: у заданий подпись «Google Classroom», «Lag quiz» работает и на задании без курса.
4. Открыть возврат ещё раз со старым `?code=…&state=…`: должен быть текст ошибки от бэкенда, а не «ещё не поддерживается».

Вопросы по бэкенду к №2.
