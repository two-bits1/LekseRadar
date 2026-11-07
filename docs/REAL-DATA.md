# Настоящие данные ученика: инструкция

Как загрузить в LekseRadar настоящие ДЗ ученика с его платформ уже сейчас, пока в бэкенде нет `POST /homework/sync`
и страницы подключения платформ (это этапы 1–5 в [PLAN-MVP.md](https://github.com/two-bits1/LekseRadar/blob/main-test-3.0/PLAN-MVP.md)). Всё делается из консоли за 10–15 минут.

Что получится: ученик входит на сайт под своим email и видит свои настоящие задания на «Lekser», на главной и в прогрессе,
а из любого задания открывает чат с ИИ или квиз.

## 0. Что нужно

- Docker и Python 3.11+.
- Доступ ученика хотя бы к одной платформе: Canvas, Teams, itslearning или Visma InSchool.
- Ключ ИИ (Groq бесплатно, https://console.groq.com/keys), если нужен чат и квизы от ИИ.

## 1. База без тестовых данных

```bash
cd database
docker compose up -d          # миграции V1–V20 и промпты, без тестового ученика
```

Если раньше запускали с `--profile testdata`, тестового ученика можно убрать (его ДЗ, чаты и квизы удалятся каскадом):

```bash
psql postgresql://lekseradar:lekseradar@127.0.0.1:5432/lekseradar -c "DELETE FROM users WHERE email = 'test@lekseradar.no'"
```

## 2. Бэкенд и фронт

```bash
cd backend && cp .env.example .env    # OPENAI_API_KEY (Groq) и JWT_SECRET
./mvnw spring-boot:run
cd ../frontend && pnpm install && pnpm dev
```

Откройте http://localhost:5173 и **зарегистрируйте ученика** на сайте. Язык интерфейса, выбранный в профиле,
станет языком ответов ИИ.

## 3. Доступы к платформам: файл `creds.json`

Создайте `homework-collector/creds.json`. Файлы `creds*.json` в git не попадают (`.gitignore`), но это пароли:
не пересылайте их в чат и не коммитьте. Оставьте в файле только те платформы, которые есть у ученика.

```json
{
 "canvas": {"base_url": "https://canvas.instructure.com", "token": "…"},
 "inschool": {"host": "ohg-vgs.inschool.visma.no", "cookies": {"JSESSIONID": "…", "Authorization": "…"}}
}
```

Teams и itslearning дописываются командой (шаги ниже), руками их вписывать не нужно.

```bash
cd homework-collector
pip install -r requirements.txt
```

### Canvas (официальный API)

1. Войдите в Canvas → **Account → Settings → + New Access Token** → назначение «LekseRadar» → **Generate Token**.
2. Скопируйте токен в `canvas.token`, в `canvas.base_url` адрес вашего Canvas (например `https://uio.instructure.com`).

Токен живёт, пока его не удалят. Это самый надёжный вариант.

### Microsoft Teams (официальный API Graph)

Нужно приложение в Microsoft Entra ID, один раз на всю команду:
- **App registrations → New registration**, «Accounts in any organizational directory»;
- **Authentication → Allow public client flows: Yes**;
- **API permissions → Microsoft Graph → Delegated**: `EduAssignments.ReadBasic`, `Team.ReadBasic.All`, `User.Read`.

Затем:

```bash
TEAMS_CLIENT_ID=<Application (client) ID> python -m homework.login teams --creds creds.json
```

Команда покажет код: откройте https://microsoft.com/devicelogin, введите его и войдите школьным аккаунтом.
Токены запишутся в `creds.json` и дальше обновляются сами. Если школа запрещает ученикам давать согласие приложениям,
вход закончится ошибкой: тогда нужен администратор школы.

### itslearning (неофициально, через клиент мобильного приложения)

```bash
python -m homework.login itslearning --creds creds.json --base-url https://<школа>.itslearning.com
```

1. Откройте показанную ссылку и войдите через Feide.
2. В конце браузер попытается открыть `itsl-itslearning://login?code=…` и не сможет. Скопируйте этот адрес
   (DevTools → Network или строка ошибки) и вставьте в консоль.

refresh_token обновляется сам. itslearning может закрыть этот путь в любой момент.

### Visma InSchool (неофициально, cookie браузера)

1. Войдите в InSchool через ID-porten, например `https://ohg-vgs.inschool.visma.no`.
2. DevTools → **Application → Cookies** → ваш адрес InSchool. Скопируйте значения `JSESSIONID` и `Authorization`
   (можно все cookie) в `inschool.cookies`, адрес без `https://` в `inschool.host`.
3. **Сразу** выполните шаг 4: cookie `Authorization` живёт около 5 минут. Каждая синхронизация записывает новые
   cookie обратно в `creds.json`, так что при запуске раз в 3–4 минуты сессия держится. Если пропустили, войдите заново.

## 4. Проверить и загрузить

```bash
python -m homework --creds creds.json                     # просто показать, что нашлось
DATABASE_URL=postgresql://lekseradar:lekseradar@127.0.0.1:5432/lekseradar \
  python -m homework.to_db --email <email ученика> --creds creds.json
```

`to_db` вызывает `collect()`, записывает задания в `assignments` (UPSERT по платформе и id задания) и сохраняет
обновлённые токены в `creds.json`. Ответ выглядит так:

```
canvas: 12 шт.
inschool: ОШИБКА Сессия InSchool истекла, нужно войти заново (нужно войти заново)
В базе: 12 новых, 0 обновлено.
```

Ошибка одной платформы не мешает остальным. Если ученик отметил задание как сданное на сайте, повторная загрузка
не вернёт его в «å gjøre». Обновите `/homework` в браузере: задания на месте.

Код выхода: `0` всё хорошо, `1` нет такого ученика (сначала регистрация), `2` часть платформ с ошибкой.

## 5. Обновлять автоматически

До появления `POST /homework/sync` можно запускать по расписанию (`crontab -e`):

```cron
*/30 * * * * cd /путь/к/LekseRadar/homework-collector && DATABASE_URL=postgresql://lekseradar:lekseradar@127.0.0.1:5432/lekseradar python3 -m homework.to_db --email <email> --creds creds.json >> sync.log 2>&1
```

Для InSchool ставьте `*/4` вместо `*/30`, иначе cookie успеют истечь.

## Если что-то не так

| Сообщение | Что делать |
|---|---|
| `Нет ученика …` | Зарегистрируйтесь на сайте этим email |
| `нужно войти заново` | Повторите вход для этой платформы (шаг 3) |
| `Не задан client_id приложения Entra` | Передайте `TEAMS_CLIENT_ID` или `--client-id` |
| `сеть или сервер недоступны` | Проверьте `base_url`/`host` и интернет |
| На сайте пусто | Проверьте, что email в `--email` тот же, под которым вошли на сайт |
