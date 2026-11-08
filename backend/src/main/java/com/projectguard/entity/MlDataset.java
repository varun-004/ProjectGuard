package com.projectguard.entity;

import com.projectguard.entity.enums.MlOutcomeStatus;
import com.projectguard.entity.enums.MlValidationStatus;
import com.projectguard.entity.enums.ProjectComplexity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ml_dataset",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ml_dataset_student_project",
                columnNames = {"student_profile_id", "project_id"}
        ),
        indexes = {
                @Index(name = "idx_ml_dataset_student", columnList = "student_profile_id"),
                @Index(name = "idx_ml_dataset_project", columnList = "project_id"),
                @Index(name = "idx_ml_dataset_outcome", columnList = "outcome_status"),
                @Index(name = "idx_ml_dataset_val_status", columnList = "validation_status")
        }
)
public class MlDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = true)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;

    // Feature 1: Average of the student's available skill proficiency scores (1.0 - 4.0)
    @Column(name = "student_skill_level")
    private Double studentSkillLevel;

    // Feature 2: Existing Phase 7 skill-gap value (0.0 - 25.0)
    @Column(name = "skill_gap")
    private Double skillGap;

    // Feature 3: EASY, MEDIUM, HARD
    @Enumerated(EnumType.STRING)
    @Column(name = "project_complexity", nullable = false, length = 20)
    private ProjectComplexity projectComplexity = ProjectComplexity.MEDIUM;

    // Feature 4: Number of people working on the project
    @Column(name = "team_size")
    private Integer teamSize;

    // Feature 5: Time available per week in hours
    @Column(name = "available_hours_per_week")
    private Double availableHoursPerWeek;

    // Feature 6: Average familiarity score for the project's required technologies (1.0 - 4.0)
    @Column(name = "technology_familiarity")
    private Double technologyFamiliarity;

    // Feature 7: Total project requirements
    @Column(name = "number_of_requirements")
    private Integer numberOfRequirements;

    // Feature 8: Total project features
    @Column(name = "number_of_features")
    private Integer numberOfFeatures;

    // Feature 9: Relevant previous experience in years
    @Column(name = "previous_experience")
    private Double previousExperience;

    // Feature 10: Estimated project effort in hours
    @Column(name = "estimated_effort")
    private Double estimatedEffort;

    // Feature 11: Target actual outcome: FEASIBLE or NOT_FEASIBLE (nullable until verified)
    @Column(name = "actual_outcome", length = 30)
    private String actualOutcome;

    // Outcome status: PENDING_OUTCOME, FEASIBLE, NOT_FEASIBLE
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome_status", nullable = false, length = 30)
    private MlOutcomeStatus outcomeStatus = MlOutcomeStatus.PENDING_OUTCOME;

    @Column(name = "data_source", length = 50)
    private String dataSource = "AUTOMATIC_COLLECTION";

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 30)
    private MlValidationStatus validationStatus = MlValidationStatus.PENDING;

    @Column(name = "label_evidence", columnDefinition = "TEXT")
    private String labelEvidence;

    @Column(name = "label_notes", columnDefinition = "TEXT")
    private String labelNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MlDataset() {
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

    public Double getStudentSkillLevel() {
        return studentSkillLevel;
    }

    public void setStudentSkillLevel(Double studentSkillLevel) {
        this.studentSkillLevel = studentSkillLevel;
    }

    public Double getSkillGap() {
        return skillGap;
    }

    public void setSkillGap(Double skillGap) {
        this.skillGap = skillGap;
    }

    public ProjectComplexity getProjectComplexity() {
        return projectComplexity;
    }

    public void setProjectComplexity(ProjectComplexity projectComplexity) {
        this.projectComplexity = projectComplexity;
    }

    public Integer getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(Integer teamSize) {
        this.teamSize = teamSize;
    }

    public Double getAvailableHoursPerWeek() {
        return availableHoursPerWeek;
    }

    public void setAvailableHoursPerWeek(Double availableHoursPerWeek) {
        this.availableHoursPerWeek = availableHoursPerWeek;
    }

    public Double getTechnologyFamiliarity() {
        return technologyFamiliarity;
    }

    public void setTechnologyFamiliarity(Double technologyFamiliarity) {
        this.technologyFamiliarity = technologyFamiliarity;
    }

    public Integer getNumberOfRequirements() {
        return numberOfRequirements;
    }

    public void setNumberOfRequirements(Integer numberOfRequirements) {
        this.numberOfRequirements = numberOfRequirements;
    }

    public Integer getNumberOfFeatures() {
        return numberOfFeatures;
    }

    public void setNumberOfFeatures(Integer numberOfFeatures) {
        this.numberOfFeatures = numberOfFeatures;
    }

    public Double getPreviousExperience() {
        return previousExperience;
    }

    public void setPreviousExperience(Double previousExperience) {
        this.previousExperience = previousExperience;
    }

    public Double getEstimatedEffort() {
        return estimatedEffort;
    }

    public void setEstimatedEffort(Double estimatedEffort) {
        this.estimatedEffort = estimatedEffort;
    }

    public String getActualOutcome() {
        return actualOutcome;
    }

    public void setActualOutcome(String actualOutcome) {
        this.actualOutcome = actualOutcome;
    }

    public MlOutcomeStatus getOutcomeStatus() {
        return outcomeStatus;
    }

    public void setOutcomeStatus(MlOutcomeStatus outcomeStatus) {
        this.outcomeStatus = outcomeStatus;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public MlValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(MlValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getLabelEvidence() {
        return labelEvidence;
    }

    public void setLabelEvidence(String labelEvidence) {
        this.labelEvidence = labelEvidence;
    }

    public String getLabelNotes() {
        return labelNotes;
    }

    public void setLabelNotes(String labelNotes) {
        this.labelNotes = labelNotes;
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

