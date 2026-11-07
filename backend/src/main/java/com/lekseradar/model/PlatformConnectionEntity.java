package com.lekseradar.model;

import com.lekseradar.model.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;

/**
 * Таблица {@code platform_connections} (V2): подключённая учебная платформа пользователя.
 * {@code credentials_enc} — токены/cookie, зашифрованные бэкендом (AES-GCM) перед записью.
 * После каждого collect() сюда записываются updated_credentials.
 */
@Entity
@Table(name = "platform_connections")
public class PlatformConnectionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @ColumnTransformer(write = "?::platform")
    @Column(nullable = false, updatable = false, columnDefinition = "platform")
    private PlatformEnum platform;

    @Column(name = "credentials_enc", nullable = false)
    private byte[] credentialsEnc;

    @Column(name = "needs_reconnect", nullable = false)
    private boolean needsReconnect;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected PlatformConnectionEntity() {}

    public PlatformConnectionEntity(Long userId, PlatformEnum platform, byte[] credentialsEnc) {
        this.userId = userId;
        this.platform = platform;
        this.credentialsEnc = credentialsEnc;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public PlatformEnum getPlatform() { return platform; }
    public byte[] getCredentialsEnc() { return credentialsEnc; }
    public boolean isNeedsReconnect() { return needsReconnect; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }

    public void setCredentialsEnc(byte[] credentialsEnc) { this.credentialsEnc = credentialsEnc; }

    /** Итог синхронизации одной платформы из ответа collect() (sources[platform]). Время — только при успехе. */
    public void markSynced(boolean ok, String error, boolean needsReconnect) {
        if (ok) this.lastSyncedAt = Instant.now();
        this.lastError = ok ? null : error;
        this.needsReconnect = !ok && needsReconnect;
    }

    /** Новое или повторное подключение: доступ проверен и ДЗ уже собраны. */
    public void connected(byte[] credentialsEnc) {
        this.credentialsEnc = credentialsEnc;
        markSynced(true, null, false);
    }
}
