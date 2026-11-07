-- Дашборд: ДЗ со статусом todo, у которого дедлайн уже прошёл, считается просроченным,
-- даже если после дедлайна ещё не было синхронизации с платформой.
CREATE OR REPLACE VIEW user_dashboard AS
SELECT u.id AS user_id,
       (SELECT count(*) FROM assignments x WHERE x.user_id = u.id AND x.status = 'todo'
                                             AND (x.due_at IS NULL OR x.due_at >= now()))   AS homework_todo,
       (SELECT count(*) FROM assignments x WHERE x.user_id = u.id
                                             AND (x.status = 'overdue'
                                                  OR (x.status = 'todo' AND x.due_at < now()))) AS homework_overdue,
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
