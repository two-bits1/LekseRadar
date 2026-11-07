package com.lekseradar.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

/**
 * Таблица {@code achievements} (V6): справочник ачивок (first_chat, first_quiz…). Это не пробные данные,
 * а список того, что можно получить; заполняет миграция V6.
 */
@Entity
@Immutable
@Table(name = "achievements")
public class Achievement {
    @Id
    private String code;

    private String title;

    private String description;

    private String icon;

    protected Achievement() {}

    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
}
