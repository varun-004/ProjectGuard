package com.projectguard.dto.mlmodel;

import java.util.Map;

public class MlTrainingStatusDto {

    private String status;
    private String message;
    private Integer availableRecords;
    private Integer requiredRecords;
    private Integer recordsNeeded;
    private Map<String, Object> classDistribution;
    private String activeVersion;
    private String bestModel;
    private Map<String, Object> metrics;
    private Map<String, Object> comparison;
    private Integer trainSize;
    private Integer testSize;

    public MlTrainingStatusDto() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getAvailableRecords() { return availableRecords; }
    public void setAvailableRecords(Integer availableRecords) { this.availableRecords = availableRecords; }

    public Integer getRequiredRecords() { return requiredRecords; }
    public void setRequiredRecords(Integer requiredRecords) { this.requiredRecords = requiredRecords; }

    public Integer getRecordsNeeded() { return recordsNeeded; }
    public void setRecordsNeeded(Integer recordsNeeded) { this.recordsNeeded = recordsNeeded; }

    public Map<String, Object> getClassDistribution() { return classDistribution; }
    public void setClassDistribution(Map<String, Object> classDistribution) { this.classDistribution = classDistribution; }

    public String getActiveVersion() { return activeVersion; }
    public void setActiveVersion(String activeVersion) { this.activeVersion = activeVersion; }

    public String getBestModel() { return bestModel; }
    public void setBestModel(String bestModel) { this.bestModel = bestModel; }

    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }

    public Map<String, Object> getComparison() { return comparison; }
    public void setComparison(Map<String, Object> comparison) { this.comparison = comparison; }

    public Integer getTrainSize() { return trainSize; }
    public void setTrainSize(Integer trainSize) { this.trainSize = trainSize; }

    public Integer getTestSize() { return testSize; }
    public void setTestSize(Integer testSize) { this.testSize = testSize; }
}

