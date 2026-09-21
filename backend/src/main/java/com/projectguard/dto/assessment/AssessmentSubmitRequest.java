package com.projectguard.dto.assessment;

import java.util.List;

public class AssessmentSubmitRequest {
    private List<Long> skillIds;
    private List<AnswerSubmission> answers;

    public static class AnswerSubmission {
        private Long questionId;
        private Integer selectedOptionIndex;

        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }
        public Integer getSelectedOptionIndex() { return selectedOptionIndex; }
        public void setSelectedOptionIndex(Integer selectedOptionIndex) { this.selectedOptionIndex = selectedOptionIndex; }
    }

    public List<Long> getSkillIds() { return skillIds; }
    public void setSkillIds(List<Long> skillIds) { this.skillIds = skillIds; }
    public List<AnswerSubmission> getAnswers() { return answers; }
    public void setAnswers(List<AnswerSubmission> answers) { this.answers = answers; }
}
