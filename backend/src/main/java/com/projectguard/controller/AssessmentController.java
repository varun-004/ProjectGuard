package com.projectguard.controller;

import com.projectguard.dto.assessment.AssessmentQuestionDto;
import com.projectguard.dto.assessment.AssessmentResultDto;
import com.projectguard.dto.assessment.AssessmentSubmitRequest;
import com.projectguard.dto.assessment.DomainAssessmentResultDto;
import com.projectguard.dto.student.SkillResponse;
import com.projectguard.service.AssessmentService;
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

    /** GET /api/assessments/skills?technologyId={id} — get skills relevant to the user's domain, optionally scoped to a technology */
    @GetMapping("/skills")
    public ResponseEntity<List<SkillResponse>> getRelevantSkills(
            @RequestParam(required = false) Long technologyId,
            Authentication authentication) {
        return ResponseEntity.ok(assessmentService.getRelevantSkills(authentication.getName(), technologyId));
    }

    /** GET /api/assessments/questions?skillIds=1,2,3 — load 10 questions combined for multiple skills */
    @GetMapping("/questions")
    public ResponseEntity<com.projectguard.dto.assessment.AssessmentAttemptResponse> getQuestionsForSkills(
            @RequestParam List<Long> skillIds,
            Authentication authentication) {
        return ResponseEntity.ok(assessmentService.getQuestionsForSkills(skillIds, authentication.getName()));
    }

    /** POST /api/assessments/submit — submit answers, receive overall and skill-wise results */
    @PostMapping("/submit")
    public ResponseEntity<DomainAssessmentResultDto> submitAssessment(
            @RequestBody AssessmentSubmitRequest request,
            Authentication authentication) {
        
        return ResponseEntity.ok(
                assessmentService.submitAssessment(authentication.getName(), request));
    }

    /** GET /api/assessments/my-results — fetch the authenticated user's past domain assessment results */
    @GetMapping("/my-results")
    public ResponseEntity<List<DomainAssessmentResultDto>> getMyResults(Authentication authentication) {
        return ResponseEntity.ok(assessmentService.getMyResults(authentication.getName()));
    }
}
