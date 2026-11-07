package com.lekseradar.model.converter;

import com.lekseradar.model.enums.DbEnum;
import jakarta.persistence.AttributeConverter;

/**
 * Общий конвертер DbEnum ↔ строка в базе. Для колонок PostgreSQL-ENUM у поля ещё нужен
 * {@code @ColumnTransformer(write = "?::<тип>")}, иначе драйвер отправит varchar и база откажет.
 */
public abstract class DbEnumConverter<E extends Enum<E> & DbEnum> implements AttributeConverter<E, String> {
    private final Class<E> type;

    protected DbEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        return value == null ? null : value.value();
    }

    @Override
    public E convertToEntityAttribute(String value) {
        return value == null ? null : DbEnum.fromValue(type, value);
    }
}
