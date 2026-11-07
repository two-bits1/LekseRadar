package com.lekseradar.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Таблица {@code subjects} (V7): предмет. Создаётся сам функциями базы (start_chat, import_quiz_json,
 * add_material) по названию; {@code key} — нормализованное имя (norm_key), по нему ищется дубль.
 */
@Entity
@Immutable
@Table(name = "subjects")
public class Subject {
    @Id
    private Long id;

    private String name;

    @Column(name = "key")
    private String key;

    @Column(name = "created_at")
    private Instant createdAt;

    protected Subject() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getKey() { return key; }
    public Instant getCreatedAt() { return createdAt; }
}
