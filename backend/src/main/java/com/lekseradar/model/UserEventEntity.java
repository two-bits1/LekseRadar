package com.lekseradar.model;

import com.lekseradar.model.enums.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Таблица {@code user_events} (V5): история действий пользователя (лента).
 * Запись — {@link com.lekseradar.repository.UserEventRepository#add}; функции базы тоже пишут сюда сами.
 */
@Entity
@Immutable
@Table(name = "user_events")
public class UserEventEntity {
    @Id
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    /** register, login, chat_created, quiz_started… */
    private String type;

    @Column(name = "ref_table")
    private String refTable;

    @Column(name = "ref_id")
    private Long refId;

    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload;

    @Column(name = "created_at")
    private Instant createdAt;

    protected UserEventEntity() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getType() { return type; }
    public String getRefTable() { return refTable; }
    public Long getRefId() { return refId; }
    public JsonNode getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
}
