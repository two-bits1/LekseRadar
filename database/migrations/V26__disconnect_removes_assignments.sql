-- Отключение платформы удаляет её задания. Раньше DELETE /platforms/{platform} удалял только доступ, и после
-- отключения (например, Google Classroom) ученик продолжал видеть задания этой платформы.
-- Чаты, квизы и карточки ученика остаются: их ссылка на задание обнуляется (ON DELETE SET NULL),
-- напоминания по этим заданиям удаляются (ON DELETE CASCADE). Файлы ДЗ, пришедшие с платформы
-- (materials.source = 'platform', привязаны к заданию), удаляются вместе с заданием.

CREATE FUNCTION remove_platform_assignments(p_user BIGINT, p_source platform)
RETURNS INT
LANGUAGE plpgsql AS $$
DECLARE
    n INT;
BEGIN
    DELETE FROM materials m
     USING assignments a
     WHERE m.assignment_id = a.id AND m.source = 'platform'
       AND a.user_id = p_user AND a.source = p_source;
    DELETE FROM assignments WHERE user_id = p_user AND source = p_source;
    GET DIAGNOSTICS n = ROW_COUNT;
    RETURN n;
END $$;

COMMENT ON FUNCTION remove_platform_assignments(BIGINT, platform) IS
    'Задания ученика с этой платформы и их файлы с платформы: удалить (платформу отключили). Возвращает число заданий.';

-- Задания платформ, отключённых до V26: подключения нет — задания удаляются.
-- (Задания, записанные в обход сайта через homework-collector/homework/to_db.py без подключения, тоже удалятся.)
DELETE FROM materials m
 USING assignments a
 WHERE m.assignment_id = a.id AND m.source = 'platform'
   AND NOT EXISTS (SELECT 1 FROM platform_connections c WHERE c.user_id = a.user_id AND c.platform = a.source);

DELETE FROM assignments a
 WHERE NOT EXISTS (SELECT 1 FROM platform_connections c WHERE c.user_id = a.user_id AND c.platform = a.source);
