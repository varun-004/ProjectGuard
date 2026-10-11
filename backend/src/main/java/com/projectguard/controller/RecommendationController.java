package com.projectguard.controller;

import com.projectguard.dto.recommendation.ProjectRecommendationResponse;
import com.projectguard.dto.recommendation.RecommendationListResponse;
import com.projectguard.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
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
}
