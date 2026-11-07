-- «Выучено» на дашборде и в карточках: раньше считалось только с интервалом от 3 недель, поэтому после
-- занятия ученик видел 0 и думал, что прогресс не сохранился. Теперь выучена карточка, на которую
-- последний раз ответили «помню» (интервал от 1 дня); «не помню» возвращает её в невыученные.
CREATE OR REPLACE VIEW user_flashcard_stats AS
SELECT user_id,
       count(*)                                          AS cards,
       count(*) FILTER (WHERE next_review_at <= now())   AS due_now,
       count(*) FILTER (WHERE interval_days >= 1)        AS learned,
       sum(reviews)                                      AS reviews
FROM flashcard_progress
GROUP BY user_id;
