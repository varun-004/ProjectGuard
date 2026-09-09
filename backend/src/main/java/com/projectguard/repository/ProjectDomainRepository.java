package com.projectguard.repository;

import com.projectguard.entity.ProjectDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectDomainRepository
        extends JpaRepository<ProjectDomain, Long> {

    Optional<ProjectDomain> findByName(String name);

    boolean existsByName(String name);
}