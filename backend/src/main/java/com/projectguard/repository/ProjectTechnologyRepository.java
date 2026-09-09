package com.projectguard.repository;

import com.projectguard.entity.ProjectTechnology;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectTechnologyRepository
        extends JpaRepository<ProjectTechnology, Long> {

    List<ProjectTechnology> findByProjectId(Long projectId);

    Optional<ProjectTechnology> findByProjectIdAndTechnologyId(
            Long projectId,
            Long technologyId
    );

    boolean existsByProjectIdAndTechnologyId(
            Long projectId,
            Long technologyId
    );
}