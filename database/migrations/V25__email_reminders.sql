-- Напоминания о домашке на почту: ученик может их выключить в настройках (по умолчанию включены).
-- Бэкенд раз в 15 минут ищет несданные задания со сроком в ближайшие 24 часа и шлёт одно письмо на ученика.
-- Отправленное напоминание записывается в reminders (channel = 'email'); уникальный индекс не даёт
-- напомнить об одном задании дважды, даже если бэкенд запущен в нескольких копиях.
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_reminders boolean NOT NULL DEFAULT true;

CREATE UNIQUE INDEX IF NOT EXISTS reminders_assignment_email
    ON reminders (assignment_id) WHERE channel = 'email';

-- Задания, о которых пора напомнить письмом: не сдано, срок в окне (now, now + interval], ученик не выключил
-- напоминания и письма об этом задании ещё не было.
CREATE OR REPLACE FUNCTION assignments_to_remind(p_within interval DEFAULT interval '24 hours')
RETURNS SETOF assignments
LANGUAGE sql STABLE AS $$
    SELECT a.*
    FROM assignments a
    JOIN users u ON u.id = a.user_id
    WHERE u.email_reminders
      AND a.status = 'todo'
      AND a.due_at > now()
      AND a.due_at <= now() + p_within
      AND NOT EXISTS (SELECT 1 FROM reminders r WHERE r.assignment_id = a.id AND r.channel = 'email')
    ORDER BY a.user_id, a.due_at;
$$;
