package com.projectguard.repository;

import com.projectguard.entity.StudentProjectRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentProjectRecommendationRepository
        extends JpaRepository<StudentProjectRecommendation, Long> {

    @Query("SELECT r FROM StudentProjectRecommendation r " +
           "JOIN FETCH r.project p " +
           "LEFT JOIN FETCH p.domain " +
           "WHERE r.studentProfile.id = :studentProfileId " +
           "ORDER BY r.rankOrder ASC")
    List<StudentProjectRecommendation> findByStudentProfileIdOrderByRankOrderAsc(
            @Param("studentProfileId") Long studentProfileId);

    Optional<StudentProjectRecommendation> findByStudentProfileIdAndProjectId(
            Long studentProfileId, Long projectId);

    boolean existsByStudentProfileId(Long studentProfileId);

    @Modifying
    @Query("DELETE FROM StudentProjectRecommendation r WHERE r.studentProfile.id = :studentProfileId")
    void deleteByStudentProfileId(@Param("studentProfileId") Long studentProfileId);
}
