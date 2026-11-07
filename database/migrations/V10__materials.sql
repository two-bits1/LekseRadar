-- Учебные материалы для ИИ-репетитора (RAG без векторов).
-- Учитель или ученик загружает файл (PDF, DOCX, конспект, ссылка). Бэкенд достаёт из него текст
-- и вызывает add_material(...). Текст режется на фрагменты (material_chunks) с полнотекстовым индексом.
-- Перед ответом ИИ бэкенд вызывает materials_for_prompt(...) и вставляет результат в {{materials}}.
-- Сам файл хранится вне базы (диск / S3), в базе только storage_key.

CREATE TYPE material_source AS ENUM ('upload', 'url', 'platform', 'teacher', 'seed');

CREATE TABLE materials (
    id           BIGSERIAL PRIMARY KEY,
    owner_id     BIGINT REFERENCES users(id) ON DELETE CASCADE,   -- NULL = общий материал для всех
    subject_id   BIGINT REFERENCES subjects(id),
    topic        TEXT,
    topic_key    TEXT GENERATED ALWAYS AS (norm_key(topic)) STORED,
    title        TEXT NOT NULL CHECK (length(title) BETWEEN 1 AND 300),
    file_name    TEXT,
    mime_type    TEXT,
    file_size    BIGINT,
    storage_key  TEXT,                         -- где лежит оригинал файла
    source       material_source NOT NULL DEFAULT 'upload',
    source_url   TEXT,
    language     TEXT NOT NULL DEFAULT 'nb',
    content_hash TEXT NOT NULL,                -- md5 текста: повторная загрузка не дублирует
    char_count   INT  NOT NULL DEFAULT 0,
    chunk_count  INT  NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX materials_dedup ON materials (coalesce(owner_id, 0), content_hash);
CREATE INDEX materials_owner ON materials (owner_id, created_at DESC);
CREATE INDEX materials_subject ON materials (subject_id, topic_key);

CREATE TABLE material_chunks (
    id          BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    position    INT    NOT NULL,
    content     TEXT   NOT NULL,
    -- 'simple' без стемминга: работает одинаково для nb, en, ru, uk
    tsv         TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED,
    UNIQUE (material_id, position)
);
CREATE INDEX material_chunks_tsv ON material_chunks USING GIN (tsv);

-- Какие материалы ИИ использовал при генерации квиза
ALTER TABLE quiz_sets ADD COLUMN material_ids BIGINT[] NOT NULL DEFAULT '{}';
