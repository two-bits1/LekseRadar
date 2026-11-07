package com.lekseradar.dto.homework;

import com.lekseradar.model.Assignment;
import com.lekseradar.model.AssignmentStatus;
import com.lekseradar.model.Platform;

import java.time.Instant;

/**
 * Домашнее задание с платформы ({@code GET /homework}).
 *
 * @param kind assignment | exam | quiz | …
 */
public record AssignmentResponse(Long id, Platform source, String title, String course, Instant dueAt,
                                 AssignmentStatus status, String url, String kind) {

    public static AssignmentResponse from(Assignment a) {
        return new AssignmentResponse(a.getId(), a.getSource(), a.getTitle(), a.getCourse(), a.getDueAt(),
                a.getStatus(), a.getUrl(), a.getKind());
    }
}
