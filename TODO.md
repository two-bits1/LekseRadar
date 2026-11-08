# Что осталось до показа (main-test-4.0)

Код готов, остальное — настройка окружения. Результаты проверки: [README-TEST.md](README-TEST.md).

## Срочно

- [ ] **№2**: отозвать пароль приложения Gmail, вписанный в `backend/.env.example`
      (https://myaccount.google.com/apppasswords), в файле оставить `MAIL_PASSWORD=`.

## Railway (база, collector, бэкенд)

Проект `distinguished-radiance`, бэкенд `https://backend-production-22b1.up.railway.app`.

- [x] **№3**: V22 на облачной базе (сделано 7 ноября, 20:22). Postgres → Networking → Add Public Access (порт 5432) →
      `python3 database/setup_cloud_db.py` → удалить Public Access. Если `ConnectionTimeout`, попробовать другую сеть Wi-Fi.
- [ ] **№2**: переменные бэкенда:
  - `MAIL_USERNAME`, `MAIL_PASSWORD`: без них регистрация отвечает 503;
  - `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`: без них Classroom отвечает 503. В Google Cloud Console добавить
    redirect URI `https://lekseradar-eight.vercel.app/settings/platforms`;
  - `CORS_ORIGINS=https://lekseradar-eight.vercel.app,http://localhost:5173`;
- [ ] **Чтобы исправления из main-test-4.0 попали на сайт** (квизы, карточки, чат, профиль, вход):
  1. Railway → `backend` → Settings → Source → Branch: `main-test-4.0` (Root Directory `backend` тот же);
  2. Railway → `collector` → Branch: `main-test-4.0` (Root Directory `homework-collector`);
  3. **№3**: V23–V26 и новый промпт квиза на облачную базу (V25 — напоминания на почту): `git switch main-test-4.0 && git pull`, временно Public Access,
     `python3 database/setup_cloud_db.py`, Public Access убрать;
  4. Vercel → Settings → Git → Production Branch: `main-test-4.0` → Redeploy.
- [ ] По желанию: `TEAMS_CLIENT_ID` у collector (вход в Teams по коду).

## Vercel (фронт), №1

- [x] Сайт: https://lekseradar-eight.vercel.app (7 ноября, 20:23). Проект из `two-bits1/LekseRadar`, Root Directory `frontend`, Production Branch `Frontend-setup` (или `main-test-4.0`).
- [ ] `VITE_API_URL=https://backend-production-22b1.up.railway.app/api/v1`, Redeploy.
- [ ] Адрес Vercel передать №2 для redirect URI Google.

## Проверка после деплоя

- [ ] Сайт: регистрация → письмо с кодом → вход.
- [ ] Платформы: Canvas (токен), Google Classroom (вход Google) → «Lekser» показывает задания, «Oppdater» без дублей.
- [ ] ИИ (№3): `python3 ai-content/check/check_ai.py --api https://backend-production-22b1.up.railway.app/api/v1 --email … --password …`.

## Желательно

- [ ] **№3**: учебные материалы в `ai-content/materials/<Предмет>/<Тема>/` и загрузка через `setup_cloud_db.py`.
- [ ] **№2**: `GET /achievements` для профиля (`frontend/docs/backend-changes.md`, раздел 3).
- [ ] **№1**: разбить бандл фронта (`import()` для страниц).
- [ ] Все: убрать упоминания `main-test-3.0` из документов после выбора финальной ветки.
