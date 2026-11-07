-- Функции для квизов из JSON: импорт, поиск готового набора, старт/продолжение, ответы, прогресс.
-- Поток для бэкенда:
--   1) set_id := find_quiz_set(предмет, тема, уровень, язык)
--   2) если NULL → попросить ИИ сгенерировать JSON → set_id := import_quiz_json(json, user_id)
--   3) quiz_id := start_quiz(user_id, set_id)          -- продолжит незаконченный, если он есть
--   4) на каждый ответ: save_quiz_answer(quiz_id, позиция, ответ)  -- выбор проверяется сам
--   5) finish_quiz(quiz_id) → балл 0–100

-- ---------- 1. Поиск готового набора ----------
CREATE FUNCTION find_quiz_set(p_subject TEXT, p_topic TEXT, p_level exercise_level, p_language TEXT DEFAULT 'nb')
RETURNS BIGINT LANGUAGE sql STABLE AS $$
    SELECT qs.id
    FROM quiz_sets qs JOIN subjects s ON s.id = qs.subject_id
    WHERE s.key = norm_key(p_subject) AND qs.topic_key = norm_key(p_topic)
      AND qs.level = p_level AND qs.language = coalesce(p_language, 'nb')
    ORDER BY qs.created_at DESC
    LIMIT 1
$$;

-- ---------- 2. Импорт JSON от ИИ (формат quiz/quiz.schema.json) ----------
-- Возвращает id набора. Если такой же набор уже был импортирован — возвращает его id, дубля не будет.
CREATE FUNCTION import_quiz_json(p_json JSONB, p_user BIGINT DEFAULT NULL)
RETURNS BIGINT LANGUAGE plpgsql AS $$
DECLARE
    v_subject_id BIGINT;
    v_set_id     BIGINT;
    v_level      exercise_level;
    v_lang       TEXT := coalesce(nullif(btrim(p_json->>'language'), ''), 'nb');
    v_hash       TEXT;
    q            JSONB;
    i            INT;
