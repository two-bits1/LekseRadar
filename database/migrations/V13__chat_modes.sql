-- Два вида чата: ИИ-репетитор и генерация квизов. К каждому чату всегда подключён свой промпт.
-- tutor → промпт 'tutor', quiz → промпт 'quiz_generator' (активная версия из ai_prompts).
-- Бэкенд перед каждым запросом к ИИ берёт системный промпт через chat_system_prompt(session_id).

CREATE TYPE chat_mode AS ENUM ('tutor', 'quiz');

ALTER TABLE chat_sessions
    ADD COLUMN mode        chat_mode NOT NULL DEFAULT 'tutor',
    ADD COLUMN subject_id  BIGINT REFERENCES subjects(id),
    ADD COLUMN quiz_set_id BIGINT REFERENCES quiz_sets(id) ON DELETE SET NULL;  -- квиз, созданный в этом чате
CREATE INDEX chat_sessions_user_mode ON chat_sessions (user_id, mode, updated_at DESC);

-- Каким промптом (версией) ответил ИИ: видно, какой текст промпта был на момент ответа
ALTER TABLE chat_messages ADD COLUMN prompt_id BIGINT REFERENCES ai_prompts(id);

-- Какой промпт у какого режима. Чтобы подключить другой промпт к режиму, меняется одна строка.
CREATE TABLE chat_mode_prompts (
    mode       chat_mode PRIMARY KEY,
    prompt_key TEXT NOT NULL
);
INSERT INTO chat_mode_prompts VALUES ('tutor', 'tutor'), ('quiz', 'quiz_generator');

-- Режим чата задаётся при создании и потом не меняется
CREATE FUNCTION chat_mode_locked() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.mode <> OLD.mode THEN
        RAISE EXCEPTION 'режим чата нельзя поменять: создай новый чат';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER chat_sessions_mode_locked BEFORE UPDATE OF mode ON chat_sessions
    FOR EACH ROW EXECUTE FUNCTION chat_mode_locked();

-- Создать чат нужного вида. Предмет создаётся, если его нет. Возвращает id чата.
CREATE FUNCTION start_chat(p_user BIGINT, p_mode chat_mode, p_subject TEXT DEFAULT NULL,
                           p_topic TEXT DEFAULT NULL, p_title TEXT DEFAULT NULL,
                           p_assignment BIGINT DEFAULT NULL)
RETURNS BIGINT LANGUAGE plpgsql AS $$
DECLARE
    v_subject BIGINT;
    v_id      BIGINT;
BEGIN
    IF p_subject IS NOT NULL AND btrim(p_subject) <> '' THEN
        INSERT INTO subjects (name, key) VALUES (btrim(p_subject), norm_key(p_subject))
        ON CONFLICT (key) DO NOTHING;
        SELECT id INTO v_subject FROM subjects WHERE key = norm_key(p_subject);
    END IF;
    INSERT INTO chat_sessions (user_id, mode, subject_id, topic, assignment_id, title)
    VALUES (p_user, p_mode, v_subject, nullif(btrim(p_topic), ''), p_assignment,
            coalesce(nullif(btrim(p_title), ''),
                     CASE p_mode WHEN 'quiz' THEN 'Quiz' ELSE 'Ny samtale' END
                     || coalesce(': ' || nullif(btrim(p_topic), ''), '')))
    RETURNING id INTO v_id;
    INSERT INTO user_events (user_id, type, ref_table, ref_id, payload)
    VALUES (p_user, 'chat_started', 'chat_sessions', v_id, jsonb_build_object('mode', p_mode));
    RETURN v_id;
END $$;

-- Системный промпт для чата: активная версия промпта его режима + данные чата для подстановки.
-- Если промпт ещё не загружен в базу — ошибка, чтобы ИИ не ответил без промпта.
CREATE FUNCTION chat_system_prompt(p_session BIGINT)
RETURNS TABLE (prompt_id BIGINT, prompt_key TEXT, version INT, content TEXT,
               mode chat_mode, user_id BIGINT, subject_id BIGINT, subject TEXT, topic TEXT, language TEXT)
LANGUAGE plpgsql STABLE AS $$
BEGIN
    RETURN QUERY
    SELECT p.id, p.key, p.version, p.content, s.mode, s.user_id, s.subject_id, sb.name, s.topic,
           u.locale::TEXT
    FROM chat_sessions s
    JOIN users u              ON u.id = s.user_id
    JOIN chat_mode_prompts mp ON mp.mode = s.mode
    LEFT JOIN subjects sb     ON sb.id = s.subject_id
    LEFT JOIN ai_prompts p    ON p.key = mp.prompt_key AND p.is_active
    WHERE s.id = p_session;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'чат % не найден', p_session;
    END IF;
    IF EXISTS (SELECT 1 FROM chat_sessions s JOIN chat_mode_prompts mp ON mp.mode = s.mode
               WHERE s.id = p_session AND get_prompt(mp.prompt_key) IS NULL) THEN
        RAISE EXCEPTION 'для режима чата % нет промпта в ai_prompts: загрузи ai-content/prompts',
            (SELECT s.mode FROM chat_sessions s WHERE s.id = p_session);
    END IF;
END $$;

-- Список чатов для бокового меню, отдельно по видам
CREATE VIEW user_chats AS
SELECT s.id, s.user_id, s.mode, s.title, sb.name AS subject, s.topic, s.quiz_set_id,
       s.assignment_id, s.archived, s.updated_at,
       (SELECT count(*) FROM chat_messages m WHERE m.session_id = s.id) AS message_count
FROM chat_sessions s
LEFT JOIN subjects sb ON sb.id = s.subject_id;
