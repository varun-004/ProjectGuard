package com.projectguard.dto.recommendation;

import java.util.ArrayList;
import java.util.List;

public class ProjectRecommendationResponse {
    private Long recommendationId;
    private Long projectId;
    private Integer rankOrder;
    private Double matchScore;
    private Double qualityScore;
    private String title;
    private String description;
    private Long domainId;
    private String domainName;
    private String complexityLevel;
    private String academicLevelSuitability;
    private Integer minTeamSize;
    private Integer maxTeamSize;
    private Integer estimatedWeeks;

    private List<String> targetBranches = new ArrayList<>();
    private List<String> requiredSkills = new ArrayList<>();
    private List<String> technologies = new ArrayList<>();

    private ScoreBreakdownDto scoreBreakdown;
    private String scoreExplanation;
    private List<String> recommendationReasons = new ArrayList<>();
    private List<String> identifiedGaps = new ArrayList<>();
    private List<AssessedSkillMatchDto> assessedSkillsMatch = new ArrayList<>();

    // 14 documented dimensions
    private String problemStatement;
    private String targetStakeholders;
    private String proposedSolution;
    private String expectedOutcomeKpis;
    private String industryValue;
    private String innovationDifferentiator;
    private String dataRequirements;
    private String functionalRequirements;
    private String nonFunctionalRequirements;
    private String constraintsDependencies;
    private String testingValidation;
    private String securityPrivacySafety;
    private String scalabilityIntegration;
    private String costSustainability;
    private String deploymentAcademicDepth;

    private String prerequisites;
    private Boolean qualityApproved;
    private String qualityEvaluation;

    private Double suitabilityScore;
    private Double feasibilityScore;
    private Boolean isFreshAlternative = false;
    private Boolean isGeneratedIdea = false;

    public ProjectRecommendationResponse() {}

