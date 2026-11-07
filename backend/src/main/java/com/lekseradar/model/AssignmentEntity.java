package com.lekseradar.model;

import com.lekseradar.model.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;

/**
 * Таблица {@code assignments} (V2, V16): ДЗ из homework-collector (collect()).
 * Уникально по (user_id, source, external_id): синхронизация пишет upsert'ом,
 * см. {@link com.lekseradar.repository.AssignmentRepository#upsert}.
 */
@Entity
@Table(name = "assignments")
public class AssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @ColumnTransformer(write = "?::platform")
    @Column(nullable = false, updatable = false, columnDefinition = "platform")
    private PlatformEnum source;

    /** id задания в исходной платформе. */
    @Column(name = "external_id", nullable = false, updatable = false)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String course = "";

    @Column(name = "due_at")
    private Instant dueAt;

    @ColumnTransformer(write = "?::assignment_status")
    @Column(nullable = false, columnDefinition = "assignment_status")
    private AssignmentStatusEnum status = AssignmentStatusEnum.TODO;

    private String url;

    @Column(nullable = false)
    private String description = "";

    /** assignment / quiz / exam / discussion / task. */
    @Column(nullable = false)
    private String kind = "assignment";

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected AssignmentEntity() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public PlatformEnum getSource() { return source; }
    public String getExternalId() { return externalId; }
    public String getTitle() { return title; }
    public String getCourse() { return course; }
    public Instant getDueAt() { return dueAt; }
    public AssignmentStatusEnum getStatus() { return status; }
    public String getUrl() { return url; }
    public String getDescription() { return description; }
    public String getKind() { return kind; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** Ученик сам отметил задание сданным. */
    public void setStatus(AssignmentStatusEnum status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
}
