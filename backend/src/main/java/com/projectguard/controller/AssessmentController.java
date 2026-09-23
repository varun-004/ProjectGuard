package com.projectguard.controller;

import com.projectguard.dto.assessment.AssessmentSubmitRequest;
import com.projectguard.dto.assessment.DomainAssessmentResultDto;
import com.projectguard.dto.student.SkillResponse;
import com.projectguard.service.AssessmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping("/skills")
    public ResponseEntity<List<SkillResponse>> getRelevantSkills(
            @RequestParam(required = false) Long technologyId,
            Authentication authentication) {
        return ResponseEntity.ok(
                assessmentService.getRelevantSkills(authentication.getName(), technologyId)
        );
    }

    @GetMapping("/questions")
    public ResponseEntity<com.projectguard.dto.assessment.AssessmentAttemptResponse> getQuestionsForSkills(
            @RequestParam List<Long> skillIds,
            Authentication authentication) {
        return ResponseEntity.ok(
                assessmentService.getQuestionsForSkills(skillIds, authentication.getName())
        );
    }

    @PostMapping("/submit")
    public ResponseEntity<DomainAssessmentResultDto> submitAssessment(
            @RequestBody AssessmentSubmitRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                assessmentService.submitAssessment(authentication.getName(), request)
        );
    }

    @GetMapping("/my-results")
    public ResponseEntity<Page<DomainAssessmentResultDto>> getMyResults(
            Authentication authentication,
            Pageable pageable) {
        return ResponseEntity.ok(
                assessmentService.getMyResults(authentication.getName(), pageable)
        );
    }
}

