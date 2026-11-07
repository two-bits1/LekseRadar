package com.lekseradar.model;

import com.lekseradar.model.enums.*;
import jakarta.persistence.*;

import java.time.Instant;

/** Таблица {@code reminders} (V6): напоминание пользователю (по ДЗ или своё). */
@Entity
@Table(name = "reminders")
public class ReminderEntity {
    public static final String CHANNEL_IN_APP = "in_app";
    public static final String CHANNEL_EMAIL = "email";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(nullable = false)
    private String message;

    @Column(name = "remind_at", nullable = false)
    private Instant remindAt;

    @Column(nullable = false)
    private String channel = CHANNEL_IN_APP;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ReminderEntity() {}

    public ReminderEntity(Long userId, Long assignmentId, String message, Instant remindAt) {
        this.userId = userId;
        this.assignmentId = assignmentId;
        this.message = message;
        this.remindAt = remindAt;
    }

    /** Уже отправленное напоминание (например, письмо): sent_at = remind_at = сейчас. */
    public static ReminderEntity sent(Long userId, Long assignmentId, String message, String channel) {
        ReminderEntity r = new ReminderEntity(userId, assignmentId, message, Instant.now());
        r.channel = channel;
        r.sentAt = r.remindAt;
        return r;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getAssignmentId() { return assignmentId; }
    public String getMessage() { return message; }
    public Instant getRemindAt() { return remindAt; }
    public String getChannel() { return channel; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setMessage(String message) { this.message = message; }
    public void setRemindAt(Instant remindAt) { this.remindAt = remindAt; }
    public void markSent() { this.sentAt = Instant.now(); }
}
