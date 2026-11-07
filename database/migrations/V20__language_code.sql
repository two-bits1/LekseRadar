-- В переменных промпта рядом с названием языка теперь есть его код.
-- {{language}}      = 'русский (ru)' — для текста промпта (V17)
-- {{language_code}} = 'ru'           — для поля "language" в JSON квиза: по нему ищется готовый квиз
--                                       (find_quiz_set) и на нём проверяются текстовые ответы.
-- Язык = свой язык чата, иначе язык сайта (users.locale).

CREATE OR REPLACE FUNCTION language_code(p_code TEXT) RETURNS TEXT LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE WHEN lower(btrim(p_code)) = 'no' THEN 'nb'
                WHEN lower(btrim(p_code)) ~ '^[a-z]{2,3}$' THEN lower(btrim(p_code))
                ELSE 'nb' END
$$;

CREATE OR REPLACE FUNCTION chat_prompt_vars(p_session BIGINT) RETURNS JSONB LANGUAGE sql STABLE AS $$
    SELECT jsonb_build_object(
        'language',      language_name(coalesce(s.language, u.locale::TEXT)),
        'language_code', language_code(coalesce(s.language, u.locale::TEXT)),
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
