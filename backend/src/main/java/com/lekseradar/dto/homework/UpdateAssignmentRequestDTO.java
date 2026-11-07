package com.lekseradar.dto.homework;

import com.lekseradar.model.enums.AssignmentStatusEnum;
import jakarta.validation.constraints.NotNull;

/** {@code PATCH /homework/{id}}: ученик отмечает ДЗ сданным (submitted) или снова «надо сделать» (todo). */
public record UpdateAssignmentRequestDTO(@NotNull AssignmentStatusEnum status) {}
