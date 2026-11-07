-- LekseRadar, PostgreSQL 16+. Миграции в формате Flyway (Spring Boot: src/main/resources/db/migration).
-- Владелец данных: Java API. Python-сервисы (ИИ и сбор ДЗ) ничего не хранят.

CREATE EXTENSION IF NOT EXISTS citext;

-- Пользователи и вход
CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          CITEXT      NOT NULL UNIQUE,
    password_hash  TEXT        NOT NULL,              -- bcrypt из Spring Security
    display_name   TEXT        NOT NULL,
    locale         TEXT        NOT NULL DEFAULT 'nb', -- язык интерфейса и тьютора
    timezone       TEXT        NOT NULL DEFAULT 'Europe/Oslo',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at  TIMESTAMPTZ
);
