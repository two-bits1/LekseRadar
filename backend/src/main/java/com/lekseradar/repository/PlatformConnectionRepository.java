package com.lekseradar.repository;

import com.lekseradar.model.enums.PlatformEnum;
import com.lekseradar.model.PlatformConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PlatformConnectionRepository extends JpaRepository<PlatformConnectionEntity, Long> {
    List<PlatformConnectionEntity> findByUserIdOrderByPlatform(Long userId);

    Optional<PlatformConnectionEntity> findByUserIdAndPlatform(Long userId, PlatformEnum platform);

    Optional<PlatformConnectionEntity> findByIdAndUserId(Long id, Long userId);

    /** Ученики для обновления по расписанию: заходили после {@code since} и есть рабочее подключение. */
    @Query("""
            SELECT DISTINCT c.userId FROM PlatformConnectionEntity c, UserEntity u
            WHERE u.id = c.userId AND c.needsReconnect = false AND u.lastLoginAt > :since
            """)
    List<Long> findUserIdsToSync(@Param("since") Instant since);
}
