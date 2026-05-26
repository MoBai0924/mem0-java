package cn.hsine.mem0.core.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response containing memory data.
 *
 * @param id the memory ID
 * @param memory the memory content
 * @param hash the content hash
 * @param metadata the metadata
 * @param score the relevance score (for search results)
 * @param createdAt the creation timestamp
 * @param updatedAt the update timestamp
 * @author MoBai

 */
public record MemoryResponse(
    String id,
    String memory,
    String hash,
    Map<String, Object> metadata,
    Double score,
    String createdAt,
    String updatedAt
) {

}
