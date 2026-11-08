package com.projectguard.dto.mldataset;

import java.time.LocalDateTime;

public class MlDatasetRecordDto {
    private Long id;
    private Long studentProfileId;
    private String studentName;
    private String studentEmail;
    private Long projectId;
    private String projectTitle;

    // 10 ML input features
    private Double studentSkillLevel;
    private Double skillGap;
    private String projectComplexity;
    private Integer teamSize;
    private Double availableHoursPerWeek;
    private Double technologyFamiliarity;
    private Integer numberOfRequirements;
    private Integer numberOfFeatures;
    private Double previousExperience;
    private Double estimatedEffort;

    // Target outcome & validation
    private String actualOutcome;
    private String outcomeStatus;
    private String dataSource;
    private String validationStatus;
    private String labelEvidence;
    private String labelNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MlDatasetRecordDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentProfileId() { return studentProfileId; }
    public void setStudentProfileId(Long studentProfileId) { this.studentProfileId = studentProfileId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

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

    public String getActualOutcome() { return actualOutcome; }
    public void setActualOutcome(String actualOutcome) { this.actualOutcome = actualOutcome; }

    public String getOutcomeStatus() { return outcomeStatus; }
    public void setOutcomeStatus(String outcomeStatus) { this.outcomeStatus = outcomeStatus; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }

    public String getLabelEvidence() { return labelEvidence; }
    public void setLabelEvidence(String labelEvidence) { this.labelEvidence = labelEvidence; }

    public String getLabelNotes() { return labelNotes; }
    public void setLabelNotes(String labelNotes) { this.labelNotes = labelNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

