# Фронт: на сайте нет формы для ключей платформ

## Проблема

В `main-test-3.0` ученик не может подключить Canvas, Teams, itslearning или InSchool.
На `/settings/platforms` стоит заглушка: `Settings` в `src/pages/Other.tsx`, строка 18.
Поля там `disabled`, кнопка «подключить позже». Запрос на бэкенд не уходит, поэтому ДЗ в базу не попадают.

Рабочая форма уже есть в ветке `Frontend-setup`, но в `main-test-3.0` её не перенесли.

## Что перенести из `Frontend-setup` в `main-test-3.0`

1. `src/components/Platforms.tsx` — компонент `PlatformsCard` с формой и `toCredentials()`.
2. `src/api/endpoints.ts` — `platformsApi` (`list`, `connect`, `disconnect`).
3. `src/api/types.ts` — `PlatformKey`, `PlatformCredentials`, `PlatformConnection`.
4. Тексты `platforms: { … }` в `src/i18n/nb.ts`, `en.ts`, `ru.ts`.
5. В `src/pages/Other.tsx` заменить заглушку (строка 18) на `<PlatformsCard/>`, как сделано в `Frontend-setup`.

## Бэкенд уже готов

Адреса и тела запросов совпадают с тем, что шлёт `PlatformsCard`, ничего менять не нужно:

| Запрос | Тело |
|---|---|
| `GET /api/v1/platforms` | — |
| `POST /api/v1/platforms/canvas` | `{ "base_url": "https://skole.instructure.com", "token": "…" }` |
| `POST /api/v1/platforms/inschool` | `{ "host": "skole.inschool.visma.no", "cookies": { "JSESSIONID": "…" } }` |
| `POST /api/v1/platforms/teams` | `{ "access_token": "…", "refresh_token": "…" }` |
| `POST /api/v1/platforms/itslearning` | `{ "base_url": "https://sms.itslearning.com", "refresh_token": "…" }` |
| `DELETE /api/v1/platforms/{platform}` | — |

После `POST` сервер проверяет ключ на самой платформе и сохраняет его зашифрованным.
Задания он сразу записывает в базу, и они появляются в `GET /api/v1/homework`.

Ошибки приходят в `message`, форма их уже показывает:
- `400` — неверные данные или адрес;
- `422` — платформа не приняла ключ (это не выход из аккаунта);
- `429` — слишком часто;
- `503` — сервис сбора ДЗ не запущен.

Платформы `ics` на бэкенде нет, на неё придёт `400`. Её лучше убрать из списка.

## Как проверить

1. `cd database && docker compose up -d` — база и сервис сбора ДЗ.
2. Запустить бэкенд и фронт, войти на сайт.
3. «Настройки» → «Платформы» → Canvas: вписать адрес школы и токен (Canvas → Account → Settings → New Access Token).
4. На странице ДЗ появятся задания из Canvas.
