package com.projectguard.dto.student;

import java.time.LocalDateTime;
import java.util.List;

public class StudentProfileResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long branchId;
    private String branchName;
    private Long domainId;
    private String domainName;
    private Integer teamSize;
    private Integer availableTimeWeeks;
    private String previousExperience;
    private String interests;
    private String photoUrl;
    private List<StudentSkillResponse> skills;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StudentProfileResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public List<StudentSkillResponse> getSkills() {
        return skills;
    }

    public void setSkills(List<StudentSkillResponse> skills) {
        this.skills = skills;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
