package com.projectguard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "assessment_attempts")
public class AssessmentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String attemptId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "assessment_attempt_questions",
        joinColumns = @JoinColumn(name = "attempt_id"),
        inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private List<AssessmentQuestion> issuedQuestions = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean submitted = false;

    @Column
    private LocalDateTime submittedAt;

    public AssessmentAttempt() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public StudentProfile getStudentProfile() { return studentProfile; }
    public void setStudentProfile(StudentProfile studentProfile) { this.studentProfile = studentProfile; }

    public List<AssessmentQuestion> getIssuedQuestions() { return issuedQuestions; }
    public void setIssuedQuestions(List<AssessmentQuestion> issuedQuestions) { this.issuedQuestions = issuedQuestions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isSubmitted() { return submitted; }
    public void setSubmitted(boolean submitted) { this.submitted = submitted; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (attemptId == null) {
            attemptId = UUID.randomUUID().toString();
        }
    }
}
