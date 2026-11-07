package com.lekseradar.dto.quiz;

import com.lekseradar.model.Subject;

public record SubjectResponse(Long id, String name) {

    public static SubjectResponse from(Subject s) {
        return new SubjectResponse(s.getId(), s.getName());
    }
}
