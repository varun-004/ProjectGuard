package com.projectguard.dto.student;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StudentSkillsRequest {

    @NotNull(message = "Skills list is required")
    @Valid
    private List<StudentSkillRequest> skills = new ArrayList<>();

    public StudentSkillsRequest() {
    }

    public List<StudentSkillRequest> getSkills() {
        return skills;
    }

    public void setSkills(List<StudentSkillRequest> skills) {
        this.skills = skills;
    }
}
