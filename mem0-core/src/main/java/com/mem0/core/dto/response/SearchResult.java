package com.mem0.core.dto.response;

import java.util.Map;
import java.util.UUID;

/**
 * Response containing search result data.
 *
 * @param id the memory ID
 * @param memory the memory content
 * @param score the relevance score
 * @param metadata the metadata
 * @param userId the user ID
 * @param agentId the agent ID
 * @param runId the run ID
 * @author MoBai

 */
public record SearchResult(
    UUID id,
    String memory,
    double score,
    Map<String, Object> metadata,
    String userId,
    String agentId,
    String runId
) {
}
