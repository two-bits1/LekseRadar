package com.lekseradar.repository;

import com.lekseradar.model.MaterialChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialChunkRepository extends JpaRepository<MaterialChunk, Long> {
    List<MaterialChunk> findByMaterialIdOrderByPosition(Long materialId);
}
