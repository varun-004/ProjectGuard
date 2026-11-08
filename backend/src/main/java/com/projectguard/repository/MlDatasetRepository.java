package com.projectguard.repository;

import com.projectguard.entity.MlDataset;
import com.projectguard.entity.enums.MlOutcomeStatus;
import com.projectguard.entity.enums.MlValidationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MlDatasetRepository extends JpaRepository<MlDataset, Long> {

    Optional<MlDataset> findByStudentProfileIdAndProjectId(Long studentProfileId, Long projectId);

    boolean existsByStudentProfileIdAndProjectId(Long studentProfileId, Long projectId);

    Page<MlDataset> findByOutcomeStatus(MlOutcomeStatus outcomeStatus, Pageable pageable);

    Page<MlDataset> findByValidationStatus(MlValidationStatus validationStatus, Pageable pageable);

    Page<MlDataset> findByStudentProfileId(Long studentProfileId, Pageable pageable);

    long countByOutcomeStatus(MlOutcomeStatus outcomeStatus);

    long countByValidationStatus(MlValidationStatus validationStatus);

    @Query("SELECT d FROM MlDataset d WHERE d.outcomeStatus IN :statuses")
    List<MlDataset> findByOutcomeStatusIn(@Param("statuses") List<MlOutcomeStatus> statuses);

    @Query("SELECT d FROM MlDataset d LEFT JOIN FETCH d.studentProfile sp LEFT JOIN FETCH sp.user LEFT JOIN FETCH d.project p")
    List<MlDataset> findAllWithStudentAndProject();

    @Query("SELECT d FROM MlDataset d LEFT JOIN FETCH d.studentProfile sp LEFT JOIN FETCH sp.user LEFT JOIN FETCH d.project p WHERE d.outcomeStatus = :status")
    List<MlDataset> findAllByOutcomeStatusWithDetails(@Param("status") MlOutcomeStatus status);
}

