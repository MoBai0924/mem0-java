package com.mem0.core.embedding;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface for embedding generation.
 * Provides abstraction over different embedding providers.
 *
 * @author MoBai

 */
public interface EmbeddingProvider {

    /**
     * Embeds a single text.
     *
     * @param text the text to embed
     * @return the embedding vector
     */
    Double[] embed(String text);

    /**
     * Embeds multiple texts in batch.
     *
     * @param texts the texts to embed
     * @return the embedding vectors
     */
    List<Double[]> embedBatch(List<String> texts);

    /**
     * Gets the dimension of embeddings produced by this provider.
     *
     * @return the dimension
     */
    int getDimension();

    /**
     * Gets the name of this embedding provider.
     *
     * @return the name
     */
    String getName();
}
