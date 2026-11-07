package com.lekseradar.model.converter;

import com.lekseradar.model.enums.MaterialSourceEnum;
import jakarta.persistence.Converter;

/** MaterialSourceEnum ↔ PostgreSQL-enum material_source. */
@Converter(autoApply = true)
public class MaterialSourceConverter extends DbEnumConverter<MaterialSourceEnum> {
    public MaterialSourceConverter() {
        super(MaterialSourceEnum.class);
    }
}
