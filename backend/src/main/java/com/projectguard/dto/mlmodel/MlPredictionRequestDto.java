package com.projectguard.dto.mlmodel;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class MlPredictionRequestDto {

    @NotNull(message = "studentSkillLevel is required")
    @DecimalMin(value = "1.0", message = "studentSkillLevel must be at least 1.0")
    @DecimalMax(value = "4.0", message = "studentSkillLevel must be at most 4.0")
    private Double studentSkillLevel;

    @NotNull(message = "skillGap is required")
    @DecimalMin(value = "0.0", message = "skillGap must be at least 0.0")
    @DecimalMax(value = "25.0", message = "skillGap must be at most 25.0")
    private Double skillGap;

    @NotBlank(message = "projectComplexity is required")
    @Pattern(regexp = "^(?i)(EASY|MEDIUM|HARD)$", message = "projectComplexity must be EASY, MEDIUM, or HARD")
    private String projectComplexity;

    @NotNull(message = "teamSize is required")
    @Min(value = 1, message = "teamSize must be at least 1")
    private Integer teamSize;

    @NotNull(message = "availableHoursPerWeek is required")
    @DecimalMin(value = "1.0", message = "availableHoursPerWeek must be at least 1.0")
    @DecimalMax(value = "80.0", message = "availableHoursPerWeek must be at most 80.0")
    private Double availableHoursPerWeek;

    @NotNull(message = "technologyFamiliarity is required")
    @DecimalMin(value = "1.0", message = "technologyFamiliarity must be at least 1.0")
    @DecimalMax(value = "4.0", message = "technologyFamiliarity must be at most 4.0")
    private Double technologyFamiliarity;

    @NotNull(message = "numberOfRequirements is required")
    @Min(value = 0, message = "numberOfRequirements must be at least 0")
    private Integer numberOfRequirements;

    @NotNull(message = "numberOfFeatures is required")
    @Min(value = 0, message = "numberOfFeatures must be at least 0")
    private Integer numberOfFeatures;

    @NotNull(message = "previousExperience is required")
    @DecimalMin(value = "0.0", message = "previousExperience must be at least 0.0")
    private Double previousExperience;

    @NotNull(message = "estimatedEffort is required")
    @DecimalMin(value = "1.0", message = "estimatedEffort must be at least 1.0")
    private Double estimatedEffort;

    public MlPredictionRequestDto() {}

    public Double getStudentSkillLevel() { return studentSkillLevel; }
    public void setStudentSkillLevel(Double studentSkillLevel) { this.studentSkillLevel = studentSkillLevel; }

    public Double getSkillGap() { return skillGap; }
    public void setSkillGap(Double skillGap) { this.skillGap = skillGap; }

    public String getProjectComplexity() { return projectComplexity; }
    public void setProjectComplexity(String projectComplexity) { this.projectComplexity = projectComplexity; }

    public Integer getTeamSize() { return teamSize; }
    public void setTeamSize(Integer teamSize) { this.teamSize = teamSize; }

    public Double getAvailableHoursPerWeek() { return availableHoursPerWeek; }
    public void setAvailableHoursPerWeek(Double availableHoursPerWeek) { this.availableHoursPerWeek = availableHoursPerWeek; }

    public Double getTechnologyFamiliarity() { return technologyFamiliarity; }
    public void setTechnologyFamiliarity(Double technologyFamiliarity) { this.technologyFamiliarity = technologyFamiliarity; }

    public Integer getNumberOfRequirements() { return numberOfRequirements; }
    public void setNumberOfRequirements(Integer numberOfRequirements) { this.numberOfRequirements = numberOfRequirements; }

    public Integer getNumberOfFeatures() { return numberOfFeatures; }
    public void setNumberOfFeatures(Integer numberOfFeatures) { this.numberOfFeatures = numberOfFeatures; }

    public Double getPreviousExperience() { return previousExperience; }
    public void setPreviousExperience(Double previousExperience) { this.previousExperience = previousExperience; }

    public Double getEstimatedEffort() { return estimatedEffort; }
    public void setEstimatedEffort(Double estimatedEffort) { this.estimatedEffort = estimatedEffort; }
}

