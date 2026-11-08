package com.projectguard.dto.mldataset;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class MlProjectRegistrationRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "Available hours per week is required")
    @DecimalMin(value = "1.0", message = "Available hours per week must be at least 1.0")
    @DecimalMax(value = "80.0", message = "Available hours per week must not exceed 80.0")
    private Double availableHoursPerWeek;

    private Integer teamSize;

    private Double previousExperience;

    private Double estimatedEffort;

    private Integer numberOfFeatures;

    public MlProjectRegistrationRequest() {}

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Double getAvailableHoursPerWeek() { return availableHoursPerWeek; }
    public void setAvailableHoursPerWeek(Double availableHoursPerWeek) { this.availableHoursPerWeek = availableHoursPerWeek; }

    public Integer getTeamSize() { return teamSize; }
    public void setTeamSize(Integer teamSize) { this.teamSize = teamSize; }

    public Double getPreviousExperience() { return previousExperience; }
    public void setPreviousExperience(Double previousExperience) { this.previousExperience = previousExperience; }

    public Double getEstimatedEffort() { return estimatedEffort; }
    public void setEstimatedEffort(Double estimatedEffort) { this.estimatedEffort = estimatedEffort; }

    public Integer getNumberOfFeatures() { return numberOfFeatures; }
    public void setNumberOfFeatures(Integer numberOfFeatures) { this.numberOfFeatures = numberOfFeatures; }
}

