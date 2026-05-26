package com.mem0.core.vectorstore;

/**
 * Distance metrics for vector similarity.
 *
 * @author MoBai

 */
public enum DistanceMetric {
    /**
     * Euclidean distance (L2 norm).
     */
    EUCLIDEAN,

    /**
     * Cosine similarity.
     */
    COSINE,

    /**
     * Inner product (dot product).
     */
    INNER_PRODUCT
}
