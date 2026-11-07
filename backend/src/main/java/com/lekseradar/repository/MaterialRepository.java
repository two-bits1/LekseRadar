package com.lekseradar.repository;

import com.lekseradar.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Учебные материалы для ИИ-репетитора: таблицы materials / material_chunks и функции V11. */
public interface MaterialRepository extends JpaRepository<Material, Long> {

    /** Экран «Материалы»: свои и общие (owner_id IS NULL). */
    @Query("SELECT m FROM Material m WHERE m.ownerId = :userId OR m.ownerId IS NULL ORDER BY m.createdAt DESC")
    List<Material> findVisible(@Param("userId") Long userId);

    /** Свой или общий материал; чужой не отдаётся. */
    @Query("SELECT m FROM Material m WHERE m.id = :id AND (m.ownerId = :userId OR m.ownerId IS NULL)")
    Optional<Material> findVisible(@Param("id") Long id, @Param("userId") Long userId);

    /** Файл, загруженный пользователем (текст уже извлечён бэкендом): id материала. */
    @Transactional
    @Query(value = """
            SELECT add_material(:userId, :subject, :topic, :title, :text,
                                p_file_name => :fileName, p_mime_type => :mimeType, p_storage_key => :storageKey)
            """, nativeQuery = true)
    Long add(@Param("userId") Long userId, @Param("subject") String subject, @Param("topic") String topic,
             @Param("title") String title, @Param("text") String text, @Param("fileName") String fileName,
             @Param("mimeType") String mimeType, @Param("storageKey") String storageKey);

    /** Готовый текст для {{materials}} в промпте тьютора (до limit фрагментов). */
    @Query(value = "SELECT materials_for_prompt(:userId, :question, :subjectId, :limit, :topic)", nativeQuery = true)
    String forPrompt(@Param("userId") Long userId, @Param("question") String question,
                     @Param("subjectId") Long subjectId, @Param("limit") Integer limit, @Param("topic") String topic);

    interface SearchHit {
        Long getChunkId();
        Long getMaterialId();
        String getTitle();
        String getTopic();
        Integer getChunkPos();
        String getContent();
        Float getScore();
    }

    /** То же построчно, для ссылок [M&lt;id&gt;] во фронте. */
    @Query(value = """
            SELECT chunk_id AS chunkId, material_id AS materialId, title, topic, chunk_pos AS chunkPos, content, score
            FROM search_materials(:userId, :question, :subjectId, :limit, :topic)
            """, nativeQuery = true)
    List<SearchHit> search(@Param("userId") Long userId, @Param("question") String question,
                           @Param("subjectId") Long subjectId, @Param("limit") Integer limit,
                           @Param("topic") String topic);
}
