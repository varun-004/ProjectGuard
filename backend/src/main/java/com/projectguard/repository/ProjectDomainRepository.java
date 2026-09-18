package com.projectguard.repository;

import com.projectguard.entity.ProjectDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectDomainRepository
        extends JpaRepository<ProjectDomain, Long> {

    Optional<ProjectDomain> findByName(String name);

    boolean existsByName(String name);

    /** Returns domains belonging to the given branch (via branch_domains join table). */
    List<ProjectDomain> findByBranches_Id(Long branchId);
}