-- ИИ-чат: сессии и сообщения (история чата с тьютором).

CREATE TYPE chat_role AS ENUM ('student', 'tutor', 'system');

CREATE TABLE chat_sessions (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assignment_id  BIGINT      REFERENCES assignments(id) ON DELETE SET NULL, -- чат по конкретному ДЗ
    title          TEXT        NOT NULL DEFAULT 'Ny samtale',
    topic          TEXT,
    archived       BOOLEAN     NOT NULL DEFAULT false,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX chat_sessions_user ON chat_sessions (user_id, updated_at DESC);

CREATE TABLE chat_messages (
    id           BIGSERIAL PRIMARY KEY,
    session_id   BIGINT      NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role         chat_role   NOT NULL,
    content      TEXT        NOT NULL CHECK (length(content) BETWEEN 1 AND 8000),
    on_topic     BOOLEAN,                 -- TutorResponse.on_topic (только у ответов тьютора)
    model        TEXT,                    -- какая модель ответила
    tokens_used  INT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX chat_messages_session ON chat_messages (session_id, created_at);

-- Новое сообщение поднимает сессию наверх списка чатов
CREATE FUNCTION touch_chat_session() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    UPDATE chat_sessions SET updated_at = NEW.created_at WHERE id = NEW.session_id;
    RETURN NEW;
END $$;
CREATE TRIGGER chat_messages_touch AFTER INSERT ON chat_messages
    FOR EACH ROW EXECUTE FUNCTION touch_chat_session();

-- Последние 10 реплик сессии в хронологическом порядке: это TutorRequest.history для Python API.
-- SELECT role, content FROM chat_context WHERE session_id = ? ORDER BY created_at;
CREATE VIEW chat_context AS
SELECT session_id, role, content, created_at
FROM (
    SELECT m.*, row_number() OVER (PARTITION BY session_id ORDER BY created_at DESC, id DESC) AS rn
    FROM chat_messages m
    WHERE role <> 'system'
) t
WHERE rn <= 10;
