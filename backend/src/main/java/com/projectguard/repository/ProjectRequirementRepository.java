package com.projectguard.repository;

import com.projectguard.entity.ProjectRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRequirementRepository
        extends JpaRepository<ProjectRequirement, Long> {

    List<ProjectRequirement> findByProjectId(Long projectId);
}