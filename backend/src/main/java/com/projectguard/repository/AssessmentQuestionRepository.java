package com.projectguard.repository;

import com.projectguard.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Long> {
    List<AssessmentQuestion> findBySkillId(Long skillId);
    List<AssessmentQuestion> findBySkillIdIn(List<Long> skillIds);
}
