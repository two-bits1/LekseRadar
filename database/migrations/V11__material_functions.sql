-- Функции для материалов: добавить (с нарезкой на фрагменты), найти фрагменты, собрать текст для промпта.

-- Нарезка текста: по абзацам, склеивая их до ~p_size символов; длинный абзац режется по предложениям.
CREATE FUNCTION split_text(p_text TEXT, p_size INT DEFAULT 1200)
RETURNS TABLE (pos INT, chunk TEXT) LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE
    v_par  TEXT;
    v_sent TEXT;
    v_buf  TEXT := '';
    v_pos  INT  := 0;
BEGIN
    FOR v_par IN
        SELECT btrim(p) FROM regexp_split_to_table(replace(p_text, E'\r', ''), E'\n\\s*\n') AS p
        WHERE btrim(p) <> ''
    LOOP
        IF length(v_par) > p_size THEN
            -- абзац больше лимита: по предложениям
            FOR v_sent IN SELECT s FROM regexp_split_to_table(v_par, E'(?<=[.!?])\\s+') AS s LOOP
                IF length(v_buf) + length(v_sent) + 1 > p_size AND v_buf <> '' THEN
                    pos := v_pos; chunk := v_buf; RETURN NEXT;
                    v_pos := v_pos + 1; v_buf := '';
                END IF;
                -- предложение длиннее лимита режем жёстко
                WHILE length(v_sent) > p_size LOOP
                    pos := v_pos; chunk := left(v_sent, p_size); RETURN NEXT;
                    v_pos := v_pos + 1; v_sent := substr(v_sent, p_size + 1);
                END LOOP;
                v_buf := CASE WHEN v_buf = '' THEN v_sent ELSE v_buf || ' ' || v_sent END;
            END LOOP;
        ELSE
            IF length(v_buf) + length(v_par) + 2 > p_size AND v_buf <> '' THEN
                pos := v_pos; chunk := v_buf; RETURN NEXT;
                v_pos := v_pos + 1; v_buf := '';
            END IF;
            v_buf := CASE WHEN v_buf = '' THEN v_par ELSE v_buf || E'\n\n' || v_par END;
        END IF;
    END LOOP;
    IF v_buf <> '' THEN
        pos := v_pos; chunk := v_buf; RETURN NEXT;
    END IF;
END $$;

-- Добавить материал. p_owner NULL = общий. Предмет создаётся, если его нет.
-- Тот же текст у того же владельца не дублируется: вернётся id существующего.
CREATE FUNCTION add_material(
    p_owner     BIGINT,
    p_subject   TEXT,
    p_topic     TEXT,
    p_title     TEXT,
    p_content   TEXT,
    p_file_name TEXT DEFAULT NULL,
    p_mime_type TEXT DEFAULT NULL,
    p_source    material_source DEFAULT 'upload',
    p_language  TEXT DEFAULT 'nb',
    p_storage_key TEXT DEFAULT NULL,
    p_source_url  TEXT DEFAULT NULL,
    p_chunk_size  INT DEFAULT 1200
) RETURNS BIGINT LANGUAGE plpgsql AS $$
DECLARE
    v_subject BIGINT;
    v_hash    TEXT := md5(coalesce(p_content, ''));
    v_id      BIGINT;
    v_n       INT;
BEGIN
    IF p_content IS NULL OR btrim(p_content) = '' THEN
        RAISE EXCEPTION 'пустой текст материала: сначала извлеки текст из файла';
    END IF;

    SELECT id INTO v_id FROM materials
    WHERE coalesce(owner_id, 0) = coalesce(p_owner, 0) AND content_hash = v_hash;
    IF FOUND THEN RETURN v_id; END IF;

    IF p_subject IS NOT NULL AND btrim(p_subject) <> '' THEN
        INSERT INTO subjects (name, key) VALUES (btrim(p_subject), norm_key(p_subject))
        ON CONFLICT (key) DO NOTHING;
        SELECT id INTO v_subject FROM subjects WHERE key = norm_key(p_subject);
    END IF;

    INSERT INTO materials (owner_id, subject_id, topic, title, file_name, mime_type, file_size,
                           storage_key, source, source_url, language, content_hash, char_count)
    VALUES (p_owner, v_subject, nullif(btrim(p_topic), ''), btrim(p_title), p_file_name, p_mime_type,
            octet_length(p_content), p_storage_key, p_source, p_source_url, p_language, v_hash,
            length(p_content))
    RETURNING id INTO v_id;

    INSERT INTO material_chunks (material_id, position, content)
    SELECT v_id, s.pos, s.chunk FROM split_text(p_content, p_chunk_size) s;
    GET DIAGNOSTICS v_n = ROW_COUNT;
    UPDATE materials SET chunk_count = v_n WHERE id = v_id;

    IF p_owner IS NOT NULL THEN
        INSERT INTO user_events (user_id, type, ref_table, ref_id, payload)
        VALUES (p_owner, 'material_added', 'materials', v_id, jsonb_build_object('title', p_title, 'chunks', v_n));
    END IF;
    RETURN v_id;
