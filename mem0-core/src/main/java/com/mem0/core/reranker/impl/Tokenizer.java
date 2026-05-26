package com.mem0.core.reranker.impl;

/**
 * Tokenizer interface for ONNX-based rerankers.
 * Converts (query, document) pairs into token IDs for model input.
 *
 * @author MoBai

 */
public interface Tokenizer {

    /**
     * Result of tokenization.
     *
     * @param inputIds      token IDs
     * @param attentionMask attention mask (1 for real tokens, 0 for padding)
     * @param tokenTypeIds  token type IDs (0 for query, 1 for document)
     */
    record TokenizationResult(long[] inputIds, long[] attentionMask, long[] tokenTypeIds) {}

    /**
     * Tokenizes a query-document pair for cross-encoder input.
     *
     * @param query    the search query
     * @param document the document text
     * @return tokenization result
     */
    TokenizationResult tokenize(String query, String document);
}
