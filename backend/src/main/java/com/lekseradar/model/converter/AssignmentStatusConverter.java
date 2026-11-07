package com.lekseradar.model.converter;

import com.lekseradar.model.enums.AssignmentStatusEnum;
import jakarta.persistence.Converter;

/** AssignmentStatusEnum ↔ PostgreSQL-enum assignment_status. */
@Converter(autoApply = true)
public class AssignmentStatusConverter extends DbEnumConverter<AssignmentStatusEnum> {
    public AssignmentStatusConverter() {
        super(AssignmentStatusEnum.class);
    }
}
