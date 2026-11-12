package com.projectguard.dto.mlmodel;

public class MlModelVersionDto {

    private String version;
    private Boolean active;
    private String algorithm;
    private String savedAt;
    private Double f1Weighted;
    private Double accuracy;

    public MlModelVersionDto() {}

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getSavedAt() { return savedAt; }
    public void setSavedAt(String savedAt) { this.savedAt = savedAt; }

    public Double getF1Weighted() { return f1Weighted; }
    public void setF1Weighted(Double f1Weighted) { this.f1Weighted = f1Weighted; }

    public Double getAccuracy() { return accuracy; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
}

