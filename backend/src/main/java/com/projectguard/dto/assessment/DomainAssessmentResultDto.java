package com.projectguard.dto.assessment;

import java.time.LocalDateTime;
import java.util.List;

public class DomainAssessmentResultDto {
    private Long id;
    private Integer score;
    private Double percentage;
    private String level;
    private LocalDateTime createdAt;
    
    private List<AssessmentResultDto> skillResults;
    private List<AssessmentReviewDto> reviews;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<AssessmentResultDto> getSkillResults() { return skillResults; }
    public void setSkillResults(List<AssessmentResultDto> skillResults) { this.skillResults = skillResults; }
    public List<AssessmentReviewDto> getReviews() { return reviews; }
    public void setReviews(List<AssessmentReviewDto> reviews) { this.reviews = reviews; }
}
