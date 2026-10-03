package com.projectguard.dto.student;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StudentProfileRequest {

    private String name;

    private String username;

    private String email;

    private String password;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private String academicLevel;

    @NotNull(message = "Domain ID is required")
    private Long domainId;

    private Long technologyId;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getAcademicLevel() {
        return academicLevel;
    }

    public void setAcademicLevel(String academicLevel) {
        this.academicLevel = academicLevel;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Long getTechnologyId() {
        return technologyId;
    }

    public void setTechnologyId(Long technologyId) {
        this.technologyId = technologyId;
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

