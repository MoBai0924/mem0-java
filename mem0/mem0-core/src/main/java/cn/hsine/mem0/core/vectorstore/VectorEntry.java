package cn.hsine.mem0.core.vectorstore;

import java.util.Map;

/**
 * Represents a vector entry in the vector store.
 *
 * @param id the vector ID
 * @param vector the vector values
 * @param payload the payload (metadata)
 * @author MoBai

 */
public record VectorEntry(
    String id,
    Double[] vector,
    Map<String, Object> payload
) {
}
