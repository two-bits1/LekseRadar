package com.lekseradar.dto.quiz;

import com.lekseradar.model.SubjectEntity;

public record SubjectResponseDTO(Long id, String name) {

    public static SubjectResponseDTO from(SubjectEntity s) {
        return new SubjectResponseDTO(s.getId(), s.getName());
    }
}
