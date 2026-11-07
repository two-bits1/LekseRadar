-- Сбор ДЗ: подключения платформ и задания в формате homework-collector (collect()).

CREATE TYPE platform          AS ENUM ('canvas', 'teams', 'itslearning', 'inschool');
CREATE TYPE assignment_status AS ENUM ('todo', 'overdue', 'submitted', 'graded');

CREATE TABLE platform_connections (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform         platform    NOT NULL,
    -- То, что collect() принимает для этой платформы (токены, cookie, base_url), в зашифрованном виде.
    -- Шифрует Java перед записью; после collect() заменяет на updated_credentials.
    credentials_enc  BYTEA       NOT NULL,
    needs_reconnect  BOOLEAN     NOT NULL DEFAULT false,  -- SourceResult.needs_reconnect
    last_synced_at   TIMESTAMPTZ,
    last_error       TEXT,                                -- SourceResult.error
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, platform)
);

CREATE TABLE assignments (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source        platform          NOT NULL,
    external_id   TEXT              NOT NULL,         -- Assignment.id из collect()
    title         TEXT              NOT NULL,
    course        TEXT              NOT NULL DEFAULT '',
    due_at        TIMESTAMPTZ,
    status        assignment_status NOT NULL DEFAULT 'todo',
    url           TEXT,
    description   TEXT              NOT NULL DEFAULT '',
    kind          TEXT              NOT NULL DEFAULT 'assignment',  -- assignment|quiz|exam|discussion|task
    is_demo       BOOLEAN           NOT NULL DEFAULT false,
    first_seen_at TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ       NOT NULL DEFAULT now(),
    UNIQUE (user_id, source, external_id)             -- для UPSERT при каждой синхронизации
);
CREATE INDEX assignments_user_due ON assignments (user_id, status, due_at);
