-- Уже сохранённые квизы от ИИ: пустые и повторяющиеся варианты убрать, а single_choice с несколькими
-- верными ответами сделать multiple_choice (иначе на сайте можно выбрать только один вариант).
-- Новые квизы чинит бэкенд перед import_quiz_json (QuizGeneratorService.fixQuestion).

-- 1. Варианты: без пустых строк и повторов, порядок как был.
UPDATE exercises e
SET options = c.cleaned
FROM (
    SELECT e2.id,
           (SELECT coalesce(jsonb_agg(to_jsonb(v) ORDER BY first_ord), '[]'::jsonb)
            FROM (SELECT btrim(o.value) AS v, min(o.ord) AS first_ord
                  FROM jsonb_array_elements_text(e2.options) WITH ORDINALITY AS o(value, ord)
                  WHERE btrim(o.value) <> ''
                  GROUP BY btrim(o.value)) s) AS cleaned
    FROM exercises e2
    WHERE e2.options IS NOT NULL AND jsonb_typeof(e2.options) = 'array'
) c
WHERE e.id = c.id AND e.options IS DISTINCT FROM c.cleaned;

-- 2. single_choice, у которого ответ записан JSON-массивом (битый JSON не трогаем).
CREATE FUNCTION pg_temp.try_jsonb(t TEXT) RETURNS JSONB LANGUAGE plpgsql IMMUTABLE AS $$
BEGIN
    RETURN t::jsonb;
EXCEPTION WHEN others THEN
    RETURN NULL;
END $$;

UPDATE exercises
SET question_type = 'multiple_choice'
WHERE question_type = 'single_choice'
  AND jsonb_typeof(pg_temp.try_jsonb(correct_answer)) = 'array'
  AND jsonb_array_length(pg_temp.try_jsonb(correct_answer)) > 1;

UPDATE exercises
SET correct_answer = pg_temp.try_jsonb(correct_answer) ->> 0
WHERE question_type = 'single_choice'
  AND jsonb_typeof(pg_temp.try_jsonb(correct_answer)) = 'array'
  AND jsonb_array_length(pg_temp.try_jsonb(correct_answer)) = 1
  AND btrim(pg_temp.try_jsonb(correct_answer) ->> 0) <> '';
