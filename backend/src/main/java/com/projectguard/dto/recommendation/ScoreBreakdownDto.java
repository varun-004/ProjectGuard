package com.projectguard.dto.recommendation;

public class ScoreBreakdownDto {
    private Double domainFitScore;
    private Double techFitScore;
    private Double skillFitScore;
    private Double proficiencyScore;
    private Double experienceFitScore;
    private Double teamFitScore;
    private Double timeFitScore;
    private Double penaltyScore;
    private Double totalMatchScore;

    public ScoreBreakdownDto() {}

    public ScoreBreakdownDto(Double domainFitScore, Double techFitScore, Double skillFitScore,
                             Double proficiencyScore, Double experienceFitScore, Double teamFitScore,
                             Double timeFitScore, Double penaltyScore, Double totalMatchScore) {
        this.domainFitScore = domainFitScore;
        this.techFitScore = techFitScore;
        this.skillFitScore = skillFitScore;
        this.proficiencyScore = proficiencyScore;
        this.experienceFitScore = experienceFitScore;
        this.teamFitScore = teamFitScore;
        this.timeFitScore = timeFitScore;
        this.penaltyScore = penaltyScore;
        this.totalMatchScore = totalMatchScore;
    }

    public Double getDomainFitScore() { return domainFitScore; }
    public void setDomainFitScore(Double domainFitScore) { this.domainFitScore = domainFitScore; }

    public Double getTechFitScore() { return techFitScore; }
    public void setTechFitScore(Double techFitScore) { this.techFitScore = techFitScore; }

    public Double getSkillFitScore() { return skillFitScore; }
    public void setSkillFitScore(Double skillFitScore) { this.skillFitScore = skillFitScore; }

    public Double getProficiencyScore() { return proficiencyScore; }
    public void setProficiencyScore(Double proficiencyScore) { this.proficiencyScore = proficiencyScore; }

    public Double getExperienceFitScore() { return experienceFitScore; }
    public void setExperienceFitScore(Double experienceFitScore) { this.experienceFitScore = experienceFitScore; }

    public Double getTeamFitScore() { return teamFitScore; }
    public void setTeamFitScore(Double teamFitScore) { this.teamFitScore = teamFitScore; }

    public Double getTimeFitScore() { return timeFitScore; }
    public void setTimeFitScore(Double timeFitScore) { this.timeFitScore = timeFitScore; }

    public Double getPenaltyScore() { return penaltyScore; }
    public void setPenaltyScore(Double penaltyScore) { this.penaltyScore = penaltyScore; }

    public Double getTotalMatchScore() { return totalMatchScore; }
    public void setTotalMatchScore(Double totalMatchScore) { this.totalMatchScore = totalMatchScore; }
}
