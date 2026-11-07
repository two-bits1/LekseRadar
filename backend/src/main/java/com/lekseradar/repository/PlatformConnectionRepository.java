package com.lekseradar.repository;

import com.lekseradar.model.enums.PlatformEnum;
import com.lekseradar.model.PlatformConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformConnectionRepository extends JpaRepository<PlatformConnectionEntity, Long> {
    List<PlatformConnectionEntity> findByUserIdOrderByPlatform(Long userId);

    Optional<PlatformConnectionEntity> findByUserIdAndPlatform(Long userId, PlatformEnum platform);

    Optional<PlatformConnectionEntity> findByIdAndUserId(Long id, Long userId);
}
