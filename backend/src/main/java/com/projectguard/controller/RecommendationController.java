package com.projectguard.controller;

import com.projectguard.dto.recommendation.GenerateFreshProjectsRequest;
import com.projectguard.dto.recommendation.GenerateFreshProjectsResponse;
import com.projectguard.dto.recommendation.ProjectRecommendationResponse;
import com.projectguard.dto.recommendation.RecommendationListResponse;
import com.projectguard.service.AiProjectGeneratorService;
import com.projectguard.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final AiProjectGeneratorService aiProjectGeneratorService;

    public RecommendationController(RecommendationService recommendationService,
                                    AiProjectGeneratorService aiProjectGeneratorService) {
        this.recommendationService = recommendationService;
        this.aiProjectGeneratorService = aiProjectGeneratorService;
    }

    /**
     * Get personalized project recommendations for the authenticated student.
     * Use refresh=true to recalculate rule-based matching.
     */
    @GetMapping
    public ResponseEntity<RecommendationListResponse> getMyRecommendations(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean refresh) {
        String username = authentication.getName();
        RecommendationListResponse response = recommendationService.getRecommendationsForUser(username, refresh);
        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed recommendation breakdown and full real-world dossier for a project.
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectRecommendationResponse> getRecommendationDetail(
            Authentication authentication,
            @PathVariable Long projectId) {
        String username = authentication.getName();
        ProjectRecommendationResponse response =
                recommendationService.getProjectRecommendationDetail(username, projectId);
        return ResponseEntity.ok(response);
    }

    /**
     * Generate fresh, personalized project ideas using the Gemini AI backend.
     *
     * <p>The authenticated student's identity is resolved exclusively from the
     * Security Context — never from a client-supplied student ID.
     *
     * <p>Request count is validated at 1–5 (default 3). The response contains
     * scored project recommendations calculated by the existing
     * {@link com.projectguard.service.RecommendationEngineService} (unmodified).
     */
    @PostMapping("/generate-fresh")
    public ResponseEntity<GenerateFreshProjectsResponse> generateFreshProjects(
            Authentication authentication,
            @Valid @RequestBody(required = false) GenerateFreshProjectsRequest request) {

        String username = authentication.getName();

        // Default to 3 when no body is supplied
        int count = (request != null) ? request.getCount() : 3;

        GenerateFreshProjectsResponse response =
                aiProjectGeneratorService.generateForUser(username, count);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the saved recommendation and generation history for the authenticated student.
     */
    @GetMapping("/history")
    public ResponseEntity<java.util.List<com.projectguard.dto.recommendation.RecommendationHistoryDto>> getRecommendationHistory(
            Authentication authentication) {
        String username = authentication.getName();
        java.util.List<com.projectguard.dto.recommendation.RecommendationHistoryDto> history =
                aiProjectGeneratorService.getGenerationHistoryForUser(username);
        return ResponseEntity.ok(history);
    }
}
