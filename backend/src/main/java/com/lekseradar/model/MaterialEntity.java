package com.lekseradar.model;

import com.lekseradar.model.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Таблица {@code materials} (V10, V14): учебный файл для ИИ-репетитора. Создаёт add_material
 * (режет текст на фрагменты material_chunks) или attach_homework_file (файл ДЗ).
 * {@code ownerId == null} — общий материал из ai-content/materials.
 */
@Entity
@Immutable
@Table(name = "materials")
public class MaterialEntity {
    @Id
    private Long id;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "assignment_id")
    private Long assignmentId;

    private String topic;

    @Column(name = "topic_key")
    private String topicKey;

    private String title;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(columnDefinition = "material_source")
    private MaterialSourceEnum source;

    @Column(name = "source_url")
    private String sourceUrl;

    private String language;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "char_count")
    private Integer charCount;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "created_at")
    private Instant createdAt;

    protected MaterialEntity() {}

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public Long getSubjectId() { return subjectId; }
    public Long getAssignmentId() { return assignmentId; }
    public String getTopic() { return topic; }
    public String getTopicKey() { return topicKey; }
    public String getTitle() { return title; }
    public String getFileName() { return fileName; }
    public String getMimeType() { return mimeType; }
    public Long getFileSize() { return fileSize; }
    public String getStorageKey() { return storageKey; }
    public MaterialSourceEnum getSource() { return source; }
    public String getSourceUrl() { return sourceUrl; }
    public String getLanguage() { return language; }
    public String getContentHash() { return contentHash; }
    public Integer getCharCount() { return charCount; }
    public Integer getChunkCount() { return chunkCount; }
    public Instant getCreatedAt() { return createdAt; }
}
