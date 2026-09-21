package com.projectguard.repository;

import com.projectguard.entity.DomainAssessmentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DomainAssessmentResultRepository extends JpaRepository<DomainAssessmentResult, Long> {
    List<DomainAssessmentResult> findByStudentProfileIdOrderByCreatedAtDesc(Long studentProfileId);
}
