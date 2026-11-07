package com.lekseradar.repository;

import com.lekseradar.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findAllByOrderByName();

    /** Поиск по названию без учёта регистра и лишних пробелов (как norm_key в базе). */
    @Query(value = "SELECT * FROM subjects WHERE key = norm_key(:name)", nativeQuery = true)
    Optional<Subject> findByName(@Param("name") String name);
}
