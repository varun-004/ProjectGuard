package com.projectguard.controller;

import com.projectguard.dto.mlmodel.MlModelStatusDto;
import com.projectguard.dto.mlmodel.MlPredictionRequestDto;
import com.projectguard.dto.mlmodel.MlPredictionResponseDto;
import com.projectguard.dto.mlmodel.MlTrainingStatusDto;
import com.projectguard.service.MlModelService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ml-model")
public class MlModelController {

    private final MlModelService mlModelService;

    public MlModelController(MlModelService mlModelService) {
        this.mlModelService = mlModelService;
    }

    /**
     * Read-only status of the active ML model.
     * Accessible by both STUDENT and ADMIN roles.
     */
    @GetMapping("/status")
    public ResponseEntity<MlModelStatusDto> getModelStatus() {
        return ResponseEntity.ok(mlModelService.getModelStatus());
    }

    /**
     * Executes real-time project feasibility prediction using the active ML model.
     * Accessible by both STUDENT and ADMIN roles.
     */
    @PostMapping("/predict")
    public ResponseEntity<MlPredictionResponseDto> predict(@Valid @RequestBody MlPredictionRequestDto requestDto) {
        MlPredictionResponseDto response = mlModelService.predict(requestDto);
        if ("NO_MODEL".equalsIgnoreCase(response.getStatus())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(response);
        }
        if ("UNREACHABLE".equalsIgnoreCase(response.getStatus())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        if ("VALIDATION_ERROR".equalsIgnoreCase(response.getStatus())) {
            return ResponseEntity.status(422).body(response);
        }
        if (!"SUCCESS".equalsIgnoreCase(response.getStatus())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_GATEWAY).body(response);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Triggers the authoritative Phase 10 training pipeline.
     * Evaluates dataset readiness (>= 50 authentic records, both classes).
     * ADMIN only.
     */
    @PostMapping("/train")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MlTrainingStatusDto> trainModel() {
        return ResponseEntity.ok(mlModelService.triggerTraining());
    }

    /**
     * Fetches detailed evaluation metrics and feature importance.
     * ADMIN only.
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getModelMetrics(@RequestParam(required = false) String version) {
        return ResponseEntity.ok(mlModelService.getModelMetrics(version));
    }

    /**
     * Lists all saved model versions and the active version.
     * ADMIN only.
     */
    @GetMapping("/versions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> listVersions() {
        return ResponseEntity.ok(mlModelService.listVersions());
    }

    /**
     * Activates a specific saved model version.
     * ADMIN only.
     */
    @PostMapping("/activate/{version}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> activateVersion(@PathVariable String version) {
        return ResponseEntity.ok(mlModelService.activateVersion(version));
    }
}

