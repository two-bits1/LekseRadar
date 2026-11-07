package com.lekseradar.repository;

import com.lekseradar.model.Platform;
import com.lekseradar.model.PlatformConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformConnectionRepository extends JpaRepository<PlatformConnection, Long> {
    List<PlatformConnection> findByUserIdOrderByPlatform(Long userId);

    Optional<PlatformConnection> findByUserIdAndPlatform(Long userId, Platform platform);

    Optional<PlatformConnection> findByIdAndUserId(Long id, Long userId);
}
