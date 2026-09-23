package com.projectguard.dto.assessment;

import java.time.LocalDateTime;
import java.util.List;

public class AssessmentResultDto {
    private Long id;
    private Long skillId;
    private String skillName;
    private Integer score;
    private Integer maxWeightedScore;
    private Double percentage;
    private String level;
    private LocalDateTime createdAt;
    
    // No detailed reviews here, moved to DomainAssessmentResultDto

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getMaxWeightedScore() { return maxWeightedScore; }
    public void setMaxWeightedScore(Integer maxWeightedScore) { this.maxWeightedScore = maxWeightedScore; }
    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    // Reviews moved
}
