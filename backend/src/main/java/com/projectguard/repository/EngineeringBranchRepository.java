package com.projectguard.repository;

import com.projectguard.entity.EngineeringBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EngineeringBranchRepository
        extends JpaRepository<EngineeringBranch, Long> {

    Optional<EngineeringBranch> findByName(String name);

    boolean existsByName(String name);
}