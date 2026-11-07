package com.lekseradar.model.converter;

import com.lekseradar.model.enums.MaterialSource;
import jakarta.persistence.Converter;

/** MaterialSource ↔ PostgreSQL-enum material_source. */
@Converter(autoApply = true)
public class MaterialSourceConverter extends DbEnumConverter<MaterialSource> {
    public MaterialSourceConverter() {
        super(MaterialSource.class);
    }
}
