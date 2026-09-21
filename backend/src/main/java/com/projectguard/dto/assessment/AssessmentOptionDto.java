package com.projectguard.dto.assessment;

import java.util.List;

public class AssessmentOptionDto {
    private Long id;
    private String optionText;

    public AssessmentOptionDto() {}

    public AssessmentOptionDto(Long id, String optionText) {
        this.id = id;
        this.optionText = optionText;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }
}
