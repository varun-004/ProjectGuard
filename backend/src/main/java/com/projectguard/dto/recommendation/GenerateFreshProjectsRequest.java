package com.projectguard.dto.recommendation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request payload for POST /api/recommendations/generate-fresh.
 * <p>
 * The student identity is never supplied by the client; it is resolved
 * exclusively from the authenticated Security Context.
 */
public class GenerateFreshProjectsRequest {

    /**
     * Number of fresh project ideas to generate. Default 3, maximum 5.
     */
    @Min(value = 1, message = "count must be at least 1")
    @Max(value = 5, message = "count must not exceed 5 (maximum generation limit)")
    private int count = 3;

    public GenerateFreshProjectsRequest() {
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
