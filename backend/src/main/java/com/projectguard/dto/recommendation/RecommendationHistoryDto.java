package com.projectguard.dto.recommendation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO for displaying a student's saved recommendation and generation history.
 */
public class RecommendationHistoryDto {

    private Long id;
    private Long projectId;
    private String projectTitle;
    private Double matchScore;
    private Integer rankOrder;
    private String recommendedAt;
    private String complexityLevel;
    private String domainName;
    private String problemStatement;
    private String industryValue;
    private boolean isGeneratedIdea;

    public RecommendationHistoryDto() {
    }

    public RecommendationHistoryDto(Long id, Long projectId, String projectTitle, Double matchScore,
                                   Integer rankOrder, LocalDateTime recommendedAt, String complexityLevel,
                                   String domainName, String problemStatement, String industryValue,
                                   boolean isGeneratedIdea) {
        this.id = id;
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.matchScore = matchScore;
        this.rankOrder = rankOrder;
        if (recommendedAt != null) {
            this.recommendedAt = recommendedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        this.complexityLevel = complexityLevel;
        this.domainName = domainName;
        this.problemStatement = problemStatement;
        this.industryValue = industryValue;
        this.isGeneratedIdea = isGeneratedIdea;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectTitle() { return projectTitle; }
    public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }

    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }

    public Integer getRankOrder() { return rankOrder; }
    public void setRankOrder(Integer rankOrder) { this.rankOrder = rankOrder; }

    public String getRecommendedAt() { return recommendedAt; }
    public void setRecommendedAt(String recommendedAt) { this.recommendedAt = recommendedAt; }

    public String getComplexityLevel() { return complexityLevel; }
    public void setComplexityLevel(String complexityLevel) { this.complexityLevel = complexityLevel; }

    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }

    public String getProblemStatement() { return problemStatement; }
    public void setProblemStatement(String problemStatement) { this.problemStatement = problemStatement; }

    public String getIndustryValue() { return industryValue; }
    public void setIndustryValue(String industryValue) { this.industryValue = industryValue; }

    public boolean isGeneratedIdea() { return isGeneratedIdea; }
    public void setGeneratedIdea(boolean generatedIdea) { isGeneratedIdea = generatedIdea; }
}
