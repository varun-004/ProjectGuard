package com.projectguard.repository;

import com.projectguard.entity.StudentSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentSkillRepository
        extends JpaRepository<StudentSkill, Long> {

    List<StudentSkill> findByStudentProfileId(Long studentProfileId);

    Optional<StudentSkill> findByStudentProfileIdAndSkillId(
            Long studentProfileId,
            Long skillId
    );

    boolean existsByStudentProfileIdAndSkillId(
            Long studentProfileId,
            Long skillId
    );
}