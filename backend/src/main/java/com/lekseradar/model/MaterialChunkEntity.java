package com.lekseradar.model;

import com.lekseradar.model.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

/**
 * Таблица {@code material_chunks} (V10): фрагмент материала для полнотекстового поиска.
 * Колонка tsv (tsvector) в Java не нужна: ищет функция search_materials.
 */
@Entity
@Immutable
@Table(name = "material_chunks")
public class MaterialChunkEntity {
    @Id
    private Long id;

    @Column(name = "material_id")
    private Long materialId;

    private Integer position;

    private String content;

    protected MaterialChunkEntity() {}

    public Long getId() { return id; }
    public Long getMaterialId() { return materialId; }
    public Integer getPosition() { return position; }
    public String getContent() { return content; }
}
