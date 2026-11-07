-- Напоминания, флеш-карты и ачивки (функции 5–7 из README, «ситуативно»).

-- Напоминания о дедлайнах ДЗ или о своём событии
CREATE TABLE reminders (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assignment_id  BIGINT      REFERENCES assignments(id) ON DELETE CASCADE,
    message        TEXT        NOT NULL,
    remind_at      TIMESTAMPTZ NOT NULL,
    channel        TEXT        NOT NULL DEFAULT 'in_app',   -- in_app | email | push
    sent_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Очередь к отправке: SELECT * FROM reminders WHERE sent_at IS NULL AND remind_at <= now();
CREATE INDEX reminders_due ON reminders (remind_at) WHERE sent_at IS NULL;

-- Флеш-карты с интервальным повторением (SM-2 упрощённо)
CREATE TABLE flashcards (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assignment_id   BIGINT       REFERENCES assignments(id) ON DELETE SET NULL,
    topic           TEXT         NOT NULL,
    front           TEXT         NOT NULL,
    back            TEXT         NOT NULL,
    ease            NUMERIC(4,2) NOT NULL DEFAULT 2.5,
    interval_days   INT          NOT NULL DEFAULT 0,
    next_review_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reviews         INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX flashcards_review ON flashcards (user_id, next_review_at);

-- Каталог ачивок и полученные ачивки
CREATE TABLE achievements (
    code         TEXT PRIMARY KEY,           -- например 'first_quiz'
    title        TEXT NOT NULL,
    description  TEXT NOT NULL,
    icon         TEXT
);

CREATE TABLE user_achievements (
    user_id           BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_code  TEXT        NOT NULL REFERENCES achievements(code),
    unlocked_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, achievement_code)
);

INSERT INTO achievements (code, title, description, icon) VALUES
    ('first_chat',     'Første spørsmål',  'Задал первый вопрос ИИ-тьютору',             'chat'),
    ('first_quiz',     'Første quiz',      'Прошёл первый квиз',                          'quiz'),
    ('perfect_quiz',   'Full pott',        'Квиз на 100%',                                'star'),
    ('streak_7',       '7 dager på rad',   'Занимался 7 дней подряд',                     'fire'),
    ('homework_zero',  'Alt levert',       'Нет просроченных ДЗ',                         'check'),
    ('connected_all',  'Radar på',         'Подключил хотя бы одну учебную платформу',    'radar');
