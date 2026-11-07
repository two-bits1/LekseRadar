-- Квизы от ИИ: предметы и сохранённые наборы вопросов.
-- ИИ генерирует квиз по (предмет, тема, уровень, язык) один раз и отдаёт JSON (формат: quiz.schema.json).
-- JSON импортируется в quiz_sets + exercises; потом тот же набор берётся из базы, без новой генерации.

CREATE TABLE subjects (
    id          BIGSERIAL PRIMARY KEY,
    name        TEXT NOT NULL,
    key         TEXT NOT NULL UNIQUE,     -- нормализованное имя: lower, без лишних пробелов
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Нормализация для поиска: 'Lineære  Funksjoner ' → 'lineære funksjoner'
CREATE FUNCTION norm_key(t TEXT) RETURNS TEXT LANGUAGE sql IMMUTABLE AS $$
    SELECT lower(regexp_replace(btrim(t), '\s+', ' ', 'g'))
$$;

CREATE TABLE quiz_sets (
    id             BIGSERIAL PRIMARY KEY,
    subject_id     BIGINT         NOT NULL REFERENCES subjects(id),
    topic          TEXT           NOT NULL CHECK (length(topic) BETWEEN 1 AND 100),
    topic_key      TEXT           NOT NULL,
    level          exercise_level NOT NULL,
    language       TEXT           NOT NULL DEFAULT 'nb',
    title          TEXT           NOT NULL,
    question_count INT            NOT NULL,
    source_json    JSONB          NOT NULL,           -- исходный JSON от ИИ как есть
    content_hash   TEXT           NOT NULL UNIQUE,    -- md5 вопросов: один и тот же JSON не импортируется дважды
    model          TEXT,                              -- какая модель сгенерировала
    created_by     BIGINT         REFERENCES users(id) ON DELETE SET NULL,
    times_used     INT            NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX quiz_sets_lookup ON quiz_sets (subject_id, topic_key, level, language, created_at DESC);

-- Вопросы набора хранятся в exercises (те же поля, что ждёт проверка ответов)
ALTER TABLE exercises
    ADD COLUMN subject_id    BIGINT REFERENCES subjects(id),
    ADD COLUMN quiz_set_id   BIGINT REFERENCES quiz_sets(id) ON DELETE CASCADE,
    ADD COLUMN position      INT,
    ADD COLUMN question_type TEXT NOT NULL DEFAULT 'text'
        CHECK (question_type IN ('single_choice', 'multiple_choice', 'text')),
    ADD COLUMN explanation   TEXT,                -- пояснение к правильному ответу
    ADD CONSTRAINT exercises_set_position UNIQUE (quiz_set_id, position);

-- Попытка квиза ссылается на набор; current_position — где пользователь остановился
ALTER TABLE quizzes
    ADD COLUMN subject_id       BIGINT REFERENCES subjects(id),
    ADD COLUMN quiz_set_id      BIGINT REFERENCES quiz_sets(id),
    ADD COLUMN current_position INT NOT NULL DEFAULT 1;
CREATE INDEX quizzes_user_set ON quizzes (user_id, quiz_set_id);
