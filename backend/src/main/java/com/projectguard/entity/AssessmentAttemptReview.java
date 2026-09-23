package com.projectguard.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "assessment_attempt_reviews")
public class AssessmentAttemptReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_assessment_result_id", nullable = false)
    private DomainAssessmentResult domainAssessmentResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    private Integer selectedOptionIndex;

    private Boolean isCorrect;

    public AssessmentAttemptReview() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DomainAssessmentResult getDomainAssessmentResult() { return domainAssessmentResult; }
    public void setDomainAssessmentResult(DomainAssessmentResult domainAssessmentResult) { this.domainAssessmentResult = domainAssessmentResult; }
    public AssessmentQuestion getQuestion() { return question; }
    public void setQuestion(AssessmentQuestion question) { this.question = question; }
    public Integer getSelectedOptionIndex() { return selectedOptionIndex; }
    public void setSelectedOptionIndex(Integer selectedOptionIndex) { this.selectedOptionIndex = selectedOptionIndex; }
    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean correct) { isCorrect = correct; }
}
