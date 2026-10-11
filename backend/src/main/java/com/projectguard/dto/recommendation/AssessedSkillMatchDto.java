package com.projectguard.dto.recommendation;

public class AssessedSkillMatchDto {
    private String skillName;
    private String assessedLevel; // BEGINNER, INTERMEDIATE, ADVANCED, NOT_ASSESSED
    private Double assessedPercentage;
    private boolean isAssessed;
    private boolean isRequirementMet;
    private String note;

    public AssessedSkillMatchDto() {}

    public AssessedSkillMatchDto(String skillName, String assessedLevel, Double assessedPercentage,
                                 boolean isAssessed, boolean isRequirementMet, String note) {
        this.skillName = skillName;
        this.assessedLevel = assessedLevel;
        this.assessedPercentage = assessedPercentage;
        this.isAssessed = isAssessed;
        this.isRequirementMet = isRequirementMet;
        this.note = note;
    }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getAssessedLevel() { return assessedLevel; }
    public void setAssessedLevel(String assessedLevel) { this.assessedLevel = assessedLevel; }

    public Double getAssessedPercentage() { return assessedPercentage; }
    public void setAssessedPercentage(Double assessedPercentage) { this.assessedPercentage = assessedPercentage; }

    public boolean isAssessed() { return isAssessed; }
    public void setAssessed(boolean assessed) { isAssessed = assessed; }

    public boolean isRequirementMet() { return isRequirementMet; }
    public void setRequirementMet(boolean requirementMet) { isRequirementMet = requirementMet; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
