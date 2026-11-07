package com.lekseradar.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Таблица {@code users} (database/migrations/V1__users.sql). */
@Entity
@Table(name = "users")
public class User {
    public static final String DEFAULT_LOCALE = "nb";
    public static final String DEFAULT_TIMEZONE = "Europe/Oslo";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CITEXT в базе: сравнение без учёта регистра. */
    @Column(nullable = false, unique = true, columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    /** Язык интерфейса и тьютора: nb, en, ru… */
    @Column(nullable = false)
    private String locale = DEFAULT_LOCALE;

    @Column(nullable = false)
    private String timezone = DEFAULT_TIMEZONE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected User() {}

    public User(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getLocale() { return locale; }
    public String getTimezone() { return timezone; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setLocale(String locale) { this.locale = locale; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public void markLoggedIn() { this.lastLoginAt = Instant.now(); }
}
