package com.lekseradar.repository;

import com.lekseradar.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUserIdOrderByRemindAt(Long userId);

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    /** К отправке: ещё не отправлены и время пришло. */
    @Query("SELECT r FROM Reminder r WHERE r.sentAt IS NULL AND r.remindAt <= :now ORDER BY r.remindAt")
    List<Reminder> findDue(@Param("now") Instant now);
}
