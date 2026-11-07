# Статус Railway (7 ноября 2026, 21:25)

**Railway собран, но сайт пока не работает целиком: нужна дополнительная настройка.**

Проект Railway `distinguished-radiance`, бэкенд `https://backend-production-22b1.up.railway.app`,
фронт `https://lekseradar-eight.vercel.app`.

## Что собрано

| Сервис | Где | Состояние |
|---|---|---|
| Postgres | Railway | V1–V22 и 3 промпта загружены |
| collector | Railway, ветка `main-test-4.0` (`homework-collector`) | Online, связь с бэкендом есть |
| backend | Railway, ветка `main-test-4.0` (`backend`), коммит `747692d` | `/actuator/health` → `UP` |
| фронт | Vercel, Root Directory `frontend` | сайт открывается |

## Почему пока не работает

| Что | Причина | Что сделать | Кто |
|---|---|---|---|
| Регистрация и напоминания на почту | у бэкенда нет `MAIL_USERNAME` / `MAIL_PASSWORD`, письма с кодом и напоминания не отправляются (503) | задать их в Railway → backend → Variables; для показа можно `MAIL_DEV_LOG_CODES=true` (код в Deploy Logs) | №2 |
| Исправления квизов и карточек | на облачной базе нет V23–V25 (V25 — напоминания на почту) и нового промпта квиза | `git switch main-test-4.0`, `git pull`, временно Public Access у Postgres, `python3 database/setup_cloud_db.py`, Public Access убрать | №3 |
| Исправления фронта (чат, лого, профиль, карточки) | Vercel собирает Production не из `main-test-4.0` | Vercel → Settings → Git → Production Branch: `main-test-4.0` → Redeploy | №1 |
| Google Classroom | нет `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` (503) | задать их; в Google Cloud Console redirect URI `https://lekseradar-eight.vercel.app/settings/platforms` | №2 |
| Безопасность | в `backend/.env.example` был вписан пароль приложения Gmail | отозвать его и оставить `MAIL_PASSWORD=` | №2 |

По желанию: `TEAMS_CLIENT_ID` у collector (вход в Teams по коду).

## Проверка после настройки

1. Сайт: регистрация → код → вход.
2. Квиз: у вопроса нет пустых вариантов; где верных несколько, можно выбрать несколько.
3. Карточки: после занятия «выучено» больше 0.
4. Платформы: Canvas (токен), Google Classroom → «Lekser» показывает задания.
5. ИИ: `python3 ai-content/check/check_ai.py --api https://backend-production-22b1.up.railway.app/api/v1 --email … --password …`.

Из песочницы Claude Railway и Vercel недоступны, поэтому живой сайт проверяется у себя.
Подробно: [TODO.md](TODO.md), [README-TEST.md](README-TEST.md).
