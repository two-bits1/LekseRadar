package com.lekseradar.model.converter;

import com.lekseradar.model.enums.AssignmentStatus;
import jakarta.persistence.Converter;

/** AssignmentStatus ↔ PostgreSQL-enum assignment_status. */
@Converter(autoApply = true)
public class AssignmentStatusConverter extends DbEnumConverter<AssignmentStatus> {
    public AssignmentStatusConverter() {
        super(AssignmentStatus.class);
    }
}
