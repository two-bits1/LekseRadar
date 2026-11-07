-- История пользователя (лента действий) и прогресс.

-- Лента действий: что пользователь делал и когда. Пишет Java при каждом действии.
-- type: login | homework_synced | homework_opened | chat_started | quiz_started | quiz_completed |
--       exercise_answered | achievement_unlocked | flashcard_reviewed
CREATE TABLE user_events (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        TEXT        NOT NULL,
    ref_table   TEXT,                     -- например 'quizzes'
    ref_id      BIGINT,                   -- id строки в ref_table
    payload     JSONB       NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX user_events_user ON user_events (user_id, created_at DESC);

-- Прогресс по темам: correct = 1, partial = 0.5, incorrect = 0
CREATE VIEW user_topic_progress AS
SELECT a.user_id,
       e.topic,
       count(*)                                      AS attempts,
       count(*) FILTER (WHERE a.verdict = 'correct') AS correct,
       count(*) FILTER (WHERE a.verdict = 'partial') AS partial,
       round(100.0 * avg(CASE a.verdict WHEN 'correct' THEN 1 WHEN 'partial' THEN 0.5 ELSE 0 END), 1) AS mastery_pct,
       max(a.created_at)                             AS last_activity
FROM exercise_attempts a
JOIN exercises e ON e.id = a.exercise_id
GROUP BY a.user_id, e.topic;

-- Активность по дням (график на дашборде)
CREATE VIEW user_daily_activity AS
SELECT e.user_id,
       date(e.created_at AT TIME ZONE u.timezone)               AS day,
       count(*)                                                 AS events,
       count(*) FILTER (WHERE e.type = 'exercise_answered')     AS exercises,
       count(*) FILTER (WHERE e.type = 'quiz_completed')        AS quizzes
FROM user_events e
JOIN users u ON u.id = e.user_id
GROUP BY e.user_id, day;

-- Сводка для дашборда: одна строка на пользователя
CREATE VIEW user_dashboard AS
SELECT u.id AS user_id,
       (SELECT count(*) FROM assignments x WHERE x.user_id = u.id AND x.status = 'todo')    AS homework_todo,
       (SELECT count(*) FROM assignments x WHERE x.user_id = u.id AND x.status = 'overdue') AS homework_overdue,
       (SELECT count(*) FROM assignments x WHERE x.user_id = u.id
                                             AND x.status IN ('submitted', 'graded'))       AS homework_done,
       (SELECT count(*) FROM quizzes q WHERE q.user_id = u.id AND q.status = 'completed')   AS quizzes_completed,
       (SELECT round(avg(q.score), 1) FROM quizzes q
         WHERE q.user_id = u.id AND q.status = 'completed')                                 AS avg_quiz_score,
       (SELECT count(*) FROM exercise_attempts a WHERE a.user_id = u.id)                    AS exercises_answered,
       (SELECT count(*) FROM chat_messages m JOIN chat_sessions s ON s.id = m.session_id
         WHERE s.user_id = u.id AND m.role = 'student')                                     AS questions_asked,
       (SELECT count(*) FROM user_daily_activity d
         WHERE d.user_id = u.id AND d.day > current_date - 30)                              AS active_days_30
FROM users u;
