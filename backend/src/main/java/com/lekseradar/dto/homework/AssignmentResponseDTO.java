package com.lekseradar.dto.homework;

import com.lekseradar.model.AssignmentEntity;
import com.lekseradar.model.enums.AssignmentStatusEnum;
import com.lekseradar.model.enums.PlatformEnum;

import java.time.Instant;

/**
 * Домашнее задание с платформы ({@code GET /homework}).
 *
 * @param kind assignment | exam | quiz | …
 */
public record AssignmentResponseDTO(Long id, PlatformEnum source, String title, String course, Instant dueAt,
                                 AssignmentStatusEnum status, String url, String kind) {

    public static AssignmentResponseDTO from(AssignmentEntity a) {
        return new AssignmentResponseDTO(a.getId(), a.getSource(), a.getTitle(), a.getCourse(), a.getDueAt(),
                a.getStatus(), a.getUrl(), a.getKind());
    }
}
