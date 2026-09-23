package com.projectguard.dto.assessment;

import java.util.List;

public class AssessmentAttemptResponse {
    private String attemptId;
    private List<AssessmentQuestionDto> questions;

    public AssessmentAttemptResponse() {}

    public AssessmentAttemptResponse(String attemptId, List<AssessmentQuestionDto> questions) {
        this.attemptId = attemptId;
        this.questions = questions;
    }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }
    public List<AssessmentQuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<AssessmentQuestionDto> questions) { this.questions = questions; }
}