END $$;

-- Вопрос ученика → tsquery с ИЛИ между словами (слова короче 3 букв отбрасываются).
CREATE FUNCTION question_tsquery(p_query TEXT) RETURNS tsquery LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE WHEN count(*) = 0 THEN NULL
                ELSE to_tsquery('simple', string_agg(quote_literal(w), ' | ')) END
    FROM unnest(tsvector_to_array(to_tsvector('simple', coalesce(p_query, '')))) AS w
    WHERE length(w) >= 3
$$;

-- Найти фрагменты для ответа. Видны свои материалы и общие (owner_id IS NULL).
-- p_subject_id / p_topic необязательны: если заданы, такие материалы поднимаются выше.
CREATE FUNCTION search_materials(
    p_user       BIGINT,
    p_query      TEXT,
    p_subject_id BIGINT DEFAULT NULL,
    p_limit      INT    DEFAULT 5,
    p_topic      TEXT   DEFAULT NULL
) RETURNS TABLE (chunk_id BIGINT, material_id BIGINT, title TEXT, topic TEXT,
                 chunk_pos INT, content TEXT, score REAL)
LANGUAGE sql STABLE AS $$
    WITH q AS (SELECT question_tsquery(p_query) AS tsq)
    SELECT c.id, m.id, m.title, m.topic, c.position, c.content,
           (ts_rank_cd(c.tsv, q.tsq)
             * CASE WHEN p_subject_id IS NOT NULL AND m.subject_id = p_subject_id THEN 2 ELSE 1 END
             * CASE WHEN p_topic IS NOT NULL AND m.topic_key = norm_key(p_topic) THEN 1.5 ELSE 1 END
           )::REAL AS score
    FROM q
    JOIN material_chunks c ON c.tsv @@ q.tsq
    JOIN materials m ON m.id = c.material_id
    WHERE (m.owner_id = p_user OR m.owner_id IS NULL)
      AND (p_subject_id IS NULL OR m.subject_id = p_subject_id OR m.subject_id IS NULL)
    ORDER BY score DESC, c.id
    LIMIT greatest(p_limit, 1)
$$;

-- Готовый текст для {{materials}} в промпте: "[M<id>] <название> (<тема>)\n<фрагмент>".
-- Если ничего не нашлось, вернёт 'нет материалов по этому вопросу'.
CREATE FUNCTION materials_for_prompt(
    p_user BIGINT, p_query TEXT, p_subject_id BIGINT DEFAULT NULL,
    p_limit INT DEFAULT 5, p_topic TEXT DEFAULT NULL
) RETURNS TEXT LANGUAGE sql STABLE AS $$
    SELECT coalesce(
        string_agg(format('[M%s] %s%s' || E'\n' || '%s',
                          s.material_id, s.title,
                          CASE WHEN s.topic IS NULL THEN '' ELSE ' (' || s.topic || ')' END,
                          s.content),
                   E'\n\n---\n\n' ORDER BY s.score DESC),
        'нет материалов по этому вопросу')
    FROM search_materials(p_user, p_query, p_subject_id, p_limit, p_topic) s
$$;

-- Список материалов пользователя для экрана «Мои материалы»
CREATE VIEW user_materials AS
SELECT m.id, m.owner_id, m.title, s.name AS subject, m.topic, m.file_name, m.mime_type,
       m.file_size, m.source, m.language, m.chunk_count, m.created_at
FROM materials m
LEFT JOIN subjects s ON s.id = m.subject_id;
