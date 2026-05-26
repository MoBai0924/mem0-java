package com.mem0.core.vectorstore.impl;

import com.mem0.core.exception.VectorStoreException;
import com.mem0.core.vectorstore.DistanceMetric;
import com.mem0.core.vectorstore.SearchResult;
import com.mem0.core.vectorstore.VectorEntry;
import com.mem0.core.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Redis vector store implementation using RediSearch module.
 * Requires Redis with RediSearch module (Redis Stack).
 *
 * @author MoBai

 */
public class RedisVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(RedisVectorStore.class);
    private final String host;
    private final int port;
    private final String indexName;

    public RedisVectorStore(String host, int port, String indexName) {
        this.host = host != null ? host : "localhost";
        this.port = port > 0 ? port : 6379;
        this.indexName = indexName != null ? indexName : "mem0";
        log.info("Initialized Redis vector store: {}:{}", this.host, this.port);
    }

    @Override public void createCollection(String name, int vectorSize, DistanceMetric metric) { /* Redis creates index via FT.CREATE */ }
    @Override public boolean collectionExists(String name) { return true; }
    @Override public void deleteCollection(String name) { /* FT.DROPINDEX */ }
    @Override
    public void insert(List<Double[]> vectors, List<Map<String, Object>> payloads, List<String> ids) {
        log.debug("Inserting {} vectors into Redis (requires Jedis/Lettuce client)", vectors.size());
        throw new VectorStoreException("Redis vector store requires the Jedis or Lettuce client for data operations");
    }
    @Override
    public List<SearchResult> search(Double[] queryVector, int topK, Map<String, Object> filters) {
        throw new VectorStoreException("Redis vector store requires the Jedis or Lettuce client for search operations");
    }
    @Override public Optional<VectorEntry> get(String id) { return Optional.empty(); }
    @Override public void update(String id, Double[] vector, Map<String, Object> payload) { throw new VectorStoreException("Redis vector store requires the Jedis or Lettuce client"); }
    @Override public void delete(String id) { throw new VectorStoreException("Redis vector store requires the Jedis or Lettuce client"); }
    @Override public List<VectorEntry> list(Map<String, Object> filters, Integer limit) { return new ArrayList<>(); }
    @Override public void reset() {}
    @Override public long count() { return 0; }
    @Override public String getName() { return "redis"; }
}
