-- Язык ИИ = язык, выбранный на сайте (users.locale). Репетитор больше не спрашивает язык в чате.
-- chat_sessions.language остаётся: если его задать (set_chat_language), он важнее языка сайта.
-- В промпт идёт понятное модели название языка, а не код: «norsk bokmål (nb)».

CREATE OR REPLACE FUNCTION language_name(p_code TEXT) RETURNS TEXT LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE lower(btrim(p_code))
        WHEN 'nb' THEN 'norsk bokmål (nb)'
        WHEN 'nn' THEN 'norsk nynorsk (nn)'
        WHEN 'no' THEN 'norsk bokmål (nb)'
        WHEN 'en' THEN 'English (en)'
        WHEN 'ru' THEN 'русский (ru)'
        WHEN 'uk' THEN 'українська (uk)'
        WHEN 'pl' THEN 'polski (pl)'
        WHEN 'ar' THEN 'العربية (ar)'
        WHEN 'so' THEN 'Soomaali (so)'
        ELSE coalesce(nullif(btrim(p_code), ''), 'norsk bokmål (nb)')
    END
$$;

CREATE OR REPLACE FUNCTION chat_prompt_vars(p_session BIGINT) RETURNS JSONB LANGUAGE sql STABLE AS $$
    SELECT jsonb_build_object(
        'language', language_name(coalesce(s.language, u.locale::TEXT)),
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
