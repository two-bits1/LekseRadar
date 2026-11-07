package com.lekseradar.model.converter;

import com.lekseradar.model.enums.PlatformEnum;
import jakarta.persistence.Converter;

/** PlatformEnum ↔ PostgreSQL-enum platform. */
@Converter(autoApply = true)
public class PlatformConverter extends DbEnumConverter<PlatformEnum> {
    public PlatformConverter() {
        super(PlatformEnum.class);
    }
}
