package com.mem0.core.vectorstore;

import java.util.Map;

/**
 * Represents a search result from the vector store.
 *
 * @param id the vector ID
 * @param score the similarity score
 * @param payload the payload (metadata)
 * @author MoBai

 */
public record SearchResult(
    String id,
    Double score,
    String hash,
    Map<String, Object> payload) {
}
