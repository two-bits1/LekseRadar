package com.lekseradar.model;

/** PostgreSQL-тип {@code assignment_status}: статус ДЗ. */
public enum AssignmentStatus implements DbEnum {
    TODO("todo"),
    OVERDUE("overdue"),
    SUBMITTED("submitted"),
    GRADED("graded");

    private final String value;

    AssignmentStatus(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static AssignmentStatus fromValue(String value) {
        return DbEnum.fromValue(AssignmentStatus.class, value);
    }
}
