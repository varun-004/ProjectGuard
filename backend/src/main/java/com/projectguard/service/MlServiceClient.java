package com.projectguard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectguard.dto.mldataset.MlDatasetRecordDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * MlServiceClient — Authoritative client connecting Spring Boot to the Python FastAPI ML Service.
 *
 * <p>Key requirements:
 * <ul>
 *   <li>Authoritative dataset flow: MySQL -> Spring Boot export -> Python ML pipeline -> Train/test CSVs.</li>
 *   <li>External configuration with environment-variable override support.</li>
 *   <li>Connection and request timeouts.</li>
 *   <li>Graceful degradation if the Python service is offline or unreachable.</li>
 *   <li>Zero exposure of secrets or credentials.</li>
 * </ul>
 */
@Service
public class MlServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MlServiceClient.class);

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    @Value("${ml.service.timeout-seconds:10}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public MlServiceClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Checks whether the Python ML Service is healthy and online.
     */
    public Map<String, Object> checkHealth() {
        Map<String, Object> result = new LinkedHashMap<>();
        String url = getCleanBaseUrl() + "/health";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {});
                result.put("status", "UP");
                result.put("serviceUrl", getCleanBaseUrl());
                result.put("details", body);
                return result;
            } else {
                result.put("status", "DOWN");
                result.put("serviceUrl", getCleanBaseUrl());
                result.put("httpStatus", response.statusCode());
                result.put("message", "ML service responded with non-200 code");
                return result;
            }
        } catch (Exception e) {
            log.warn("ML Service health check failed at {}: {}", url, e.getMessage());
            result.put("status", "UNREACHABLE");
            result.put("serviceUrl", getCleanBaseUrl());
            result.put("message", "Python ML service is offline or unreachable: " + e.getMessage());
            return result;
        }
    }

    /**
     * Sends validated records to the Python ML Service to execute validation, encoding,
     * stratified 80/20 train/test splitting, and CSV generation.
     */
    public Map<String, Object> triggerPipeline(List<MlDatasetRecordDto> records, double testSize, int randomState, boolean groupByStudent) {
        Map<String, Object> result = new LinkedHashMap<>();
        String url = getCleanBaseUrl() + "/dataset/pipeline/run";

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("records", records);
            payload.put("test_size", testSize);
            payload.put("random_state", randomState);
            payload.put("group_by_student", groupByStudent);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {});
                result.put("status", "SUCCESS");
                result.put("serviceUrl", getCleanBaseUrl());
                result.put("pipelineResult", body);
                return result;
            } else {
                log.warn("Python ML Service pipeline returned HTTP {}: {}", response.statusCode(), response.body());
                result.put("status", "ERROR");
                result.put("httpStatus", response.statusCode());
                result.put("message", "Python ML Service returned HTTP " + response.statusCode());
                result.put("responseBody", response.body());
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to communicate with ML Service pipeline at {}: {}", url, e.getMessage());
            result.put("status", "UNREACHABLE");
            result.put("serviceUrl", getCleanBaseUrl());
            result.put("message", "Python ML Service is unavailable: " + e.getMessage());
            return result;
        }
    }

    /**
     * Retrieves the status of the trained ML model from Python service.
     */
    public Map<String, Object> getModelStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        String url = getCleanBaseUrl() + "/model/status";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            } else {
                result.put("model_available", false);
                result.put("message", "ML service returned HTTP " + response.statusCode());
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to get model status from {}: {}", url, e.getMessage());
            result.put("model_available", false);
            result.put("message", "Python ML service unreachable: " + e.getMessage());
            return result;
        }
    }

    /**
     * Triggers model training in the Python ML service.
     */
    public Map<String, Object> triggerModelTraining(List<MlDatasetRecordDto> records, Double testSize, Map<String, Object> rfParams, Map<String, Object> gbParams) {
        Map<String, Object> result = new LinkedHashMap<>();
        String url = getCleanBaseUrl() + "/model/train";
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (records != null) {
                payload.put("records", records);
            }
            if (testSize != null) {
                payload.put("test_size", testSize);
            }
            if (rfParams != null) {
                payload.put("rf_params", rfParams);
            }
            if (gbParams != null) {
                payload.put("gb_params", gbParams);
            }

            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(Math.max(timeoutSeconds, 60))) // training can take longer
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            } else {
                result.put("status", "ERROR");
                result.put("message", "ML training failed with HTTP " + response.statusCode() + ": " + response.body());
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to trigger model training at {}: {}", url, e.getMessage());
            result.put("status", "UNREACHABLE");
            result.put("message", "Python ML service unreachable: " + e.getMessage());
            return result;
        }
    }

    /**
     * Fetches detailed model metrics from Python service.
     */
    public Map<String, Object> getModelMetrics(String version) {
        Map<String, Object> result = new LinkedHashMap<>();
        String url = getCleanBaseUrl() + "/model/metrics" + (version != null && !version.isBlank() ? "?version=" + version : "");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            } else {
                result.put("status", "NOT_FOUND");
                result.put("message", "No metrics found or ML service error (HTTP " + response.statusCode() + ")");
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to get model metrics from {}: {}", url, e.getMessage());
            result.put("status", "UNREACHABLE");
            result.put("message", "Python ML service unreachable: " + e.getMessage());
            return result;
        }
    }

    /**
     * Executes prediction using active model in Python service.
     */
    public Map<String, Object> predict(com.projectguard.dto.mlmodel.MlPredictionRequestDto requestDto) {
        Map<String, Object> result = new LinkedHashMap<>();
        String url = getCleanBaseUrl() + "/model/predict";
        try {
            String requestBody = objectMapper.writeValueAsString(requestDto);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            } else if (response.statusCode() == 404) {
                result.put("status", "NO_MODEL");
                result.put("message", "No trained model is currently active. Model training is required before predictions can be served.");
                return result;
            } else {
                result.put("status", "ERROR");
                result.put("message", "Prediction failed with HTTP " + response.statusCode() + ": " + response.body());
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to execute prediction at {}: {}", url, e.getMessage());
            result.put("status", "UNREACHABLE");
            result.put("message", "Python ML service unreachable: " + e.getMessage());
            return result;
        }
    }

    /**
     * Lists all saved model versions in Python registry.
     */
    public Map<String, Object> listModelVersions() {
        Map<String, Object> result = new LinkedHashMap<>();
        String url = getCleanBaseUrl() + "/model/versions";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            } else {
                result.put("total_versions", 0);
                result.put("message", "Failed to list versions (HTTP " + response.statusCode() + ")");
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to list model versions from {}: {}", url, e.getMessage());
            result.put("total_versions", 0);
            result.put("message", "Python ML service unreachable: " + e.getMessage());
            return result;
        }
    }

    /**
     * Activates a model version in Python registry.
     */
    public Map<String, Object> activateModelVersion(String version) {
        Map<String, Object> result = new LinkedHashMap<>();
        String url = getCleanBaseUrl() + "/model/activate/" + version;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<>() {});
            } else {
                result.put("status", "ERROR");
                result.put("message", "Activation failed: " + response.body());
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to activate model version at {}: {}", url, e.getMessage());
            result.put("status", "UNREACHABLE");
            result.put("message", "Python ML service unreachable: " + e.getMessage());
            return result;
        }
    }

    public String getCleanBaseUrl() {
        if (mlServiceUrl == null || mlServiceUrl.isBlank()) {
            return "http://localhost:8000";
        }
        return mlServiceUrl.trim().replaceAll("/+$", "");
    }

    public void setMlServiceUrl(String mlServiceUrl) {
        this.mlServiceUrl = mlServiceUrl;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}

