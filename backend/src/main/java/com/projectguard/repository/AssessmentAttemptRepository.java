package com.projectguard.repository;

import com.projectguard.entity.AssessmentAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import java.util.List;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AssessmentAttempt> findByAttemptId(String attemptId);

    List<AssessmentAttempt> findByStudentProfileIdOrderByCreatedAtDesc(Long studentProfileId);
}
