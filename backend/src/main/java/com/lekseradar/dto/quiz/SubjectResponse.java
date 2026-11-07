package com.lekseradar.dto.quiz;

import com.lekseradar.model.SubjectEntity;

public record SubjectResponse(Long id, String name) {

    public static SubjectResponse from(SubjectEntity s) {
        return new SubjectResponse(s.getId(), s.getName());
    }
}
