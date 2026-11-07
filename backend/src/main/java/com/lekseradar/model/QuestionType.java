package com.lekseradar.model;

/** Колонка {@code exercises.question_type} (TEXT): тип вопроса квиза из JSON ИИ. */
public enum QuestionType implements DbEnum {
    SINGLE_CHOICE("single_choice"),
    MULTIPLE_CHOICE("multiple_choice"),
    TEXT("text");

    private final String value;

    QuestionType(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static QuestionType fromValue(String value) {
        return DbEnum.fromValue(QuestionType.class, value);
    }
}
