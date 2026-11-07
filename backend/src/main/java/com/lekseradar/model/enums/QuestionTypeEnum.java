package com.lekseradar.model.enums;

/** Колонка {@code exercises.question_type} (TEXT): тип вопроса квиза из JSON ИИ. */
public enum QuestionTypeEnum implements DbEnum {
    SINGLE_CHOICE("single_choice"),
    MULTIPLE_CHOICE("multiple_choice"),
    TEXT("text");

    private final String value;

    QuestionTypeEnum(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public static QuestionTypeEnum fromValue(String value) {
        return DbEnum.fromValue(QuestionTypeEnum.class, value);
    }
}
