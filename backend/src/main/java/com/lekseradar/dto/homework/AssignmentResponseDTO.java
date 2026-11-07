package com.lekseradar.dto.homework;

import com.lekseradar.model.AssignmentEntity;
import com.lekseradar.model.enums.AssignmentStatusEnum;
import com.lekseradar.model.enums.PlatformEnum;

import java.time.Instant;

/**
 * Домашнее задание с платформы ({@code GET /homework}, {@code GET /homework/{id}}).
 *
 * @param status      todo с прошедшим дедлайном отдаётся как overdue (как на дашборде, V18)
 * @param kind        assignment | exam | quiz | …
 * @param description текст задания; в списке обрезан до 300 символов
 */
public record AssignmentResponseDTO(Long id, PlatformEnum source, String title, String course, Instant dueAt,
                                 AssignmentStatusEnum status, String url, String kind, String description) {

    public static AssignmentResponseDTO from(AssignmentEntity a) {
        return from(a, Integer.MAX_VALUE);
    }

    public static AssignmentResponseDTO from(AssignmentEntity a, int maxDescription) {
        AssignmentStatusEnum status = a.getStatus() == AssignmentStatusEnum.TODO
                && a.getDueAt() != null && a.getDueAt().isBefore(Instant.now())
                ? AssignmentStatusEnum.OVERDUE : a.getStatus();
        String d = a.getDescription() == null ? "" : a.getDescription();
        if (d.length() > maxDescription) d = d.substring(0, maxDescription).stripTrailing() + "…";
        return new AssignmentResponseDTO(a.getId(), a.getSource(), a.getTitle(), a.getCourse(), a.getDueAt(),
                status, a.getUrl(), a.getKind(), d);
    }
}
