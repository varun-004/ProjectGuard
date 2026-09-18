package com.projectguard.dto.student;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StudentSkillRequest {

    /**
     * Either skillId (existing skill) or skillName (new skill) must be provided.
     * Priority: skillId > skillName.
     */
    private Long skillId;

    private String skillName;

    @NotBlank(message = "Proficiency level is required")
    private String proficiencyLevel;

    @NotNull(message = "Years of experience is required")
    @DecimalMin(value = "0.0", message = "Years of experience must be 0 or more")
    private Double yearsOfExperience;

    public StudentSkillRequest() {
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(String proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    public Double getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Double yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }
}
