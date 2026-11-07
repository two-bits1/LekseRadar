package com.lekseradar.model;

import com.lekseradar.model.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Таблица {@code ai_prompts} (V12): промпт ИИ с версиями. Пишут save_prompt / activate_prompt
 * (загрузка из ai-content/prompts через load.py). Активна одна версия на ключ.
 */
@Entity
@Immutable
@Table(name = "ai_prompts")
public class AiPromptEntity {
    @Id
    private Long id;

    @Column(name = "key")
    private String key;

    private Integer version;

    private String content;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "is_active")
    private boolean active;

    private String note;

    @Column(name = "created_at")
    private Instant createdAt;

    protected AiPromptEntity() {}

    public Long getId() { return id; }
    public String getKey() { return key; }
    public Integer getVersion() { return version; }
    public String getContent() { return content; }
    public String getContentHash() { return contentHash; }
    public boolean isActive() { return active; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
