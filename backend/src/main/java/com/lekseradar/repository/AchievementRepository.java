package com.lekseradar.repository;

import com.lekseradar.model.AchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<AchievementEntity, String> {
}
