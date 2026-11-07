package com.lekseradar.model.converter;

import com.lekseradar.model.enums.Verdict;
import jakarta.persistence.Converter;

/** Verdict ↔ PostgreSQL-enum verdict. */
@Converter(autoApply = true)
public class VerdictConverter extends DbEnumConverter<Verdict> {
    public VerdictConverter() {
        super(Verdict.class);
    }
}
