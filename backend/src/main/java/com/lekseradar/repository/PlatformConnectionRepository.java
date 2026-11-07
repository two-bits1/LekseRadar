package com.lekseradar.repository;

import com.lekseradar.model.enums.Platform;
import com.lekseradar.model.PlatformConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformConnectionRepository extends JpaRepository<PlatformConnectionEntity, Long> {
    List<PlatformConnectionEntity> findByUserIdOrderByPlatform(Long userId);

    Optional<PlatformConnectionEntity> findByUserIdAndPlatform(Long userId, Platform platform);

    Optional<PlatformConnectionEntity> findByIdAndUserId(Long id, Long userId);
}
