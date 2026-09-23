package com.projectguard.repository;

import com.projectguard.entity.AssessmentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, Long> {
    Optional<AssessmentAttempt> findByAttemptId(String attemptId);
    List<AssessmentAttempt> findByStudentProfileIdOrderByCreatedAtDesc(Long studentProfileId);
}
