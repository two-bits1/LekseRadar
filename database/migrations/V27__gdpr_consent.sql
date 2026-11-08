-- GDPR: когда и с какой версией условий ученик согласился при регистрации (условия и политика конфиденциальности,
-- подтверждение возраста 13+ или согласия родителя). У аккаунтов, созданных до V27, NULL.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS terms_version     TEXT;

COMMENT ON COLUMN users.terms_accepted_at IS 'когда принял условия и политику конфиденциальности (GDPR)';
COMMENT ON COLUMN users.terms_version     IS 'версия условий, которую принял (дата публикации, например 2026-11-08)';

-- Неактивные аккаунты удаляет бэкенд (RetentionScheduler) по last_login_at: индекс для этого поиска
CREATE INDEX IF NOT EXISTS users_last_activity ON users ((coalesce(last_login_at, created_at)));
