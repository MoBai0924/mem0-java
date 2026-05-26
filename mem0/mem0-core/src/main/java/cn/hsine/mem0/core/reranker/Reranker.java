package cn.hsine.mem0.core.reranker;

import java.util.List;
import java.util.Map;

/**
 * Base interface for reranking search results.
 * Ported from Python mem0/reranker/base.py.
 *
 * @author MoBai

 */
public interface Reranker {

    /**
     * Reranks documents based on relevance to the query.
     *
     * @param query     the search query
     * @param documents the documents to rerank (each with "memory"/"text"/"content" and "score" fields)
     * @param topK      maximum number of results to return
     * @return reranked documents with added "rerank_score" field
     */
    List<Map<String, Object>> rerank(String query, List<Map<String, Object>> documents, int topK);

    /**
     * Gets the name of this reranker.
     */
    String getName();
}