    public Long getRecommendationId() { return recommendationId; }
    public void setRecommendationId(Long recommendationId) { this.recommendationId = recommendationId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Integer getRankOrder() { return rankOrder; }
    public void setRankOrder(Integer rankOrder) { this.rankOrder = rankOrder; }

    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
        this.suitabilityScore = matchScore;
    }

    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getDomainId() { return domainId; }
    public void setDomainId(Long domainId) { this.domainId = domainId; }

    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }

    public String getComplexityLevel() { return complexityLevel; }
    public void setComplexityLevel(String complexityLevel) { this.complexityLevel = complexityLevel; }

    public String getAcademicLevelSuitability() { return academicLevelSuitability; }
    public void setAcademicLevelSuitability(String academicLevelSuitability) { this.academicLevelSuitability = academicLevelSuitability; }

    public Integer getMinTeamSize() { return minTeamSize; }
    public void setMinTeamSize(Integer minTeamSize) { this.minTeamSize = minTeamSize; }

    public Integer getMaxTeamSize() { return maxTeamSize; }
    public void setMaxTeamSize(Integer maxTeamSize) { this.maxTeamSize = maxTeamSize; }

    public Integer getEstimatedWeeks() { return estimatedWeeks; }
    public void setEstimatedWeeks(Integer estimatedWeeks) { this.estimatedWeeks = estimatedWeeks; }

    public List<String> getTargetBranches() { return targetBranches; }
    public void setTargetBranches(List<String> targetBranches) { this.targetBranches = targetBranches; }

    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }

    public List<String> getTechnologies() { return technologies; }
    public void setTechnologies(List<String> technologies) { this.technologies = technologies; }

    public ScoreBreakdownDto getScoreBreakdown() { return scoreBreakdown; }
    public void setScoreBreakdown(ScoreBreakdownDto scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; }

    public String getScoreExplanation() { return scoreExplanation; }
    public void setScoreExplanation(String scoreExplanation) { this.scoreExplanation = scoreExplanation; }

    public List<String> getRecommendationReasons() { return recommendationReasons; }
    public void setRecommendationReasons(List<String> recommendationReasons) { this.recommendationReasons = recommendationReasons; }

    public List<String> getIdentifiedGaps() { return identifiedGaps; }
    public void setIdentifiedGaps(List<String> identifiedGaps) { this.identifiedGaps = identifiedGaps; }

    public List<AssessedSkillMatchDto> getAssessedSkillsMatch() { return assessedSkillsMatch; }
    public void setAssessedSkillsMatch(List<AssessedSkillMatchDto> assessedSkillsMatch) { this.assessedSkillsMatch = assessedSkillsMatch; }

    public String getProblemStatement() { return problemStatement; }
    public void setProblemStatement(String problemStatement) { this.problemStatement = problemStatement; }

    public String getTargetStakeholders() { return targetStakeholders; }
    public void setTargetStakeholders(String targetStakeholders) { this.targetStakeholders = targetStakeholders; }

    public String getProposedSolution() { return proposedSolution; }
    public void setProposedSolution(String proposedSolution) { this.proposedSolution = proposedSolution; }

    public String getExpectedOutcomeKpis() { return expectedOutcomeKpis; }
    public void setExpectedOutcomeKpis(String expectedOutcomeKpis) { this.expectedOutcomeKpis = expectedOutcomeKpis; }

    public String getIndustryValue() { return industryValue; }
    public void setIndustryValue(String industryValue) { this.industryValue = industryValue; }

    public String getInnovationDifferentiator() { return innovationDifferentiator; }
    public void setInnovationDifferentiator(String innovationDifferentiator) { this.innovationDifferentiator = innovationDifferentiator; }

    public String getDataRequirements() { return dataRequirements; }
    public void setDataRequirements(String dataRequirements) { this.dataRequirements = dataRequirements; }

    public String getFunctionalRequirements() { return functionalRequirements; }
    public void setFunctionalRequirements(String functionalRequirements) { this.functionalRequirements = functionalRequirements; }

    public String getNonFunctionalRequirements() { return nonFunctionalRequirements; }
    public void setNonFunctionalRequirements(String nonFunctionalRequirements) { this.nonFunctionalRequirements = nonFunctionalRequirements; }

    public String getConstraintsDependencies() { return constraintsDependencies; }
    public void setConstraintsDependencies(String constraintsDependencies) { this.constraintsDependencies = constraintsDependencies; }

    public String getTestingValidation() { return testingValidation; }
    public void setTestingValidation(String testingValidation) { this.testingValidation = testingValidation; }

    public String getSecurityPrivacySafety() { return securityPrivacySafety; }
    public void setSecurityPrivacySafety(String securityPrivacySafety) { this.securityPrivacySafety = securityPrivacySafety; }

    public String getScalabilityIntegration() { return scalabilityIntegration; }
    public void setScalabilityIntegration(String scalabilityIntegration) { this.scalabilityIntegration = scalabilityIntegration; }

    public String getCostSustainability() { return costSustainability; }
    public void setCostSustainability(String costSustainability) { this.costSustainability = costSustainability; }

    public String getDeploymentAcademicDepth() { return deploymentAcademicDepth; }
    public void setDeploymentAcademicDepth(String deploymentAcademicDepth) { this.deploymentAcademicDepth = deploymentAcademicDepth; }

    public String getPrerequisites() { return prerequisites; }
    public void setPrerequisites(String prerequisites) { this.prerequisites = prerequisites; }

    public Boolean getQualityApproved() { return qualityApproved; }
    public void setQualityApproved(Boolean qualityApproved) { this.qualityApproved = qualityApproved; }

    public String getQualityEvaluation() { return qualityEvaluation; }
    public void setQualityEvaluation(String qualityEvaluation) { this.qualityEvaluation = qualityEvaluation; }

    public Double getSuitabilityScore() { return suitabilityScore; }
    public void setSuitabilityScore(Double suitabilityScore) { this.suitabilityScore = suitabilityScore; }

    public Double getFeasibilityScore() { return feasibilityScore; }
    public void setFeasibilityScore(Double feasibilityScore) { this.feasibilityScore = feasibilityScore; }

    public Boolean getIsFreshAlternative() { return isFreshAlternative; }
    public void setIsFreshAlternative(Boolean isFreshAlternative) { this.isFreshAlternative = isFreshAlternative; }

    public Boolean getIsGeneratedIdea() { return isGeneratedIdea; }
    public void setIsGeneratedIdea(Boolean isGeneratedIdea) { this.isGeneratedIdea = isGeneratedIdea; }
}