BEGIN
    IF jsonb_typeof(p_json) <> 'object' THEN
        RAISE EXCEPTION 'quiz json: ожидается объект';
    END IF;
    IF coalesce(btrim(p_json->>'subject'), '') = '' OR coalesce(btrim(p_json->>'topic'), '') = ''
       OR coalesce(btrim(p_json->>'title'), '') = '' THEN
        RAISE EXCEPTION 'quiz json: нужны subject, topic и title';
    END IF;
    IF p_json->>'level' NOT IN ('beginner', 'intermediate', 'advanced') THEN
        RAISE EXCEPTION 'quiz json: level должен быть beginner, intermediate или advanced';
    END IF;
    v_level := (p_json->>'level')::exercise_level;
    IF jsonb_typeof(p_json->'questions') <> 'array' OR jsonb_array_length(p_json->'questions') = 0 THEN
        RAISE EXCEPTION 'quiz json: questions должен быть непустым массивом';
    END IF;

    FOR q, i IN SELECT value, ordinality FROM jsonb_array_elements(p_json->'questions') WITH ORDINALITY LOOP
        IF q->>'type' NOT IN ('single_choice', 'multiple_choice', 'text') THEN
            RAISE EXCEPTION 'quiz json: вопрос %: type должен быть single_choice, multiple_choice или text', i;
        END IF;
        IF coalesce(btrim(q->>'question'), '') = '' OR q->'correct_answer' IS NULL THEN
            RAISE EXCEPTION 'quiz json: вопрос %: нужны question и correct_answer', i;
        END IF;
        IF q->>'type' <> 'text' AND (jsonb_typeof(q->'options') <> 'array' OR jsonb_array_length(q->'options') < 2) THEN
            RAISE EXCEPTION 'quiz json: вопрос %: для выбора нужен options (минимум 2 варианта)', i;
        END IF;
        IF q->>'type' = 'multiple_choice' AND jsonb_typeof(q->'correct_answer') <> 'array' THEN
            RAISE EXCEPTION 'quiz json: вопрос %: для multiple_choice correct_answer — массив', i;
        END IF;
        IF q->>'type' <> 'text' AND NOT (
            CASE jsonb_typeof(q->'correct_answer')
                WHEN 'array' THEN q->'options' @> q->'correct_answer'
                ELSE q->'options' @> jsonb_build_array(q->>'correct_answer') END) THEN
            RAISE EXCEPTION 'quiz json: вопрос %: correct_answer должен быть среди options', i;
        END IF;
    END LOOP;

    INSERT INTO subjects (name, key) VALUES (btrim(p_json->>'subject'), norm_key(p_json->>'subject'))
    ON CONFLICT (key) DO UPDATE SET key = EXCLUDED.key
    RETURNING id INTO v_subject_id;

    v_hash := md5(concat_ws('|', norm_key(p_json->>'subject'), norm_key(p_json->>'topic'),
                            v_level::TEXT, v_lang, (p_json->'questions')::TEXT));
    SELECT id INTO v_set_id FROM quiz_sets WHERE content_hash = v_hash;
    IF v_set_id IS NOT NULL THEN
        RETURN v_set_id;
    END IF;

    INSERT INTO quiz_sets (subject_id, topic, topic_key, level, language, title, question_count,
                           source_json, content_hash, model, created_by)
    VALUES (v_subject_id, btrim(p_json->>'topic'), norm_key(p_json->>'topic'), v_level, v_lang,
            btrim(p_json->>'title'), jsonb_array_length(p_json->'questions'),
            p_json, v_hash, p_json->>'model', p_user)
    RETURNING id INTO v_set_id;

    INSERT INTO exercises (user_id, subject_id, quiz_set_id, position, topic, level, statement,
                           correct_answer, answer_type, question_type, options, explanation, generated)
    SELECT NULL, v_subject_id, v_set_id, ord, btrim(p_json->>'topic'), v_level, q->>'question',
           CASE jsonb_typeof(q->'correct_answer') WHEN 'array' THEN (q->'correct_answer')::TEXT
                ELSE q->>'correct_answer' END,
           CASE q->>'type' WHEN 'text' THEN 'free' ELSE 'exact' END::answer_type,
           q->>'type', CASE WHEN q->>'type' = 'text' THEN NULL ELSE q->'options' END,
           q->>'explanation', true
    FROM jsonb_array_elements(p_json->'questions') WITH ORDINALITY AS t(q, ord);

    RETURN v_set_id;
END $$;

-- ---------- 3. Старт или продолжение ----------
CREATE FUNCTION start_quiz(p_user BIGINT, p_set BIGINT) RETURNS BIGINT LANGUAGE plpgsql AS $$
DECLARE
    v_quiz BIGINT;
BEGIN
    SELECT id INTO v_quiz FROM quizzes
    WHERE user_id = p_user AND quiz_set_id = p_set AND status = 'in_progress'
    ORDER BY started_at DESC LIMIT 1;
    IF v_quiz IS NOT NULL THEN
        RETURN v_quiz;                       -- продолжить с current_position
    END IF;

    INSERT INTO quizzes (user_id, subject_id, quiz_set_id, title, topic, level)
    SELECT p_user, subject_id, id, title, topic, level FROM quiz_sets WHERE id = p_set
    RETURNING id INTO v_quiz;
    IF v_quiz IS NULL THEN
        RAISE EXCEPTION 'quiz set % не найден', p_set;
    END IF;

    INSERT INTO quiz_questions (quiz_id, position, exercise_id)
    SELECT v_quiz, position, id FROM exercises WHERE quiz_set_id = p_set;
    UPDATE quiz_sets SET times_used = times_used + 1 WHERE id = p_set;
    INSERT INTO user_events (user_id, type, ref_table, ref_id, payload)
    VALUES (p_user, 'quiz_started', 'quizzes', v_quiz, jsonb_build_object('quiz_set_id', p_set));
    RETURN v_quiz;
END $$;

-- Вопросы для показа пользователю: без правильного ответа и пояснения
CREATE VIEW quiz_questions_public AS
SELECT qq.quiz_id, qq.position, e.id AS exercise_id, e.question_type, e.statement AS question, e.options
FROM quiz_questions qq JOIN exercises e ON e.id = qq.exercise_id;

