-- Запись ДЗ из homework-collector одним вызовом: бэкенд (POST /homework/sync) и to_db.py
-- передают сюда CollectResult.assignments как есть (JSON-массив Assignment).
--   SELECT * FROM sync_assignments(:user_id, CAST(:assignments AS jsonb));  → (added, updated)
-- UPSERT по (user_id, source, external_id). Статус «сдано»/«оценено», выставленный на сайте, не откатывается,
-- если платформа ещё показывает todo/overdue. Пустое описание с платформы не затирает сохранённое.
-- Задания, которых больше нет на платформе, не удаляются: по ним могут быть чаты и квизы.

CREATE FUNCTION sync_assignments(p_user BIGINT, items JSONB)
RETURNS TABLE (added INT, updated INT)
LANGUAGE sql AS $$
    WITH src AS (
        -- одинаковый id дважды в одном ответе сломал бы ON CONFLICT: берём последний
        SELECT DISTINCT ON (x->>'source', x->>'id') x, ord
        FROM jsonb_array_elements(coalesce(items, '[]'::jsonb)) WITH ORDINALITY AS t(x, ord)
        ORDER BY x->>'source', x->>'id', ord DESC
    ), up AS (
        INSERT INTO assignments AS a (user_id, source, external_id, title, course, due_at, status, url, description, kind)
        SELECT p_user, (x->>'source')::platform, x->>'id', x->>'title', coalesce(x->>'course', ''),
               (x->>'due')::timestamptz, (x->>'status')::assignment_status, x->>'url',
               coalesce(x->>'description', ''), coalesce(x->>'kind', 'assignment')
        FROM src
        ON CONFLICT (user_id, source, external_id) DO UPDATE SET
            title       = EXCLUDED.title,
            course      = EXCLUDED.course,
            due_at      = EXCLUDED.due_at,
            url         = EXCLUDED.url,
            description = CASE WHEN EXCLUDED.description <> '' THEN EXCLUDED.description ELSE a.description END,
            kind        = EXCLUDED.kind,
            status      = CASE WHEN a.status IN ('submitted', 'graded') AND EXCLUDED.status IN ('todo', 'overdue')
                               THEN a.status ELSE EXCLUDED.status END,
            updated_at  = now()
        RETURNING (xmax = 0) AS inserted
    )
    SELECT (count(*) FILTER (WHERE inserted))::int, (count(*) FILTER (WHERE NOT inserted))::int FROM up;
$$;

COMMENT ON FUNCTION sync_assignments(BIGINT, JSONB) IS
    'UPSERT заданий из collect(): возвращает (added, updated). Не откатывает submitted/graded, не удаляет пропавшие.';
