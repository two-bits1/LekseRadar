package com.lekseradar.repository;

import com.lekseradar.model.UserEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Таблица {@code user_events} (V5): лента действий пользователя. Запись через {@link #add},
 * payload остаётся значением по умолчанию '{}'.
 */
public interface UserEventRepository extends JpaRepository<UserEventEntity, Long> {

    /** Лента: новые сверху, например PageRequest.of(0, 50). */
    List<UserEventEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable page);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO user_events (user_id, type, ref_table, ref_id) VALUES (:userId, :type, :refTable, :refId)",
            nativeQuery = true)
    void add(@Param("userId") Long userId, @Param("type") String type, @Param("refTable") String refTable,
             @Param("refId") Long refId);
}
