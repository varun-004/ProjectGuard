package com.projectguard.dto.recommendation;

import java.util.ArrayList;
import java.util.List;

/**
 * Response wrapper returned by POST /api/recommendations/generate-fresh.
 * Contains the freshly generated and scored project recommendations together
 * with generation metadata.
 */
public class GenerateFreshProjectsResponse {

    private Long studentProfileId;
    private String studentName;
    private String branchName;
    private String domainName;
    private String technologyName;

    private int requestedCount;
    private int generatedCount;
    private int savedCount;

    /** Human-readable note about this generation run. */
    private String message;

    /** Safe user-facing error detail when generation partially or fully fails. */
    private String errorDetail;

    /** True when the Gemini API was reachable and contributed to generation. */
    private boolean geminiContributed;

    private String generatedAt;

    /** Scored recommendation responses, ranked by the existing RecommendationEngineService. */
    private List<ProjectRecommendationResponse> recommendations = new ArrayList<>();

    public GenerateFreshProjectsResponse() {
    }

    public Long getStudentProfileId() { return studentProfileId; }
    public void setStudentProfileId(Long studentProfileId) { this.studentProfileId = studentProfileId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }

    public String getTechnologyName() { return technologyName; }
    public void setTechnologyName(String technologyName) { this.technologyName = technologyName; }

    public int getRequestedCount() { return requestedCount; }
    public void setRequestedCount(int requestedCount) { this.requestedCount = requestedCount; }

    public int getGeneratedCount() { return generatedCount; }
    public void setGeneratedCount(int generatedCount) { this.generatedCount = generatedCount; }

    public int getSavedCount() { return savedCount; }
    public void setSavedCount(int savedCount) { this.savedCount = savedCount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getErrorDetail() { return errorDetail; }
    public void setErrorDetail(String errorDetail) { this.errorDetail = errorDetail; }

    public boolean isGeminiContributed() { return geminiContributed; }
    public void setGeminiContributed(boolean geminiContributed) { this.geminiContributed = geminiContributed; }

    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }

    public List<ProjectRecommendationResponse> getRecommendations() { return recommendations; }
    public void setRecommendations(List<ProjectRecommendationResponse> recommendations) {
        this.recommendations = recommendations;
    }
}
