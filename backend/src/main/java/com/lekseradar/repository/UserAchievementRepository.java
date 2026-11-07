package com.lekseradar.repository;

import com.lekseradar.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UserAchievement.Key> {
    List<UserAchievement> findByUserIdOrderByUnlockedAt(Long userId);

    /** Выдать ачивку; повторная выдача ничего не делает. 1 — выдана сейчас, 0 — уже была. */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO user_achievements (user_id, achievement_code) VALUES (:userId, :code)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int unlock(@Param("userId") Long userId, @Param("code") String code);
}
