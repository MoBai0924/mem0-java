package cn.hsine.mem0.core.entitystore;

import cn.hsine.mem0.core.vectorstore.DistanceMetric;
import cn.hsine.mem0.core.vectorstore.SearchResult;
import cn.hsine.mem0.core.vectorstore.VectorEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 实体关系存储方法
 *
 */
public interface EntityStore {

    /**
     * Creates a collection for storing vectors.
     *
     * @param name       the collection name
     * @param vectorSize the vector dimension
     * @param metric     the distance metric
     */
    void createCollection(String name, int vectorSize, DistanceMetric metric);

    /**
     * Checks if a collection exists.
     *
     * @param name the collection name
     * @return true if exists
     */
    boolean collectionExists(String name);

    /**
     * Deletes a collection.
     *
     * @param name the collection name
     */
    void deleteCollection(String name);

    /**
     * Inserts vectors with payloads.
     *
     * @param vectors  the vectors
     * @param payloads the payloads
     * @param ids      the vector IDs
     */
    void insert(List<Double[]> vectors, List<Map<String, Object>> payloads, List<String> ids);

    /**
     * Inserts a single vector.
     *
     * @param vector  the vector
     * @param payload the payload
     * @param id      the vector ID
     */
    default void insert(Double[] vector, Map<String, Object> payload, String id) {
        insert(List.<Double[]>of(vector), List.of(payload), List.of(id));
    }

    /**
     * Searches for similar vectors.
     *
     * @param queryVector the query vector
     * @param topK        the number of results
     * @param filters     the metadata filters
     * @return the search results
     */
    List<SearchResult> search(Double[] queryVector, int topK, Map<String, Object> filters);

    /**
     * Searches for similar vectors without filters.
     *
     * @param queryVector the query vector
     * @param topK        the number of results
     * @return the search results
     */
    default List<SearchResult> search(Double[] queryVector, int topK) {
        return search(queryVector, topK, null);
    }

    /**
     * Gets a vector by ID.
     *
     * @param id the vector ID
     * @return the vector entry
     */
    Optional<VectorEntry> get(String id);

    /**
     * Updates a vector.
     *
     * @param id      the vector ID
     * @param vector  the new vector
     * @param payload the new payload
     */
    void update(String id, Double[] vector, Map<String, Object> payload);

    /**
     * Deletes a vector.
     *
     * @param id the vector ID
     */
    void delete(String id);

    /**
     * Deletes multiple vectors.
     *
     * @param ids the vector IDs
     */
    default void delete(List<String> ids) {
        ids.forEach(this::delete);
    }

    /**
     * Lists vectors with optional filters.
     *
     * @param filters the metadata filters
     * @param limit   the maximum number of results
     * @return the vector entries
     */
    List<VectorEntry> list(Map<String, Object> filters, int limit);

    /**
     * Lists all vectors.
     *
     * @param limit the maximum number of results
     * @return the vector entries
     */
    default List<VectorEntry> list(int limit) {
        return list(null, limit);
    }

    /**
     * Resets the collection (deletes all vectors).
     */
    void reset();

    /**
     * Counts the number of vectors.
     *
     * @return the count
     */
    long count();

    /**
     * Performs keyword search (BM25 or similar).
     * Optional operation - not all vector stores support this.
     *
     * @param query   the query text
     * @param topK    the number of results
     * @param filters the metadata filters
     * @return the search results
     * @throws UnsupportedOperationException if not supported
     */
    default List<SearchResult> keywordSearch(String query, int topK, Map<String, Object> filters) {
        throw new UnsupportedOperationException("Keyword search not supported by this entity vector store");
    }

    /**
     * Batch search - searches with multiple query vectors.
     * Default implementation calls search() sequentially.
     *
     * @param queryVectors the query vectors
     * @param topK         the number of results per query
     * @param filters      the metadata filters
     * @return list of search result lists, one per query
     */
    default List<List<SearchResult>> searchBatch(List<Double[]> queryVectors, int topK, Map<String, Object> filters) {
        return queryVectors.stream()
                .map(v -> search(v, topK, filters))
                .toList();
    }

    /**
     * Lists all collections.
     * Optional operation.
     *
     * @return the collection names
     */
    default List<String> listCollections() {
        throw new UnsupportedOperationException("List collections not supported by this entity vector store");
    }

    /**
     * Gets collection info.
     * Optional operation.
     *
     * @param name the collection name
     * @return collection metadata
     */
    default Map<String, Object> collectionInfo(String name) {
        throw new UnsupportedOperationException("Collection info not supported by this entity vector store");
    }

    /**
     * Gets the name of this vector store implementation.
     *
     * @return the name
     */
    String getName();

    default List<List<SearchResult>> searchBatch(List<String> query, List<Double[]> queryVectors, int topK, Map<String, Object> filters) {
        return queryVectors.stream()
                .map(v -> search(v, topK, filters))
                .toList();
    }
}
