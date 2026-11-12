package com.projectguard.dto.mlmodel;

import java.util.List;
import java.util.Map;

public class MlModelStatusDto {

    private boolean modelAvailable;
    private String activeVersion;
    private List<String> allVersions;
    private String algorithm;
    private Map<String, Object> metrics;
    private String trainedAt;
    private String datasetFingerprint;
    private String message;

    public MlModelStatusDto() {}

    public boolean isModelAvailable() { return modelAvailable; }
    public void setModelAvailable(boolean modelAvailable) { this.modelAvailable = modelAvailable; }

    public String getActiveVersion() { return activeVersion; }
    public void setActiveVersion(String activeVersion) { this.activeVersion = activeVersion; }

    public List<String> getAllVersions() { return allVersions; }
    public void setAllVersions(List<String> allVersions) { this.allVersions = allVersions; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }

    public String getTrainedAt() { return trainedAt; }
    public void setTrainedAt(String trainedAt) { this.trainedAt = trainedAt; }

    public String getDatasetFingerprint() { return datasetFingerprint; }
    public void setDatasetFingerprint(String datasetFingerprint) { this.datasetFingerprint = datasetFingerprint; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

