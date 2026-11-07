package com.lekseradar.model.converter;

import com.lekseradar.model.enums.VerdictEnum;
import jakarta.persistence.Converter;

/** VerdictEnum ↔ PostgreSQL-enum verdict. */
@Converter(autoApply = true)
public class VerdictConverter extends DbEnumConverter<VerdictEnum> {
    public VerdictConverter() {
        super(VerdictEnum.class);
    }
}
