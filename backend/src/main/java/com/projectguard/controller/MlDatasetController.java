package com.projectguard.controller;

import com.projectguard.dto.mldataset.*;
import com.projectguard.entity.User;
import com.projectguard.entity.enums.MlOutcomeStatus;
import com.projectguard.entity.enums.Role;
import com.projectguard.exception.BadRequestException;
import com.projectguard.exception.ResourceNotFoundException;
import com.projectguard.repository.StudentProfileRepository;
import com.projectguard.repository.UserRepository;
import com.projectguard.service.MlDatasetService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/ml-dataset")
public class MlDatasetController {

    private final MlDatasetService mlDatasetService;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;

    public MlDatasetController(MlDatasetService mlDatasetService,
                               UserRepository userRepository,
                               StudentProfileRepository studentProfileRepository) {
        this.mlDatasetService = mlDatasetService;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    /**
     * GET /api/ml-dataset
     * Retrieve authorized dataset records with pagination.
     * Admin users see all records; students only see their own records.
     */
    @GetMapping
    public ResponseEntity<Page<MlDatasetRecordDto>> getRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String outcomeStatus,
            Authentication authentication) {

        User user = getUser(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        MlOutcomeStatus status = null;
        if (outcomeStatus != null && !outcomeStatus.isBlank() && !"ALL".equalsIgnoreCase(outcomeStatus)) {
            try {
                status = MlOutcomeStatus.valueOf(outcomeStatus.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid outcome status filter: " + outcomeStatus);
            }
        }

        Long filterStudentProfileId = null;
        if (user.getRole() == Role.STUDENT) {
            filterStudentProfileId = studentProfileRepository.findByUserId(user.getId())
                    .map(com.projectguard.entity.StudentProfile::getId)
                    .orElse(null);
            if (filterStudentProfileId == null) {
                return ResponseEntity.ok(Page.empty(pageable));
            }
        }

        return ResponseEntity.ok(mlDatasetService.getRecords(pageable, status, filterStudentProfileId));
    }

    /**
     * GET /api/ml-dataset/statistics
     * Return dataset statistics.
     */
    @GetMapping("/statistics")
    public ResponseEntity<MlDatasetStatisticsDto> getStatistics() {
        return ResponseEntity.ok(mlDatasetService.getStatistics());
    }

    /**
     * GET /api/ml-dataset/pending-outcomes
     * Retrieve projects awaiting outcome evaluation (Admin / Evaluator).
     */
    @GetMapping("/pending-outcomes")
    public ResponseEntity<Page<MlDatasetRecordDto>> getPendingOutcomes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(mlDatasetService.getPendingOutcomes(pageable));
    }

    /**
     * PUT /api/ml-dataset/{id}/outcome
     * Record a verified actual project outcome (FEASIBLE or NOT_FEASIBLE).
     */
    @PutMapping("/{id}/outcome")
    public ResponseEntity<MlDatasetRecordDto> recordActualOutcome(
            @PathVariable Long id,
            @Valid @RequestBody MlOutcomeUpdateRequest request,
            Authentication authentication) {
        User user = getUser(authentication);
        if (user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only authorized administrators or evaluators can record actual project outcomes.");
        }
        return ResponseEntity.ok(mlDatasetService.recordActualOutcome(id, request));
    }

    /**
     * POST /api/ml-dataset/prepare
     * Trigger dataset validation and preparation.
     */
    @PostMapping("/prepare")
    public ResponseEntity<MlDatasetPrepareSummaryDto> prepareDataset(Authentication authentication) {
        User user = getUser(authentication);
        if (user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only authorized administrators can run dataset validation and preparation.");
        }
        return ResponseEntity.ok(mlDatasetService.prepareAndValidateDataset());
    }

    /**
     * GET /api/ml-dataset/export
     * Export the cleaned training-ready CSV (UTF-8, exact 10 features + 1 target).
     */
    @GetMapping("/export")
    public void exportCleanedCsv(HttpServletResponse response, Authentication authentication) throws IOException {
        User user = getUser(authentication);
        if (user.getRole() != Role.ADMIN) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Access denied: Admin authorization required");
            return;
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"projectguard_ml_dataset.csv\"");

        try (OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            mlDatasetService.exportCleanedTrainingCsv(writer);
        }
    }

    /**
     * GET /api/ml-dataset/split
     * Retrieve 80/20 train/test split statistics and generated dataset file information.
     */
    @GetMapping("/split")
    public ResponseEntity<Map<String, Object>> getSplitStatistics(Authentication authentication) {
        User user = getUser(authentication);
        if (user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only authorized administrators can view training/testing split pipeline details.");
        }
        return ResponseEntity.ok(mlDatasetService.getSplitReport());
    }

    /**
     * POST /api/ml-dataset/register
     * Student registers or selects a project, capturing available hours and snapshot into ml_dataset.
     */
    @PostMapping("/register")
    public ResponseEntity<MlDatasetRecordDto> registerProject(
            @Valid @RequestBody MlProjectRegistrationRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mlDatasetService.registerStudentProject(authentication.getName(), request));
    }

    private User getUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BadRequestException("Unauthenticated request");
        }
        return userRepository.findByEmail(authentication.getName())
                .or(() -> userRepository.findByUsername(authentication.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }
}

