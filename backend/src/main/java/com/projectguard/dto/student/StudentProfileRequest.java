package com.projectguard.dto.student;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StudentProfileRequest {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Domain ID is required")
    private Long domainId;

    @NotNull(message = "Team size is required")
    @Min(value = 1, message = "Team size must be at least 1")
    @Max(value = 20, message = "Team size must be at most 20")
    private Integer teamSize;

    @NotNull(message = "Available time in weeks is required")
    @Min(value = 1, message = "Available time must be at least 1 week")
    @Max(value = 52, message = "Available time must be at most 52 weeks")
    private Integer availableTimeWeeks;

    private String previousExperience;

    private String interests;

    public StudentProfileRequest() {
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Integer getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(Integer teamSize) {
        this.teamSize = teamSize;
    }

    public Integer getAvailableTimeWeeks() {
        return availableTimeWeeks;
    }

    public void setAvailableTimeWeeks(Integer availableTimeWeeks) {
        this.availableTimeWeeks = availableTimeWeeks;
    }

    public String getPreviousExperience() {
        return previousExperience;
    }

    public void setPreviousExperience(String previousExperience) {
        this.previousExperience = previousExperience;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }
}
