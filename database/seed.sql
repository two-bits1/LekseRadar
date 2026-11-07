-- Демо-данные для хакатона: один ученик с ДЗ, чатом, квизом и прогрессом.
-- Запуск после миграций: psql -d lekseradar -f seed.sql  (пароль демо-пользователя: demo1234)

BEGIN;

INSERT INTO users (id, email, password_hash, display_name)
VALUES (1, 'demo@lekseradar.no', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi5BWX4sQn0jHq7QKZ0u3ZJdT1lH0yK', 'Demo Elev');
SELECT setval('users_id_seq', 1);

INSERT INTO assignments (user_id, source, external_id, title, course, due_at, status, url, kind, is_demo) VALUES
 (1, 'canvas',      '5001', 'Lab 3: Python-løkker og lister',     'Informasjonsteknologi 1', now() + interval '2 days', 'todo',      'https://canvas.instructure.com/courses/101/assignments/5001', 'assignment', true),
 (1, 'canvas',      '5004', 'Essay: Demokrati i Norge',           'Samfunnskunnskap',        now() - interval '2 days', 'overdue',   'https://canvas.instructure.com/courses/101/assignments/5004', 'assignment', true),
 (1, 'teams',       't-2',  'Oppgaver 2.10–2.25: Lineære funksjoner', 'Matematikk 1P',       now() + interval '1 day',  'todo',      NULL, 'assignment', true),
 (1, 'teams',       't-4',  'Innlevering: Prosentregning',        'Matematikk 1P',           now() - interval '3 days', 'graded',    NULL, 'assignment', true),
 (1, 'itslearning', '9102', 'Naturfag: Labrapport fotosyntese',   'Naturfag 1FKB',           now() + interval '2 days', 'todo',      NULL, 'assignment', true),
 (1, 'inschool',    '7001', 'Heldagsprøve matematikk',            'Matematikk',              now() + interval '9 days', 'todo',      NULL, 'exam',       true);

-- Чат по ДЗ «Lineære funksjoner»
INSERT INTO chat_sessions (id, user_id, assignment_id, title, topic)
SELECT 1, 1, id, 'Lineære funksjoner', 'lineære funksjoner' FROM assignments WHERE external_id = 't-2';
SELECT setval('chat_sessions_id_seq', 1);
INSERT INTO chat_messages (session_id, role, content, on_topic, created_at) VALUES
 (1, 'student', 'Hva betyr stigningstallet i y = 2x + 3?', NULL, now() - interval '50 minutes'),
 (1, 'tutor',   'Stigningstallet er 2: når x øker med 1, øker y med 2. Hva tror du konstantleddet 3 betyr?', true, now() - interval '49 minutes'),
 (1, 'student', 'At linja krysser y-aksen i 3?', NULL, now() - interval '47 minutes'),
 (1, 'tutor',   'Helt riktig! Vil du prøve en kort quiz om dette?', true, now() - interval '46 minutes');

-- Упражнения и квиз
INSERT INTO exercises (id, user_id, topic, level, statement, correct_answer, answer_type, options) VALUES
 (1, 1, 'lineære funksjoner', 'beginner', 'Hva er stigningstallet i y = -4x + 1?', '-4', 'exact', '["-4","1","4","-1"]'),
 (2, 1, 'lineære funksjoner', 'beginner', 'Hvor krysser y = 3x - 2 y-aksen?', '-2', 'exact', '["3","-2","2","0"]'),
 (3, 1, 'lineære funksjoner', 'intermediate', 'Forklar hva et negativt stigningstall betyr.', 'Grafen synker: y minker når x øker.', 'free', NULL);
SELECT setval('exercises_id_seq', 3);

INSERT INTO quizzes (id, user_id, chat_session_id, title, topic, level, started_at)
VALUES (1, 1, 1, 'Quiz: Lineære funksjoner', 'lineære funksjoner', 'beginner', now() - interval '40 minutes');
SELECT setval('quizzes_id_seq', 1);
INSERT INTO quiz_questions (quiz_id, position, exercise_id) VALUES (1, 1, 1), (1, 2, 2), (1, 3, 3);
INSERT INTO exercise_attempts (user_id, exercise_id, quiz_id, answer, verdict, explanation, created_at) VALUES
 (1, 1, 1, '-4', 'correct',   'Riktig.', now() - interval '39 minutes'),
 (1, 2, 1, '3',  'incorrect', 'Se på konstantleddet, ikke tallet foran x.', now() - interval '38 minutes'),
 (1, 2, 1, '-2', 'correct',   'Riktig nå.', now() - interval '37 minutes'),
 (1, 3, 1, 'At grafen går ned', 'partial', 'Bra start. Hva skjer med y når x øker?', now() - interval '36 minutes');
SELECT finish_quiz(1);

-- История пользователя
INSERT INTO user_events (user_id, type, ref_table, ref_id, payload, created_at) VALUES
 (1, 'login',             NULL,            NULL, '{}',                         now() - interval '1 hour'),
 (1, 'homework_synced',   NULL,            NULL, '{"platforms":4,"count":6}',  now() - interval '55 minutes'),
 (1, 'chat_started',      'chat_sessions', 1,    '{}',                         now() - interval '50 minutes'),
 (1, 'quiz_started',      'quizzes',       1,    '{}',                         now() - interval '40 minutes'),
 (1, 'exercise_answered', 'exercises',     1,    '{"verdict":"correct"}',      now() - interval '39 minutes'),
 (1, 'quiz_completed',    'quizzes',       1,    '{}',                         now() - interval '35 minutes'),
 (1, 'achievement_unlocked', 'achievements', NULL, '{"code":"first_quiz"}',    now() - interval '35 minutes');

INSERT INTO user_achievements (user_id, achievement_code) VALUES (1, 'first_chat'), (1, 'first_quiz'), (1, 'connected_all');

INSERT INTO reminders (user_id, assignment_id, message, remind_at)
SELECT 1, id, 'Frist i morgen: ' || title, due_at - interval '1 day' FROM assignments WHERE due_at > now();

INSERT INTO flashcards (user_id, topic, front, back) VALUES
 (1, 'lineære funksjoner', 'Stigningstall', 'Hvor mye y endrer seg når x øker med 1'),
 (1, 'lineære funksjoner', 'Konstantledd',  'Der grafen krysser y-aksen');

COMMIT;
