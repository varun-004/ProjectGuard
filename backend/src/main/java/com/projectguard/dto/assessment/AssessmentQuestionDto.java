package com.projectguard.dto.assessment;

import java.util.List;

public class AssessmentQuestionDto {
    private Long id;
    private String questionText;
    private String skillName;
    private String difficulty;
    private List<AssessmentOptionDto> options;

    public AssessmentQuestionDto() {}

    public AssessmentQuestionDto(Long id, String questionText, String skillName, String difficulty, List<AssessmentOptionDto> options) {
        this.id = id;
        this.questionText = questionText;
        this.skillName = skillName;
        this.difficulty = difficulty;
        this.options = options;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public List<AssessmentOptionDto> getOptions() { return options; }
    public void setOptions(List<AssessmentOptionDto> options) { this.options = options; }
}
