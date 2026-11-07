package com.lekseradar.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Таблица {@code user_events} (database/migrations/V5__history_progress.sql): лента действий пользователя.
 * Только запись, поэтому без JPA-сущности; payload остаётся значением по умолчанию '{}'.
 */
@Repository
public class UserEventRepository {
    private final JdbcTemplate jdbc;

    public UserEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void add(Long userId, String type, String refTable, Long refId) {
        jdbc.update("INSERT INTO user_events (user_id, type, ref_table, ref_id) VALUES (?, ?, ?, ?)",
                userId, type, refTable, refId);
    }
}
