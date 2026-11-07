-- Упражнения (генератор), квизы и ответы студента (проверка ответов).

CREATE TYPE exercise_level AS ENUM ('beginner', 'intermediate', 'advanced');
CREATE TYPE answer_type    AS ENUM ('exact', 'free');
CREATE TYPE verdict        AS ENUM ('correct', 'incorrect', 'partial');
CREATE TYPE quiz_status    AS ENUM ('in_progress', 'completed', 'abandoned');

-- Exercise из Python API (GenerateResponse.exercises)
CREATE TABLE exercises (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT         REFERENCES users(id) ON DELETE CASCADE,  -- NULL = общий каталог
    assignment_id   BIGINT         REFERENCES assignments(id) ON DELETE SET NULL,
    topic           TEXT           NOT NULL CHECK (length(topic) BETWEEN 1 AND 100),
    level           exercise_level NOT NULL,
    statement       TEXT           NOT NULL CHECK (length(statement) BETWEEN 1 AND 2000),
    correct_answer  TEXT           NOT NULL CHECK (length(correct_answer) BETWEEN 1 AND 2000),
    answer_type     answer_type    NOT NULL,
    options         JSONB,          -- варианты ответа для квиза с выбором; NULL = ответ текстом
    generated       BOOLEAN        NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX exercises_topic ON exercises (topic, level);

CREATE TABLE quizzes (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assignment_id    BIGINT         REFERENCES assignments(id) ON DELETE SET NULL,   -- квиз по ДЗ
    chat_session_id  BIGINT         REFERENCES chat_sessions(id) ON DELETE SET NULL, -- квиз из чата
    title            TEXT           NOT NULL,
    topic            TEXT           NOT NULL,
    level            exercise_level NOT NULL,
    status           quiz_status    NOT NULL DEFAULT 'in_progress',
    score            NUMERIC(5,2) CHECK (score BETWEEN 0 AND 100),  -- ставит finish_quiz()
    started_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    completed_at     TIMESTAMPTZ
);
CREATE INDEX quizzes_user ON quizzes (user_id, started_at DESC);

CREATE TABLE quiz_questions (
    quiz_id      BIGINT NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    position     INT    NOT NULL CHECK (position >= 1),
    exercise_id  BIGINT NOT NULL REFERENCES exercises(id),
    PRIMARY KEY (quiz_id, position),
    UNIQUE (quiz_id, exercise_id)
);

-- Каждый ответ студента: в квизе или отдельно (из чата или генератора). verdict/explanation = CheckResult.
CREATE TABLE exercise_attempts (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_id  BIGINT      NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    quiz_id      BIGINT      REFERENCES quizzes(id) ON DELETE CASCADE,
    answer       TEXT        NOT NULL CHECK (length(answer) BETWEEN 1 AND 4000),
    verdict      verdict     NOT NULL,
    explanation  TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX attempts_user ON exercise_attempts (user_id, created_at DESC);
CREATE INDEX attempts_quiz ON exercise_attempts (quiz_id);

-- Завершить квиз: балл = доля верных по последнему ответу на каждый вопрос (partial = 0.5).
-- SELECT finish_quiz(42);
CREATE FUNCTION finish_quiz(p_quiz BIGINT) RETURNS NUMERIC LANGUAGE sql AS $$
    WITH last AS (
        SELECT DISTINCT ON (a.exercise_id) a.verdict
        FROM exercise_attempts a
        WHERE a.quiz_id = p_quiz
        ORDER BY a.exercise_id, a.created_at DESC, a.id DESC
    ), s AS (
        SELECT round(100.0 * coalesce(sum(CASE verdict WHEN 'correct' THEN 1 WHEN 'partial' THEN 0.5 ELSE 0 END), 0)
                     / nullif((SELECT count(*) FROM quiz_questions WHERE quiz_id = p_quiz), 0), 2) AS score
        FROM last
    )
    UPDATE quizzes q SET score = coalesce(s.score, 0), status = 'completed', completed_at = now()
    FROM s WHERE q.id = p_quiz
    RETURNING q.score;
$$;
