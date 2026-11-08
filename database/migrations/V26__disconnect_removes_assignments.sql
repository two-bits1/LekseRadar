-- Отключение платформы удаляет её задания. Раньше DELETE /platforms/{platform} удалял только доступ, и после
-- отключения (например, Google Classroom) ученик продолжал видеть задания этой платформы.
-- Теперь задания удаляет бэкенд (PlatformService.disconnect → AssignmentRepository.removePlatformAssignments),
-- обычным SQL, поэтому бэкенд работает и без этой миграции. Она один раз убирает то, что осталось от прошлых
-- отключений: задания платформ, которых у ученика больше нет в platform_connections.
-- Чаты, квизы и карточки остаются: их ссылка на задание обнуляется (ON DELETE SET NULL), напоминания по этим
-- заданиям удаляются (ON DELETE CASCADE). Файлы ДЗ с платформы (materials.source = 'platform') удаляются.
-- Задания, записанные в обход сайта через homework-collector/homework/to_db.py без подключения, тоже удалятся.

DELETE FROM materials m
 USING assignments a
 WHERE m.assignment_id = a.id AND m.source = 'platform'
   AND NOT EXISTS (SELECT 1 FROM platform_connections c WHERE c.user_id = a.user_id AND c.platform = a.source);

DELETE FROM assignments a
 WHERE NOT EXISTS (SELECT 1 FROM platform_connections c WHERE c.user_id = a.user_id AND c.platform = a.source);
