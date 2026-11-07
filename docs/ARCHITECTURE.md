# LekseRadar: как всё работает

Документ для всей команды: кто что делает, как данные ходят между частями и почему сделано именно так.
Подробности по базе: [`database/README.md`](../database/README.md), по сбору ДЗ: [`homework-collector/README.md`](../homework-collector/README.md).

## 1. Общая схема

```mermaid
flowchart LR
    subgraph FE["Frontend · React + Tailwind (№1)"]
        UI["Экраны: Дашборд · Домашка · ИИ-чат<br/>Квиз · Флеш-карты · Прогресс · Настройки платформ"]
    end

    subgraph BE["Backend · Java Spring Boot (№2)"]
        API["REST /api/*<br/>JWT-вход"]
        AI["AiClient<br/>(промпты, вызов модели)"]
    end

    subgraph DB["PostgreSQL 16"]
        T["таблицы + функции<br/>import_quiz_json · start_quiz · save_quiz_answer<br/>finish_quiz · review_flashcard · виды прогресса"]
    end

    subgraph PY["Python (№3)"]
        HC["homework-collector<br/>collect()"]
    end

    LLM["Модель ИИ<br/>(API-ключ)"]
    PL["Canvas · Teams · itslearning · Visma InSchool"]

    UI -- "HTTPS + JWT, JSON" --> API
    API -- "SQL / вызов функций" --> T
    API --> AI
    AI -- "промпт → ответ / JSON квиза" --> LLM
    API -- "учётные данные платформ" --> HC
    HC -- "API платформ" --> PL
    HC -- "список ДЗ в одном формате" --> API
```

**Главное правило:** фронт разговаривает только с бэкендом. Бэкенд единственный, кто пишет в базу,
зовёт модель ИИ и сервис сбора ДЗ. Python ничего не хранит.

## 2. Кто за что отвечает

| Часть | Владелец | Отвечает за | Не делает |
|-------|----------|-------------|-----------|
| Frontend | №1 | экраны, состояние интерфейса, вызовы `/api/*` | не ходит в базу, ИИ и платформы напрямую |
| Backend | №2 | вход (JWT), REST API, вызов ИИ, шифрование токенов платформ, вызов функций БД | не дублирует логику квизов/карточек: она в функциях БД |
| База | №2 + №3 | схема, миграции Flyway, функции квизов, карточек и прогресса | — |
| Python | №3 | `collect()` сбор ДЗ с 4 платформ, промпты и формат JSON квиза | не хранит данные |

## 3. Данные коротко

```mermaid
erDiagram
    users ||--o{ assignments : "ДЗ с платформ"
    users ||--o{ platform_connections : "подключённые платформы"
    users ||--o{ chat_sessions : "чаты"
    chat_sessions ||--o{ chat_messages : "история чата"
    subjects ||--o{ quiz_sets : "предмет"
    quiz_sets ||--o{ exercises : "вопросы (один JSON от ИИ)"
    users ||--o{ quizzes : "попытки квиза"
    quizzes ||--o{ exercise_attempts : "ответы"
    users ||--o{ flashcard_progress : "колода"
    exercises ||--o{ flashcard_progress : "карточка = вопрос"
    users ||--o{ user_events : "история действий"
    users ||--o{ reminders : "напоминания"
    users ||--o{ user_achievements : "ачивки"
```
