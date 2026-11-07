package com.lekseradar.dto.platform;

import com.lekseradar.model.Platform;
import com.lekseradar.model.PlatformConnection;

import java.time.Instant;

/**
 * Состояние подключения платформы ({@code GET /platforms}). Учётные данные наружу не отдаются.
 */
public record PlatformResponse(Platform platform, boolean connected, boolean needsReconnect,
                               Instant lastSyncedAt, String lastError) {

    public static PlatformResponse from(PlatformConnection c) {
        return new PlatformResponse(c.getPlatform(), true, c.isNeedsReconnect(), c.getLastSyncedAt(),
                c.getLastError());
    }

    /** Платформа, которую пользователь ещё не подключал. */
    public static PlatformResponse notConnected(Platform platform) {
        return new PlatformResponse(platform, false, false, null, null);
    }
}
