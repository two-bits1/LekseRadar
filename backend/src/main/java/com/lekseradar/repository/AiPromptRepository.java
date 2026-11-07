package com.lekseradar.repository;

import com.lekseradar.model.AiPromptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Промпты ИИ с версиями (V12). Загружает ai-content/load/load.py, бэкенд только читает. */
public interface AiPromptRepository extends JpaRepository<AiPromptEntity, Long> {
    Optional<AiPromptEntity> findByKeyAndActiveTrue(String key);

    List<AiPromptEntity> findByKeyOrderByVersionDesc(String key);

    /** Текст активной версии или ошибка базы, если промпт не загружен. */
    @Query(value = "SELECT get_prompt(:key)", nativeQuery = true)
    String getActiveContent(@Param("key") String key);
}
