package com.mem0.core.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to search memories.
 *
 * @param query     the search query
 * @param userId    filter by user ID (optional)
 * @param agentId   filter by agent ID (optional)
 * @param runId     filter by run ID (optional)
 * @param filters   additional metadata filters (optional)
 * @param topK      the number of results to return
 * @param threshold the minimum similarity threshold (0.0 to 1.0)
 * @param rerank    whether to rerank results
 * @author MoBai

 */
public record MemorySearchRequest(
        @NotBlank String query,
        MemorySearchFilters filters,
        @Min(1) int topK,
        @DecimalMin("0.0") @DecimalMax("1.0") double threshold,
        boolean rerank) {
    /**
     * Creates a search request with default parameters.
     *
     * @param query the search query
     * @return the search request
     */
    public static MemorySearchRequest of(String query) {
        return new MemorySearchRequest(query, null, 10, 0.0, false);
    }

    /**
     * Creates a search request with user scope.
     *
     * @param query  the search query
     * @param userId the user ID
     * @return the search request
     */
    public static MemorySearchRequest of(String query, String userId) {
        return new MemorySearchRequest(query, null, 10, 0.0, false);
    }

    record MemorySearchFilters(String userId,
                               String agentId,
                               String runId) {

    }
}
