-- Демо-данные для хакатона: один ученик с ДЗ, чатом, квизом и прогрессом.
-- Запуск после миграций: psql -d lekseradar -f seed.sql  (пароль демо-пользователя: demo1234)

BEGIN;

INSERT INTO users (id, email, password_hash, display_name)
VALUES (1, 'demo@lekseradar.no', '$2a$10$05xlxQCtj63XqgqBM/82NOB/154Pb07HMsEfC5QyQ4WiJLPcyz6Aq', 'Demo Elev');
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

-- Квиз: ИИ прислал JSON (тот же, что quiz/example.json) → импорт → старт → ответы → итог
SELECT import_quiz_json($json${
  "subject": "Matematikk",
  "topic": "Lineære funksjoner",
  "level": "beginner",
  "language": "nb",
  "title": "Quiz: Lineære funksjoner",
  "model": "example",
  "questions": [
    {
      "type": "single_choice",
      "question": "Hva er stigningstallet i y = -4x + 1?",
      "options": ["-4", "1", "4", "-1"],
      "correct_answer": "-4",
      "explanation": "Stigningstallet er tallet foran x."
    },
    {
      "type": "single_choice",
      "question": "Hvor krysser y = 3x - 2 y-aksen?",
      "options": ["3", "-2", "2", "0"],
      "correct_answer": "-2",
      "explanation": "Konstantleddet viser hvor grafen krysser y-aksen."
    },
    {
      "type": "multiple_choice",
      "question": "Hvilke funksjoner er synkende?",
      "options": ["y = -2x + 5", "y = 3x", "y = -x", "y = 4"],
      "correct_answer": ["y = -2x + 5", "y = -x"],
      "explanation": "En lineær funksjon synker når stigningstallet er negativt."
    },
    {
      "type": "text",
      "question": "Forklar med egne ord hva et negativt stigningstall betyr.",
      "correct_answer": "Grafen synker: y blir mindre når x øker.",
      "explanation": "Negativt stigningstall betyr at y minker når x øker."
    }
  ]
}$json$::jsonb, 1);
SELECT start_quiz(1, find_quiz_set('Matematikk', 'Lineære funksjoner', 'beginner'));
UPDATE quizzes SET chat_session_id = 1, started_at = now() - interval '40 minutes' WHERE id = 1;
SELECT verdict FROM save_quiz_answer(1, 1, '-4');
SELECT verdict FROM save_quiz_answer(1, 2, '3');
SELECT verdict FROM save_quiz_answer(1, 2, '-2');
SELECT verdict FROM save_quiz_answer(1, 3, '["y = -2x + 5", "y = -x"]');
SELECT verdict FROM save_quiz_answer(1, 4, 'At grafen går ned', 'partial', 'Bra start. Hva skjer med y når x øker?');
SELECT finish_quiz(1);

-- История пользователя
INSERT INTO user_events (user_id, type, ref_table, ref_id, payload, created_at) VALUES
 (1, 'login',             NULL,            NULL, '{}',                         now() - interval '1 hour'),
 (1, 'homework_synced',   NULL,            NULL, '{"platforms":4,"count":6}',  now() - interval '55 minutes'),
 (1, 'chat_started',      'chat_sessions', 1,    '{}',                         now() - interval '50 minutes'),
 (1, 'quiz_completed',    'quizzes',       1,    '{}',                         now() - interval '35 minutes'),
 (1, 'achievement_unlocked', 'achievements', NULL, '{"code":"first_quiz"}',    now() - interval '35 minutes');

INSERT INTO user_achievements (user_id, achievement_code) VALUES (1, 'first_chat'), (1, 'first_quiz'), (1, 'connected_all');

INSERT INTO reminders (user_id, assignment_id, message, remind_at)
SELECT 1, id, 'Frist i morgen: ' || title, due_at - interval '1 day' FROM assignments WHERE due_at > now();

-- Флеш-карты: весь квиз в колоду (ошибки из квиза уже попали туда сами) и одно повторение
SELECT add_quiz_set_to_flashcards(1, find_quiz_set('Matematikk', 'Lineære funksjoner', 'beginner'));
SELECT interval_days FROM review_flashcard(1, 1, 5);

COMMIT;
