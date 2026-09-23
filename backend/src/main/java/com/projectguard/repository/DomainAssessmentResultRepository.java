package com.projectguard.repository;

import com.projectguard.entity.DomainAssessmentResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainAssessmentResultRepository extends JpaRepository<DomainAssessmentResult, Long> {

    Page<DomainAssessmentResult> findByStudentProfileIdOrderByCreatedAtDesc(
            Long studentProfileId,
            Pageable pageable
    );
}
