-- Промпты для ИИ хранятся в базе с версиями, чтобы менять их без пересборки бэкенда.
-- key: tutor, quiz_generator, answer_checker ... (любое имя файла промпта без .md)
-- Бэкенд берёт текст через get_prompt('tutor') и подставляет переменные {{...}}.

CREATE TABLE ai_prompts (
    id           BIGSERIAL PRIMARY KEY,
    key          TEXT NOT NULL CHECK (key ~ '^[a-z0-9_]+$'),
    version      INT  NOT NULL,
    content      TEXT NOT NULL CHECK (btrim(content) <> ''),
    content_hash TEXT NOT NULL,
    is_active    BOOLEAN NOT NULL DEFAULT true,
    note         TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (key, version)
);
-- Активная версия у ключа ровно одна
CREATE UNIQUE INDEX ai_prompts_one_active ON ai_prompts (key) WHERE is_active;

-- Сохранить промпт. Если текст не изменился, вернёт текущую версию; иначе создаст новую и сделает её активной.
CREATE FUNCTION save_prompt(p_key TEXT, p_content TEXT, p_note TEXT DEFAULT NULL)
RETURNS INT LANGUAGE plpgsql AS $$
DECLARE
    v_hash TEXT := md5(p_content);
    v_cur  ai_prompts%ROWTYPE;
    v_ver  INT;
BEGIN
    SELECT * INTO v_cur FROM ai_prompts WHERE key = p_key AND is_active;
    IF FOUND AND v_cur.content_hash = v_hash THEN
        RETURN v_cur.version;
    END IF;
    SELECT coalesce(max(version), 0) + 1 INTO v_ver FROM ai_prompts WHERE key = p_key;
    UPDATE ai_prompts SET is_active = false WHERE key = p_key AND is_active;
    INSERT INTO ai_prompts (key, version, content, content_hash, note)
    VALUES (p_key, v_ver, p_content, v_hash, p_note);
    RETURN v_ver;
END $$;

-- Текст активной версии (NULL, если такого промпта нет)
CREATE FUNCTION get_prompt(p_key TEXT) RETURNS TEXT LANGUAGE sql STABLE AS $$
    SELECT content FROM ai_prompts WHERE key = p_key AND is_active
$$;

-- Откатиться на старую версию
CREATE FUNCTION activate_prompt(p_key TEXT, p_version INT) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM ai_prompts WHERE key = p_key AND version = p_version) THEN
        RAISE EXCEPTION 'нет промпта % версии %', p_key, p_version;
    END IF;
    UPDATE ai_prompts SET is_active = false WHERE key = p_key AND is_active;
    UPDATE ai_prompts SET is_active = true  WHERE key = p_key AND version = p_version;
END $$;

-- Каким промптом сгенерирован квиз (для разбора плохих квизов)
ALTER TABLE quiz_sets ADD COLUMN prompt_id BIGINT REFERENCES ai_prompts(id);
