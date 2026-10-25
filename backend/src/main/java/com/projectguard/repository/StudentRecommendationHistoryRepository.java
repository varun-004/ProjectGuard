package com.projectguard.repository;

import com.projectguard.entity.StudentRecommendationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface StudentRecommendationHistoryRepository extends JpaRepository<StudentRecommendationHistory, Long> {

    @Query("SELECT DISTINCT h.project.id FROM StudentRecommendationHistory h WHERE h.studentProfile.id = :studentProfileId")
    Set<Long> findRecommendedProjectIdsByStudentProfileId(@Param("studentProfileId") Long studentProfileId);

    List<StudentRecommendationHistory> findByStudentProfileIdOrderByRecommendedAtDesc(Long studentProfileId);

    long countByStudentProfileId(Long studentProfileId);

    void deleteByStudentProfileId(Long studentProfileId);
}
