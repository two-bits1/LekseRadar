-- Флеш-карты — это вопросы квизов в режиме повторения, а не отдельная сущность.
-- Один JSON от ИИ (quiz/quiz.schema.json) даёт и квиз, и колоду карточек:
--   лицевая сторона = вопрос (exercises.statement), обратная = правильный ответ + пояснение.
-- Отдельно хранится только прогресс повторения каждого пользователя по каждой карточке.

CREATE TABLE flashcard_progress (
    user_id           BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_id       BIGINT       NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    ease              NUMERIC(4,2) NOT NULL DEFAULT 2.5,   -- SM-2: насколько легко даётся
    interval_days     INT          NOT NULL DEFAULT 0,
    repetitions       INT          NOT NULL DEFAULT 0,     -- успешных повторов подряд
    reviews           INT          NOT NULL DEFAULT 0,
    lapses            INT          NOT NULL DEFAULT 0,     -- сколько раз забыл
    next_review_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_reviewed_at  TIMESTAMPTZ,
    added_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, exercise_id)
);
CREATE INDEX flashcard_progress_due ON flashcard_progress (user_id, next_review_at);

-- Колода пользователя: карточка = вопрос квиза
-- Карточки к повторению: SELECT * FROM flashcards WHERE user_id = ? AND due ORDER BY next_review_at;
CREATE VIEW flashcards AS
SELECT p.user_id,
       e.id            AS exercise_id,
       e.quiz_set_id,
       s.name          AS subject,
       e.topic,
       e.statement     AS front,
       CASE WHEN e.question_type = 'multiple_choice'
            THEN (SELECT string_agg(x, ', ') FROM jsonb_array_elements_text(e.correct_answer::JSONB) AS x)
            ELSE e.correct_answer END AS back,
       e.explanation,
       p.next_review_at,
       p.interval_days,
       p.reviews,
       p.next_review_at <= now() AS due
FROM flashcard_progress p
JOIN exercises e ON e.id = p.exercise_id
LEFT JOIN subjects s ON s.id = e.subject_id;

-- Добавить весь квиз в колоду (кнопка «Учить карточками»). Возвращает число новых карточек.
CREATE FUNCTION add_quiz_set_to_flashcards(p_user BIGINT, p_set BIGINT) RETURNS INT LANGUAGE sql AS $$
    WITH ins AS (
        INSERT INTO flashcard_progress (user_id, exercise_id)
        SELECT p_user, id FROM exercises WHERE quiz_set_id = p_set
        ON CONFLICT DO NOTHING
        RETURNING 1
    )
    SELECT count(*)::INT FROM ins
$$;

-- Ответ на карточку: p_grade 0–5 (0–2 = не помню, 3 = с трудом, 4 = помню, 5 = легко). Алгоритм SM-2.
CREATE FUNCTION review_flashcard(p_user BIGINT, p_exercise BIGINT, p_grade INT)
RETURNS TABLE (next_review_at TIMESTAMPTZ, interval_days INT) LANGUAGE plpgsql AS $$
DECLARE
    c flashcard_progress%ROWTYPE;
BEGIN
    IF p_grade NOT BETWEEN 0 AND 5 THEN
        RAISE EXCEPTION 'grade должен быть от 0 до 5';
    END IF;
    INSERT INTO flashcard_progress (user_id, exercise_id) VALUES (p_user, p_exercise)
    ON CONFLICT DO NOTHING;
    SELECT * INTO c FROM flashcard_progress f WHERE f.user_id = p_user AND f.exercise_id = p_exercise FOR UPDATE;

    IF p_grade < 3 THEN
        c.repetitions := 0;
        c.interval_days := 0;
        c.lapses := c.lapses + 1;
        c.next_review_at := now() + interval '10 minutes';       -- показать снова в этой же сессии
    ELSE
        c.repetitions := c.repetitions + 1;
        c.interval_days := CASE c.repetitions WHEN 1 THEN 1 WHEN 2 THEN 6
                                ELSE greatest(1, round(c.interval_days * c.ease))::INT END;
        c.next_review_at := now() + make_interval(days => c.interval_days);
    END IF;
    c.ease := greatest(1.3, c.ease + (0.1 - (5 - p_grade) * (0.08 + (5 - p_grade) * 0.02)));

    UPDATE flashcard_progress f
    SET ease = c.ease, interval_days = c.interval_days, repetitions = c.repetitions, lapses = c.lapses,
        reviews = f.reviews + 1, next_review_at = c.next_review_at, last_reviewed_at = now()
    WHERE f.user_id = p_user AND f.exercise_id = p_exercise;

    INSERT INTO user_events (user_id, type, ref_table, ref_id, payload)
    VALUES (p_user, 'flashcard_reviewed', 'exercises', p_exercise, jsonb_build_object('grade', p_grade));

    RETURN QUERY SELECT c.next_review_at, c.interval_days;
END $$;

-- Ошибка в квизе сразу превращает вопрос в карточку к повторению
CREATE FUNCTION attempt_to_flashcard() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.verdict <> 'correct' THEN
        INSERT INTO flashcard_progress (user_id, exercise_id, next_review_at)
        VALUES (NEW.user_id, NEW.exercise_id, now())
        ON CONFLICT (user_id, exercise_id) DO UPDATE
        SET next_review_at = least(flashcard_progress.next_review_at, now());
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER exercise_attempts_to_flashcard AFTER INSERT ON exercise_attempts
    FOR EACH ROW EXECUTE FUNCTION attempt_to_flashcard();

-- Сводка по карточкам для дашборда
CREATE VIEW user_flashcard_stats AS
SELECT user_id,
       count(*)                                          AS cards,
       count(*) FILTER (WHERE next_review_at <= now())   AS due_now,
       count(*) FILTER (WHERE interval_days >= 21)       AS learned,     -- интервал от 3 недель
       sum(reviews)                                      AS reviews
FROM flashcard_progress
GROUP BY user_id;
