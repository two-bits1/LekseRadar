package com.lekseradar.repository;

import com.lekseradar.model.MaterialChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialChunkRepository extends JpaRepository<MaterialChunkEntity, Long> {
    List<MaterialChunkEntity> findByMaterialIdOrderByPosition(Long materialId);
}
