package com.lekseradar.repository;

import com.lekseradar.model.ReminderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<ReminderEntity, Long> {
    List<ReminderEntity> findByUserIdOrderByRemindAt(Long userId);

    Optional<ReminderEntity> findByIdAndUserId(Long id, Long userId);

    /** К отправке: ещё не отправлены и время пришло. */
    @Query("SELECT r FROM ReminderEntity r WHERE r.sentAt IS NULL AND r.remindAt <= :now ORDER BY r.remindAt")
    List<ReminderEntity> findDue(@Param("now") Instant now);
}
