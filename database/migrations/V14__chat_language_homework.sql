-- Чат по домашнему заданию: язык ученика (выбирается первым вопросом репетитора),
-- уровень квиза и файл ДЗ, который получает ИИ.

-- NULL = язык ещё не выбран → репетитор первым сообщением спрашивает язык
ALTER TABLE chat_sessions
    ADD COLUMN language TEXT CHECK (language ~ '^[a-z]{2,3}$'),
    ADD COLUMN level    exercise_level NOT NULL DEFAULT 'intermediate';

-- Файл ДЗ (текст задания из PDF/DOCX/платформы) хранится как материал, привязанный к заданию
ALTER TABLE materials ADD COLUMN assignment_id BIGINT REFERENCES assignments(id) ON DELETE SET NULL;
CREATE INDEX materials_assignment ON materials (assignment_id);

-- Сохранить файл ДЗ. Текст из файла извлекает бэкенд (или load.py). Возвращает id материала.
CREATE FUNCTION attach_homework_file(p_assignment BIGINT, p_text TEXT,
                                     p_file_name TEXT DEFAULT NULL, p_mime_type TEXT DEFAULT NULL,
                                     p_storage_key TEXT DEFAULT NULL)
RETURNS BIGINT LANGUAGE plpgsql AS $$
DECLARE
    a    assignments%ROWTYPE;
    v_id BIGINT;
BEGIN
    SELECT * INTO a FROM assignments WHERE id = p_assignment;
    IF NOT FOUND THEN RAISE EXCEPTION 'задание % не найдено', p_assignment; END IF;
    v_id := add_material(a.user_id, nullif(a.course, ''), NULL, a.title, p_text,
                         p_file_name => p_file_name, p_mime_type => p_mime_type,
                         p_source => 'platform', p_storage_key => p_storage_key);
    UPDATE materials SET assignment_id = p_assignment WHERE id = v_id;
    RETURN v_id;
END $$;

-- Ученик выбрал язык (бэкенд нашёл в ответе ИИ метку [[LANGUAGE=xx]])
CREATE FUNCTION set_chat_language(p_session BIGINT, p_language TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    UPDATE chat_sessions SET language = lower(btrim(p_language)) WHERE id = p_session;
    IF NOT FOUND THEN RAISE EXCEPTION 'чат % не найден', p_session; END IF;
END $$;

-- Все переменные {{...}} для промпта чата одним JSON. {{materials}} бэкенд добавляет сам
-- через materials_for_prompt(вопрос), а для квиза ещё {{count}}.
--   tutor: язык 'не выбран', пока ученик не ответил на первый вопрос
--   quiz:  язык чата, иначе язык профиля
CREATE FUNCTION chat_prompt_vars(p_session BIGINT) RETURNS JSONB LANGUAGE sql STABLE AS $$
    SELECT jsonb_build_object(
        'language', coalesce(s.language,
                             CASE s.mode WHEN 'tutor' THEN 'не выбран' ELSE u.locale::TEXT END),
        'subject',  coalesce(sb.name, a.course, ''),
        'topic',    coalesce(s.topic, ''),
        'level',    s.level,
        'homework_title', coalesce(a.title, 'нет'),
        'homework_due',   coalesce(to_char(a.due_at AT TIME ZONE u.timezone, 'DD.MM.YYYY HH24:MI'), 'не указан'),
        'homework_text',  coalesce(
            (SELECT string_agg(c.content, E'\n\n' ORDER BY m.id, c.position)
             FROM materials m JOIN material_chunks c ON c.material_id = m.id
             WHERE m.assignment_id = a.id),
            nullif(a.description, ''),
            'текст задания не загружен'),
        'weak_topics', coalesce(
            (SELECT string_agg(p.topic || ' (' || p.mastery_pct || '%)', ', ' ORDER BY p.mastery_pct)
             FROM (SELECT * FROM user_topic_progress
                   WHERE user_id = s.user_id AND mastery_pct < 70 AND topic IS NOT NULL
                   ORDER BY mastery_pct LIMIT 3) p),
            'нет данных')
    )
    FROM chat_sessions s
    JOIN users u ON u.id = s.user_id
    LEFT JOIN subjects sb ON sb.id = s.subject_id
    LEFT JOIN assignments a ON a.id = s.assignment_id
    WHERE s.id = p_session
$$;
