package com.lekseradar.model;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Таблица {@code user_achievements} (V6): какие ачивки и когда получил пользователь. */
@Entity
@Table(name = "user_achievements")
@IdClass(UserAchievement.Key.class)
public class UserAchievement {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "achievement_code")
    private String achievementCode;

    @Column(name = "unlocked_at", nullable = false, updatable = false)
    private Instant unlockedAt = Instant.now();

    protected UserAchievement() {}

    public UserAchievement(Long userId, String achievementCode) {
        this.userId = userId;
        this.achievementCode = achievementCode;
    }

    public Long getUserId() { return userId; }
    public String getAchievementCode() { return achievementCode; }
    public Instant getUnlockedAt() { return unlockedAt; }

    public static class Key implements Serializable {
        private Long userId;
        private String achievementCode;

        public Key() {}

        public Key(Long userId, String achievementCode) {
            this.userId = userId;
            this.achievementCode = achievementCode;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(userId, k.userId)
                    && Objects.equals(achievementCode, k.achievementCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, achievementCode);
        }
    }
}
