package com.lekseradar.dto.material;

import com.lekseradar.model.MaterialEntity;
import com.lekseradar.model.enums.MaterialSource;

import java.time.Instant;

/**
 * Учебный материал ({@code GET /materials}).
 *
 * @param shared true — общий материал (owner_id = null), виден всем
 */
public record MaterialResponse(Long id, String title, String subject, String topic, String fileName,
                               String mimeType, Long fileSize, MaterialSource source, int chunkCount,
                               boolean shared, Instant createdAt) {

    /** @param subject название предмета (subjects.name) или null */
    public static MaterialResponse from(MaterialEntity m, String subject) {
        return new MaterialResponse(m.getId(), m.getTitle(), subject, m.getTopic(), m.getFileName(),
                m.getMimeType(), m.getFileSize(), m.getSource(),
                m.getChunkCount() == null ? 0 : m.getChunkCount(), m.getOwnerId() == null, m.getCreatedAt());
    }
}
