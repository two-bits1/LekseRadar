package com.lekseradar.repository;

import com.lekseradar.model.SubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<SubjectEntity, Long> {
    List<SubjectEntity> findAllByOrderByName();

    /** Поиск по названию без учёта регистра и лишних пробелов (как norm_key в базе). */
    @Query(value = "SELECT * FROM subjects WHERE key = norm_key(:name)", nativeQuery = true)
    Optional<SubjectEntity> findByName(@Param("name") String name);
}
