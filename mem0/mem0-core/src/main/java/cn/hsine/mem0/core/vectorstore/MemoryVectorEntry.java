package cn.hsine.mem0.core.vectorstore;

import java.util.Map;

/**
 * Represents a vector entry in the vector store.
 *
 * @param id the vector ID
 * @param payload the payload (metadata)
 * @author MoBai

 */
public record MemoryVectorEntry(
    String id,
    Map<String, Object> payload
) {
}