-- ---------- 4. Ответ ----------
-- Выбор (single/multiple_choice) проверяется сразу в базе. Для text передай p_verdict и p_explanation
-- от проверки ответов ИИ (CheckResult). Ответ на multiple_choice — JSON-массив строкой: '["a","b"]'.
CREATE FUNCTION save_quiz_answer(p_quiz BIGINT, p_position INT, p_answer TEXT,
                                 p_verdict verdict DEFAULT NULL, p_explanation TEXT DEFAULT NULL)
RETURNS TABLE (attempt_id BIGINT, verdict verdict, explanation TEXT, correct_answer TEXT)
LANGUAGE plpgsql AS $$
DECLARE
    v_user BIGINT;
    e      exercises%ROWTYPE;
    v_ver  verdict := p_verdict;
    v_exp  TEXT := p_explanation;
    v_id   BIGINT;
    v_got  JSONB;
    v_ok   JSONB;
BEGIN
    SELECT q.user_id INTO v_user FROM quizzes q WHERE q.id = p_quiz AND q.status = 'in_progress';
    IF v_user IS NULL THEN
        RAISE EXCEPTION 'квиз % не найден или уже завершён', p_quiz;
    END IF;
    SELECT x.* INTO e FROM quiz_questions qq JOIN exercises x ON x.id = qq.exercise_id
    WHERE qq.quiz_id = p_quiz AND qq.position = p_position;
    IF e.id IS NULL THEN
        RAISE EXCEPTION 'в квизе % нет вопроса %', p_quiz, p_position;
    END IF;

    IF v_ver IS NULL AND e.question_type = 'single_choice' THEN
        v_ver := CASE WHEN norm_key(p_answer) = norm_key(e.correct_answer) THEN 'correct' ELSE 'incorrect' END;
    ELSIF v_ver IS NULL AND e.question_type = 'multiple_choice' THEN
        v_got := p_answer::JSONB;
        v_ok  := e.correct_answer::JSONB;
        v_ver := CASE WHEN v_got @> v_ok AND v_ok @> v_got THEN 'correct'
                      WHEN v_ok @> v_got AND jsonb_array_length(v_got) > 0 THEN 'partial'
                      ELSE 'incorrect' END;
    ELSIF v_ver IS NULL THEN
        RAISE EXCEPTION 'для текстового вопроса нужен verdict от проверки ИИ';
    END IF;
    v_exp := coalesce(v_exp, e.explanation);

    INSERT INTO exercise_attempts (user_id, exercise_id, quiz_id, answer, verdict, explanation)
    VALUES (v_user, e.id, p_quiz, p_answer, v_ver, v_exp)
    RETURNING id INTO v_id;
    UPDATE quizzes SET current_position = greatest(current_position, p_position + 1) WHERE id = p_quiz;
    INSERT INTO user_events (user_id, type, ref_table, ref_id, payload)
    VALUES (v_user, 'exercise_answered', 'exercises', e.id, jsonb_build_object('quiz_id', p_quiz, 'verdict', v_ver));

    RETURN QUERY SELECT v_id, v_ver, v_exp, e.correct_answer;
END $$;

-- ---------- 5. Прогресс по предметам и темам квизов ----------
CREATE VIEW user_quiz_progress AS
SELECT q.user_id,
       s.name                                                   AS subject,
       q.topic,
       count(*) FILTER (WHERE q.status = 'completed')           AS quizzes_completed,
       count(*) FILTER (WHERE q.status = 'in_progress')         AS quizzes_in_progress,
       max(q.score)                                             AS best_score,
       (array_agg(q.score ORDER BY q.completed_at DESC NULLS LAST)
            FILTER (WHERE q.status = 'completed'))[1]           AS last_score,
       max(coalesce(q.completed_at, q.started_at))              AS last_activity
FROM quizzes q
LEFT JOIN subjects s ON s.id = q.subject_id
GROUP BY q.user_id, s.name, q.topic;
