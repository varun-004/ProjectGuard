package com.projectguard.dto.recommendation;

import java.util.ArrayList;
import java.util.List;

public class RecommendationListResponse {
    private Long studentProfileId;
    private String studentName;
    private String branchName;
    private String domainName;
    private String technologyName;
    private String academicLevel;
    private Integer teamSize;
    private Integer availableTimeWeeks;
    private Integer totalRecommendations;
    private String message;
    private String disclaimer;
    private List<ProjectRecommendationResponse> recommendations = new ArrayList<>();
    private String evaluatedAt;
    private String aiConfigurationStatus;

    public RecommendationListResponse() {}

    public Long getStudentProfileId() { return studentProfileId; }
    public void setStudentProfileId(Long studentProfileId) { this.studentProfileId = studentProfileId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }

    public String getTechnologyName() { return technologyName; }
    public void setTechnologyName(String technologyName) { this.technologyName = technologyName; }

    public String getAcademicLevel() { return academicLevel; }
    public void setAcademicLevel(String academicLevel) { this.academicLevel = academicLevel; }

    public Integer getTeamSize() { return teamSize; }
    public void setTeamSize(Integer teamSize) { this.teamSize = teamSize; }

    public Integer getAvailableTimeWeeks() { return availableTimeWeeks; }
    public void setAvailableTimeWeeks(Integer availableTimeWeeks) { this.availableTimeWeeks = availableTimeWeeks; }

    public Integer getTotalRecommendations() { return totalRecommendations; }
    public void setTotalRecommendations(Integer totalRecommendations) { this.totalRecommendations = totalRecommendations; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }

    public List<ProjectRecommendationResponse> getRecommendations() { return recommendations; }
    public void setRecommendations(List<ProjectRecommendationResponse> recommendations) { this.recommendations = recommendations; }

    public String getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(String evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public String getAiConfigurationStatus() { return aiConfigurationStatus; }
    public void setAiConfigurationStatus(String aiConfigurationStatus) { this.aiConfigurationStatus = aiConfigurationStatus; }
}
