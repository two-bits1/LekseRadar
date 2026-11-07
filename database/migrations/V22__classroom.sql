-- Google Classroom как пятая платформа: подключение (platform_connections) и источник ДЗ (assignments.source).
-- PostgreSQL 12+: ADD VALUE можно в транзакции, если новое значение не используется в ней же.
ALTER TYPE platform ADD VALUE IF NOT EXISTS 'classroom';
