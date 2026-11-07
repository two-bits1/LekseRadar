package com.lekseradar.model.enums;

/** PostgreSQL-тип {@code assignment_status}: статус ДЗ. */
public enum AssignmentStatusEnum implements DbEnum {
    TODO("todo"),
    OVERDUE("overdue"),
    SUBMITTED("submitted"),
    GRADED("graded");

    private final String value;

    AssignmentStatusEnum(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static AssignmentStatusEnum fromValue(String value) {
        return DbEnum.fromValue(AssignmentStatusEnum.class, value);
    }
}
