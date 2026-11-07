-- Ограничение длины текста ДЗ в промпте: длинный PDF (учебник целиком) не должен
-- съесть весь контекст модели и деньги. ~12 000 символов ≈ 3–4 тыс. токенов.

CREATE OR REPLACE FUNCTION clip_text(p_text TEXT, p_max INT DEFAULT 12000) RETURNS TEXT
LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE WHEN length(p_text) <= p_max THEN p_text
                ELSE left(p_text, p_max) || E'\n\n[... текст задания обрезан, показано ' || p_max || ' символов]' END
$$;

CREATE OR REPLACE FUNCTION chat_prompt_vars(p_session BIGINT) RETURNS JSONB LANGUAGE sql STABLE AS $$
    SELECT jsonb_build_object(
        'language', coalesce(s.language,
                             CASE s.mode WHEN 'tutor' THEN 'не выбран' ELSE u.locale::TEXT END),
        'subject',  coalesce(sb.name, a.course, ''),
        'topic',    coalesce(s.topic, ''),
        'level',    s.level,
        'homework_title', coalesce(a.title, 'нет'),
        'homework_due',   coalesce(to_char(a.due_at AT TIME ZONE u.timezone, 'DD.MM.YYYY HH24:MI'), 'не указан'),
        'homework_text',  clip_text(coalesce(
            (SELECT string_agg(c.content, E'\n\n' ORDER BY m.id, c.position)
             FROM materials m JOIN material_chunks c ON c.material_id = m.id
             WHERE m.assignment_id = a.id),
            nullif(a.description, ''),
            'текст задания не загружен')),
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
