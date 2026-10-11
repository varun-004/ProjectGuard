package com.projectguard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_project_recommendations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_student_recommendations_profile_project",
                columnNames = {"student_profile_id", "project_id"}
        )
)
public class StudentProjectRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "match_score", nullable = false)
    private Double matchScore; // 0 - 100

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Column(name = "domain_fit_score")
    private Double domainFitScore;

    @Column(name = "tech_fit_score")
    private Double techFitScore;

    @Column(name = "skill_fit_score")
    private Double skillFitScore;

    @Column(name = "proficiency_score")
    private Double proficiencyScore;

    @Column(name = "experience_fit_score")
    private Double experienceFitScore;

    @Column(name = "team_fit_score")
    private Double teamFitScore;

    @Column(name = "time_fit_score")
    private Double timeFitScore;

    @Column(name = "penalty_score")
    private Double penaltyScore;

    @Column(name = "recommendation_reasons", columnDefinition = "TEXT")
    private String recommendationReasons;

    @Column(name = "identified_gaps", columnDefinition = "TEXT")
    private String identifiedGaps;

    @Column(name = "score_explanation", columnDefinition = "TEXT")
    private String scoreExplanation;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StudentProjectRecommendation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StudentProfile getStudentProfile() {
        return studentProfile;
    }

    public void setStudentProfile(StudentProfile studentProfile) {
        this.studentProfile = studentProfile;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }

    public Integer getRankOrder() {
        return rankOrder;
    }

    public void setRankOrder(Integer rankOrder) {
        this.rankOrder = rankOrder;
    }

    public Double getDomainFitScore() {
        return domainFitScore;
    }

    public void setDomainFitScore(Double domainFitScore) {
        this.domainFitScore = domainFitScore;
    }

    public Double getTechFitScore() {
        return techFitScore;
    }

    public void setTechFitScore(Double techFitScore) {
        this.techFitScore = techFitScore;
    }

    public Double getSkillFitScore() {
        return skillFitScore;
    }

    public void setSkillFitScore(Double skillFitScore) {
        this.skillFitScore = skillFitScore;
    }

    public Double getProficiencyScore() {
        return proficiencyScore;
    }

    public void setProficiencyScore(Double proficiencyScore) {
        this.proficiencyScore = proficiencyScore;
    }

    public Double getExperienceFitScore() {
        return experienceFitScore;
    }

    public void setExperienceFitScore(Double experienceFitScore) {
        this.experienceFitScore = experienceFitScore;
    }

    public Double getTeamFitScore() {
        return teamFitScore;
    }

    public void setTeamFitScore(Double teamFitScore) {
        this.teamFitScore = teamFitScore;
    }

    public Double getTimeFitScore() {
        return timeFitScore;
    }

    public void setTimeFitScore(Double timeFitScore) {
        this.timeFitScore = timeFitScore;
    }

    public Double getPenaltyScore() {
        return penaltyScore;
    }

    public void setPenaltyScore(Double penaltyScore) {
        this.penaltyScore = penaltyScore;
    }

    public String getRecommendationReasons() {
        return recommendationReasons;
    }

    public void setRecommendationReasons(String recommendationReasons) {
        this.recommendationReasons = recommendationReasons;
    }

    public String getIdentifiedGaps() {
        return identifiedGaps;
    }

    public void setIdentifiedGaps(String identifiedGaps) {
        this.identifiedGaps = identifiedGaps;
    }

    public String getScoreExplanation() {
        return scoreExplanation;
    }

    public void setScoreExplanation(String scoreExplanation) {
        this.scoreExplanation = scoreExplanation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentProjectRecommendation other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
