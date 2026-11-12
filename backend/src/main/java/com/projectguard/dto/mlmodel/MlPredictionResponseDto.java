package com.projectguard.dto.mlmodel;

import java.util.List;
import java.util.Map;

public class MlPredictionResponseDto {

    private String status;
    private String prediction;
    private Double probability;
    private String modelVersion;
    private String algorithm;
    private Map<String, Object> featureSummary;
    private List<String> riskFactors;
    private String disclaimer;
    private String message;

    public MlPredictionResponseDto() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPrediction() { return prediction; }
    public void setPrediction(String prediction) { this.prediction = prediction; }

    public Double getProbability() { return probability; }
    public void setProbability(Double probability) { this.probability = probability; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public Map<String, Object> getFeatureSummary() { return featureSummary; }
    public void setFeatureSummary(Map<String, Object> featureSummary) { this.featureSummary = featureSummary; }

    public List<String> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

