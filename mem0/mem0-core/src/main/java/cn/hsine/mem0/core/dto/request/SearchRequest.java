package cn.hsine.mem0.core.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to search memories.
 *
 * @param query     the search query
 * @param filters   additional metadata filters (optional)
 * @param topK      the number of results to return
 * @param threshold the minimum similarity threshold (0.0 to 1.0)
 * @param rerank    whether to rerank results
 * @author MoBai

 */
public record SearchRequest(
        @NotBlank String query,
        SearchFilters filters,
        @Min(1) Integer topK,
        @DecimalMin("0.0") @DecimalMax("1.0") double threshold,
        boolean rerank) {
    /**
     * Creates a search request with default parameters.
     *
     * @param query the search query
     * @return the search request
     */
    public static SearchRequest of(String query) {
        return new SearchRequest(query, null, 10, 0.0, false);
    }

    /**
     * Creates a search request with user scope.
     *
     * @param query  the search query
     * @param userId the user ID
     * @return the search request
     */
    public static SearchRequest of(String query, String userId) {
        return new SearchRequest(query, new SearchFilters(userId, null, null),
                10, 0.0, false);
    }
}
