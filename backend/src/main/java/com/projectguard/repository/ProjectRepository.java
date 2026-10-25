package com.projectguard.repository;

import com.projectguard.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByStudentProfileId(Long studentProfileId);

    List<Project> findByIsCatalogTrue();

    List<Project> findByIsCatalogTrueAndQualityApprovedTrue();

    Optional<Project> findByIdAndIsCatalogTrue(Long id);

    Optional<Project> findByTitleAndIsCatalogTrue(String title);

    boolean existsByTitleAndIsCatalogTrue(String title);

    List<Project> findByDomainIdAndIsCatalogTrue(Long domainId);

    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN FETCH p.domain " +
           "WHERE p.isCatalog = true AND p.qualityApproved = true")
    List<Project> findAllApprovedCatalogProjectsWithDetails();

    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN FETCH p.domain " +
           "WHERE (p.isCatalog = true OR p.studentProfile.id = :profileId) AND p.qualityApproved = true")
    List<Project> findAllApprovedProjectsForStudentWithDetails(@Param("profileId") Long profileId);

    boolean existsByTitle(String title);

    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN FETCH p.domain " +
           "WHERE p.id = :id AND p.isCatalog = true")
    Optional<Project> findCatalogProjectWithAllDetailsById(@Param("id") Long id);
}