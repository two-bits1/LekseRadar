-- Отключение платформы удаляет её задания и всё, что ученик сделал по ним: чаты, квизы, карточки, ответы, историю,
-- файлы ДЗ. Раньше DELETE /platforms/{platform} удалял только доступ, и после отключения (например, Google Classroom)
-- задания, квизы и карточки оставались на главной и в карточках.
-- Теперь это делает бэкенд (PlatformService.disconnect → AssignmentRepository.removePlatformAssignments) обычным SQL,
-- поэтому бэкенд работает и без этой миграции. Она один раз убирает то, что осталось от прошлых отключений:
-- задания платформ, которых у ученика больше нет в platform_connections, и всё, что с ними связано.
-- Те же запросы, что в AssignmentRepository, для каждой пары (ученик, платформа). Общие наборы вопросов
-- (quiz_sets и их exercises) не удаляются: ими пользуются и другие ученики.
-- Задания, записанные в обход сайта через homework-collector/homework/to_db.py без подключения, тоже удалятся.

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT DISTINCT a.user_id AS u, a.source AS s
        FROM assignments a
        WHERE NOT EXISTS (SELECT 1 FROM platform_connections c WHERE c.user_id = a.user_id AND c.platform = a.source)
    LOOP
        CREATE TEMP TABLE gone_a ON COMMIT DROP AS
            SELECT id FROM assignments WHERE user_id = r.u AND source = r.s;
        CREATE TEMP TABLE gone_q ON COMMIT DROP AS
            SELECT id, quiz_set_id, chat_session_id FROM quizzes WHERE user_id = r.u AND assignment_id IN (SELECT id FROM gone_a);
        CREATE TEMP TABLE gone_c ON COMMIT DROP AS
            SELECT id FROM chat_sessions WHERE user_id = r.u
               AND (assignment_id IN (SELECT id FROM gone_a) OR id IN (SELECT chat_session_id FROM gone_q));
        CREATE TEMP TABLE gone_e ON COMMIT DROP AS
            SELECT id FROM exercises
             WHERE quiz_set_id IN (SELECT quiz_set_id FROM gone_q WHERE quiz_set_id IS NOT NULL
                                   EXCEPT SELECT quiz_set_id FROM quizzes WHERE user_id = r.u AND quiz_set_id IS NOT NULL
                                                                            AND id NOT IN (SELECT id FROM gone_q))
                OR (user_id = r.u AND assignment_id IN (SELECT id FROM gone_a));

        DELETE FROM user_events WHERE user_id = r.u AND (
               (ref_table = 'assignments' AND ref_id IN (SELECT id FROM gone_a))
            OR (ref_table = 'quizzes' AND ref_id IN (SELECT id FROM gone_q))
            OR (payload ->> 'quiz_id' IN (SELECT CAST(id AS text) FROM gone_q))
            OR (ref_table = 'chat_sessions' AND ref_id IN (SELECT id FROM gone_c))
            OR (ref_table = 'exercises' AND ref_id IN (SELECT id FROM gone_e)));
        DELETE FROM exercise_attempts WHERE user_id = r.u AND exercise_id IN (SELECT id FROM gone_e);
        DELETE FROM flashcard_progress WHERE user_id = r.u AND exercise_id IN (SELECT id FROM gone_e);
        DELETE FROM chat_sessions WHERE id IN (SELECT id FROM gone_c);
        DELETE FROM quizzes WHERE id IN (SELECT id FROM gone_q);
        DELETE FROM exercises e WHERE e.user_id = r.u AND e.assignment_id IN (SELECT id FROM gone_a)
           AND NOT EXISTS (SELECT 1 FROM quiz_questions qq WHERE qq.exercise_id = e.id);
        DELETE FROM materials WHERE assignment_id IN (SELECT id FROM gone_a) AND source = 'platform';
        DELETE FROM assignments WHERE id IN (SELECT id FROM gone_a);

        DROP TABLE gone_a, gone_q, gone_c, gone_e;
    END LOOP;
END $$;
