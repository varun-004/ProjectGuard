package com.projectguard.dto.mlmodel;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class MlPredictionRequestDto {

    @JsonProperty("student_skill_level")
    @JsonAlias({"studentSkillLevel", "student_skill_level"})
    @NotNull(message = "studentSkillLevel is required")
    @DecimalMin(value = "1.0", message = "studentSkillLevel must be at least 1.0")
    @DecimalMax(value = "4.0", message = "studentSkillLevel must be at most 4.0")
    private Double studentSkillLevel;

    @JsonProperty("skill_gap")
    @JsonAlias({"skillGap", "skill_gap"})
    @NotNull(message = "skillGap is required")
    @DecimalMin(value = "0.0", message = "skillGap must be at least 0.0")
    @DecimalMax(value = "25.0", message = "skillGap must be at most 25.0")
    private Double skillGap;

    @JsonProperty("project_complexity")
    @JsonAlias({"projectComplexity", "project_complexity"})
    @NotBlank(message = "projectComplexity is required")
    @Pattern(regexp = "^(?i)(EASY|MEDIUM|HARD)$", message = "projectComplexity must be EASY, MEDIUM, or HARD")
    private String projectComplexity;

    @JsonProperty("team_size")
    @JsonAlias({"teamSize", "team_size"})
    @NotNull(message = "teamSize is required")
    @Min(value = 1, message = "teamSize must be at least 1")
    private Integer teamSize;

    @JsonProperty("available_hours_per_week")
    @JsonAlias({"availableHoursPerWeek", "available_hours_per_week"})
    @NotNull(message = "availableHoursPerWeek is required")
    @DecimalMin(value = "1.0", message = "availableHoursPerWeek must be at least 1.0")
    @DecimalMax(value = "80.0", message = "availableHoursPerWeek must be at most 80.0")
    private Double availableHoursPerWeek;

    @JsonProperty("technology_familiarity")
    @JsonAlias({"technologyFamiliarity", "technology_familiarity"})
    @NotNull(message = "technologyFamiliarity is required")
    @DecimalMin(value = "1.0", message = "technologyFamiliarity must be at least 1.0")
    @DecimalMax(value = "4.0", message = "technologyFamiliarity must be at most 4.0")
    private Double technologyFamiliarity;

    @JsonProperty("number_of_requirements")
    @JsonAlias({"numberOfRequirements", "number_of_requirements"})
    @NotNull(message = "numberOfRequirements is required")
    @Min(value = 0, message = "numberOfRequirements must be at least 0")
    private Integer numberOfRequirements;

    @JsonProperty("number_of_features")
    @JsonAlias({"numberOfFeatures", "number_of_features"})
    @NotNull(message = "numberOfFeatures is required")
    @Min(value = 0, message = "numberOfFeatures must be at least 0")
    private Integer numberOfFeatures;

    @JsonProperty("previous_experience")
    @JsonAlias({"previousExperience", "previous_experience"})
    @NotNull(message = "previousExperience is required")
    @DecimalMin(value = "0.0", message = "previousExperience must be at least 0.0")
    private Double previousExperience;

    @JsonProperty("estimated_effort")
    @JsonAlias({"estimatedEffort", "estimated_effort"})
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

