-- save_quiz_answer: понятные ошибки вместо сырых ошибок PostgreSQL.
-- multiple_choice не JSON-массивом и пустой ответ → 'quiz answer: ...' (бэкенд отдаёт это как 400).

CREATE OR REPLACE FUNCTION save_quiz_answer(p_quiz BIGINT, p_position INT, p_answer TEXT,
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
    IF p_answer IS NULL OR btrim(p_answer) = '' THEN
        RAISE EXCEPTION 'quiz answer: пустой ответ';
    END IF;

    IF v_ver IS NULL AND e.question_type = 'single_choice' THEN
        v_ver := CASE WHEN norm_key(p_answer) = norm_key(e.correct_answer) THEN 'correct' ELSE 'incorrect' END;
    ELSIF v_ver IS NULL AND e.question_type = 'multiple_choice' THEN
        BEGIN
            v_got := p_answer::JSONB;
        EXCEPTION WHEN others THEN
            v_got := NULL;
        END;
        IF v_got IS NULL OR jsonb_typeof(v_got) <> 'array' THEN
            RAISE EXCEPTION 'quiz answer: для multiple_choice ответ — JSON-массив вариантов, например ["a","b"]';
        END IF;
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
