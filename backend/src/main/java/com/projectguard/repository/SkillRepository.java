package com.projectguard.repository;

import com.projectguard.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByName(String name);

    boolean existsByName(String name);

    /** Returns skills belonging to the given domain (via domain_skills join table). */
    List<Skill> findByDomains_Id(Long domainId);
}