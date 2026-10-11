package com.projectguard.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "student_profile_id", nullable = true)
    private StudentProfile studentProfile;

    @Column(name = "is_catalog", nullable = false)
    private Boolean isCatalog = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private ProjectDomain domain;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Real-world problem, context and clear pain point
    @Column(name = "problem_statement", columnDefinition = "TEXT")
    private String problemStatement;

    // Target stakeholders and actual use case
    @Column(name = "target_stakeholders", columnDefinition = "TEXT")
    private String targetStakeholders;

    // Proposed solution and real operational workflow
    @Column(name = "proposed_solution", columnDefinition = "TEXT")
    private String proposedSolution;

    // Expected outcome and measurable KPIs
    @Column(name = "expected_outcome_kpis", columnDefinition = "TEXT")
    private String expectedOutcomeKpis;

    // Industry/business/social value
    @Column(name = "industry_value", columnDefinition = "TEXT")
    private String industryValue;

    // Meaningful innovation and differentiation
    @Column(name = "innovation_differentiator", columnDefinition = "TEXT")
    private String innovationDifferentiator;

    // Required data and realistic data sources
    @Column(name = "data_requirements", columnDefinition = "TEXT")
    private String dataRequirements;

    // Functional requirements
    @Column(name = "functional_requirements", columnDefinition = "TEXT")
    private String functionalRequirements;

    // Non-functional requirements
    @Column(name = "non_functional_requirements", columnDefinition = "TEXT")
    private String nonFunctionalRequirements;

    // Constraints, assumptions, risks and dependencies
    @Column(name = "constraints_dependencies", columnDefinition = "TEXT")
    private String constraintsDependencies;

    // Testing and validation approach
    @Column(name = "testing_validation", columnDefinition = "TEXT")
    private String testingValidation;

    // Security, privacy, reliability and safety where relevant
    @Column(name = "security_privacy_safety", columnDefinition = "TEXT")
    private String securityPrivacySafety;

    // Scalability, integration, maintainability and usability
    @Column(name = "scalability_integration", columnDefinition = "TEXT")
    private String scalabilityIntegration;

    // Cost-effectiveness and sustainability where relevant
    @Column(name = "cost_sustainability", columnDefinition = "TEXT")
    private String costSustainability;

    // Deployment potential and academic depth
    @Column(name = "deployment_academic_depth", columnDefinition = "TEXT")
    private String deploymentAcademicDepth;

    // Complexity level: BEGINNER, INTERMEDIATE, ADVANCED
    @Column(name = "complexity_level", length = 30)
    private String complexityLevel = "INTERMEDIATE";

    // Academic level suitability: UG, PG, UG_AND_PG
    @Column(name = "academic_level_suitability", length = 30)
    private String academicLevelSuitability = "UG_AND_PG";

    @Column(name = "min_team_size")
    private Integer minTeamSize = 1;

    @Column(name = "max_team_size")
    private Integer maxTeamSize = 4;

    @Column(name = "estimated_weeks")
    private Integer estimatedWeeks = 12;

    // Quality gate score (0-100)
    @Column(name = "quality_score")
    private Double qualityScore = 85.0;

    @Column(name = "quality_approved")
    private Boolean qualityApproved = true;

    @Column(name = "quality_evaluation", columnDefinition = "TEXT")
    private String qualityEvaluation;

    @Column(name = "prerequisites", columnDefinition = "TEXT")
    private String prerequisites;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_branches",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "branch_id")
    )
    private List<EngineeringBranch> targetBranches = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_skills",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private List<Skill> requiredSkills = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProjectRequirement> requirements = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProjectTechnology> projectTechnologies = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Project() {
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

    public Boolean getIsCatalog() {
        return isCatalog;
    }

    public void setIsCatalog(Boolean isCatalog) {
        this.isCatalog = isCatalog;
    }

    public ProjectDomain getDomain() {
        return domain;
    }

    public void setDomain(ProjectDomain domain) {
        this.domain = domain;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProblemStatement() {
        return problemStatement;
    }

    public void setProblemStatement(String problemStatement) {
        this.problemStatement = problemStatement;
    }

    public String getTargetStakeholders() {
        return targetStakeholders;
    }

    public void setTargetStakeholders(String targetStakeholders) {
        this.targetStakeholders = targetStakeholders;
    }

    public String getProposedSolution() {
        return proposedSolution;
    }

    public void setProposedSolution(String proposedSolution) {
        this.proposedSolution = proposedSolution;
    }

    public String getExpectedOutcomeKpis() {
        return expectedOutcomeKpis;
    }

    public void setExpectedOutcomeKpis(String expectedOutcomeKpis) {
        this.expectedOutcomeKpis = expectedOutcomeKpis;
    }

    public String getIndustryValue() {
        return industryValue;
    }

    public void setIndustryValue(String industryValue) {
        this.industryValue = industryValue;
    }

    public String getInnovationDifferentiator() {
        return innovationDifferentiator;
    }

    public void setInnovationDifferentiator(String innovationDifferentiator) {
        this.innovationDifferentiator = innovationDifferentiator;
    }

    public String getDataRequirements() {
        return dataRequirements;
    }

    public void setDataRequirements(String dataRequirements) {
        this.dataRequirements = dataRequirements;
    }

    public String getFunctionalRequirements() {
        return functionalRequirements;
    }

    public void setFunctionalRequirements(String functionalRequirements) {
        this.functionalRequirements = functionalRequirements;
    }

    public String getNonFunctionalRequirements() {
        return nonFunctionalRequirements;
    }

    public void setNonFunctionalRequirements(String nonFunctionalRequirements) {
        this.nonFunctionalRequirements = nonFunctionalRequirements;
    }

    public String getConstraintsDependencies() {
        return constraintsDependencies;
    }

    public void setConstraintsDependencies(String constraintsDependencies) {
        this.constraintsDependencies = constraintsDependencies;
    }

    public String getTestingValidation() {
        return testingValidation;
    }

    public void setTestingValidation(String testingValidation) {
        this.testingValidation = testingValidation;
    }

    public String getSecurityPrivacySafety() {
        return securityPrivacySafety;
    }

    public void setSecurityPrivacySafety(String securityPrivacySafety) {
        this.securityPrivacySafety = securityPrivacySafety;
    }

    public String getScalabilityIntegration() {
        return scalabilityIntegration;
    }

    public void setScalabilityIntegration(String scalabilityIntegration) {
        this.scalabilityIntegration = scalabilityIntegration;
    }

    public String getCostSustainability() {
        return costSustainability;
    }

    public void setCostSustainability(String costSustainability) {
        this.costSustainability = costSustainability;
    }

    public String getDeploymentAcademicDepth() {
        return deploymentAcademicDepth;
    }

    public void setDeploymentAcademicDepth(String deploymentAcademicDepth) {
        this.deploymentAcademicDepth = deploymentAcademicDepth;
    }

    public String getComplexityLevel() {
        return complexityLevel;
    }

    public void setComplexityLevel(String complexityLevel) {
        this.complexityLevel = complexityLevel;
    }

    public String getAcademicLevelSuitability() {
        return academicLevelSuitability;
    }

    public void setAcademicLevelSuitability(String academicLevelSuitability) {
        this.academicLevelSuitability = academicLevelSuitability;
    }

    public Integer getMinTeamSize() {
        return minTeamSize;
    }

    public void setMinTeamSize(Integer minTeamSize) {
        this.minTeamSize = minTeamSize;
    }

    public Integer getMaxTeamSize() {
        return maxTeamSize;
    }

    public void setMaxTeamSize(Integer maxTeamSize) {
        this.maxTeamSize = maxTeamSize;
    }

    public Integer getEstimatedWeeks() {
        return estimatedWeeks;
    }

    public void setEstimatedWeeks(Integer estimatedWeeks) {
        this.estimatedWeeks = estimatedWeeks;
    }

    public Double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Boolean getQualityApproved() {
        return qualityApproved;
    }

    public void setQualityApproved(Boolean qualityApproved) {
        this.qualityApproved = qualityApproved;
    }

    public String getQualityEvaluation() {
        return qualityEvaluation;
    }

    public void setQualityEvaluation(String qualityEvaluation) {
        this.qualityEvaluation = qualityEvaluation;
    }

    public String getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(String prerequisites) {
        this.prerequisites = prerequisites;
    }

    public List<EngineeringBranch> getTargetBranches() {
        return targetBranches;
    }

    public void setTargetBranches(List<EngineeringBranch> targetBranches) {
        this.targetBranches = targetBranches;
    }

    public List<Skill> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<Skill> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public List<ProjectRequirement> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<ProjectRequirement> requirements) {
        this.requirements = requirements;
    }

    public List<ProjectTechnology> getProjectTechnologies() {
        return projectTechnologies;
    }

    public void setProjectTechnologies(List<ProjectTechnology> projectTechnologies) {
        this.projectTechnologies = projectTechnologies;
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
        if (!(o instanceof Project other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Project{id=" + id + ", title='" + title + "', isCatalog=" + isCatalog + "}";
    }
}