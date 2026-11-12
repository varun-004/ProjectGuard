package com.projectguard.service;

import com.projectguard.dto.mldataset.MlDatasetRecordDto;
import com.projectguard.dto.mlmodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MlModelService {

    private static final Logger log = LoggerFactory.getLogger(MlModelService.class);

    private final MlServiceClient mlServiceClient;
    private final MlDatasetService mlDatasetService;

    public MlModelService(MlServiceClient mlServiceClient, MlDatasetService mlDatasetService) {
        this.mlServiceClient = mlServiceClient;
        this.mlDatasetService = mlDatasetService;
    }

    public MlModelStatusDto getModelStatus() {
        Map<String, Object> raw = mlServiceClient.getModelStatus();
        MlModelStatusDto dto = new MlModelStatusDto();
        dto.setModelAvailable(Boolean.TRUE.equals(raw.get("model_available")));
        dto.setActiveVersion((String) raw.get("active_version"));
        dto.setAlgorithm((String) raw.get("algorithm"));
        if (raw.get("metrics") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metrics = (Map<String, Object>) raw.get("metrics");
            dto.setMetrics(metrics);
        }
        dto.setTrainedAt((String) raw.get("trained_at"));
        dto.setDatasetFingerprint((String) raw.get("dataset_fingerprint"));
        dto.setMessage((String) raw.get("message"));
        if (raw.get("all_versions") instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> all = (List<String>) raw.get("all_versions");
            dto.setAllVersions(all);
        }
        return dto;
    }

    public MlTrainingStatusDto triggerTraining() {
        // Step 1: Run preparation and fetch verified records from MySQL
        mlDatasetService.prepareAndValidateDataset();
        List<MlDatasetRecordDto> trainingRecords = mlDatasetService.getTrainingReadyRecords();

        // Step 2: Trigger training in Python service
        Map<String, Object> raw = mlServiceClient.triggerModelTraining(trainingRecords, 0.20, null, null);

        MlTrainingStatusDto dto = new MlTrainingStatusDto();
        dto.setStatus((String) raw.get("status"));
        dto.setMessage((String) raw.get("message"));
        if (raw.get("available_records") instanceof Number n) {
            dto.setAvailableRecords(n.intValue());
        } else {
            dto.setAvailableRecords(trainingRecords.size());
        }
        if (raw.get("required_records") instanceof Number n) {
            dto.setRequiredRecords(n.intValue());
        } else {
            dto.setRequiredRecords(50);
        }
        if (raw.get("records_needed") instanceof Number n) {
            dto.setRecordsNeeded(n.intValue());
        }
        if (raw.get("class_distribution") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dist = (Map<String, Object>) raw.get("class_distribution");
            dto.setClassDistribution(dist);
        }
        dto.setActiveVersion((String) raw.get("active_version"));
        dto.setBestModel((String) raw.get("best_model"));
        if (raw.get("metrics") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metrics = (Map<String, Object>) raw.get("metrics");
            dto.setMetrics(metrics);
        }
        if (raw.get("comparison") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> comp = (Map<String, Object>) raw.get("comparison");
            dto.setComparison(comp);
        }
        if (raw.get("train_size") instanceof Number n) {
            dto.setTrainSize(n.intValue());
        }
        if (raw.get("test_size") instanceof Number n) {
            dto.setTestSize(n.intValue());
        }
        return dto;
    }

    public Map<String, Object> getModelMetrics(String version) {
        return mlServiceClient.getModelMetrics(version);
    }

    public MlPredictionResponseDto predict(MlPredictionRequestDto requestDto) {
        Map<String, Object> raw = mlServiceClient.predict(requestDto);
        MlPredictionResponseDto dto = new MlPredictionResponseDto();
        dto.setStatus((String) raw.get("status"));
        dto.setPrediction((String) raw.get("prediction"));
        if (raw.get("probability") instanceof Number n) {
            dto.setProbability(n.doubleValue());
        }
        dto.setModelVersion((String) raw.get("model_version"));
        dto.setAlgorithm((String) raw.get("algorithm"));
        if (raw.get("feature_summary") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) raw.get("feature_summary");
            dto.setFeatureSummary(summary);
        }
        if (raw.get("risk_factors") instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> risks = (List<String>) raw.get("risk_factors");
            dto.setRiskFactors(risks);
        }
        dto.setDisclaimer((String) raw.get("disclaimer"));
        dto.setMessage((String) raw.get("message"));
        return dto;
    }

    public Map<String, Object> listVersions() {
        return mlServiceClient.listModelVersions();
    }

    public Map<String, Object> activateVersion(String version) {
        return mlServiceClient.activateModelVersion(version);
    }
}

