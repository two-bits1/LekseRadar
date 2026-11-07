package com.lekseradar.model.converter;

import com.lekseradar.model.Platform;
import jakarta.persistence.Converter;

/** Platform ↔ PostgreSQL-enum platform. */
@Converter(autoApply = true)
public class PlatformConverter extends DbEnumConverter<Platform> {
    public PlatformConverter() {
        super(Platform.class);
    }
}
