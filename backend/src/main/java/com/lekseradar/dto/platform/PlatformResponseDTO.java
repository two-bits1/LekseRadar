package com.lekseradar.dto.platform;

import com.lekseradar.model.enums.PlatformEnum;
import com.lekseradar.model.PlatformConnectionEntity;

import java.time.Instant;

/**
 * Состояние подключения платформы ({@code GET /platforms}). Учётные данные наружу не отдаются.
 */
public record PlatformResponseDTO(PlatformEnum platform, boolean connected, boolean needsReconnect,
                               Instant lastSyncedAt, String lastError) {

    public static PlatformResponseDTO from(PlatformConnectionEntity c) {
        return new PlatformResponseDTO(c.getPlatform(), true, c.isNeedsReconnect(), c.getLastSyncedAt(),
                c.getLastError());
    }

    /** Платформа, которую пользователь ещё не подключал. */
    public static PlatformResponseDTO notConnected(PlatformEnum platform) {
        return new PlatformResponseDTO(platform, false, false, null, null);
    }
}
