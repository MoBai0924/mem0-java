package cn.hsine.mem0.core.vectorstore.impl;

import cn.hsine.mem0.core.vectorstore.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hsine.mem0.core.exception.VectorStoreException;
import cn.hsine.mem0.core.vectorstore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PostgreSQL pgvector implementation of VectorStore.
 * Uses native SQL queries for vector operations.
 *
 * @author MoBai

 */
public class PgVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(PgVectorStore.class);

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private int vectorSize;
    private DistanceMetric distanceMetric;

    /**
     * Creates a new PgVectorStore.
     *
     * @param jdbcTemplate the JDBC template
     * @param tableName    the table name
     */
    public PgVectorStore(JdbcTemplate jdbcTemplate, String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    @Override
    public void createCollection(String name, int vectorSize, DistanceMetric metric) {
        log.info("Creating collection {} with vector size {} and metric {}", name, vectorSize, metric);

        this.vectorSize = vectorSize;
        this.distanceMetric = metric;

        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                        "id VARCHAR(255) PRIMARY KEY, " +
                        "vector vector(%d), " +
                        "payload JSONB, " +
                        "created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP" +
                        ")",
                tableName, vectorSize
        );

        try {
            jdbcTemplate.execute(sql);

            // Create vector index based on metric
            String indexSql = String.format(
                    "CREATE INDEX IF NOT EXISTS idx_%s_vector ON %s USING hnsw (vector %s)",
                    tableName, tableName, getOperatorClass(metric)
            );
            jdbcTemplate.execute(indexSql);

            // Create GIN index for JSONB payload
            String payloadIndexSql = String.format(
                    "CREATE INDEX IF NOT EXISTS idx_%s_payload ON %s USING gin (payload)",
                    tableName, tableName
            );
            jdbcTemplate.execute(payloadIndexSql);

            log.info("Collection {} created successfully", name);
        } catch (Exception e) {
            throw new VectorStoreException("Failed to create collection: " + name, e);
        }
    }

    @Override
    public boolean collectionExists(String name) {
        String sql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
        try {
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, tableName));
        } catch (Exception e) {
            throw new VectorStoreException("Failed to check collection existence: " + name, e);
        }
    }

    @Override
    public void deleteCollection(String name) {
        log.info("Deleting collection {}", name);
        String sql = String.format("DROP TABLE IF EXISTS %s", tableName);
        try {
            jdbcTemplate.execute(sql);
            log.info("Collection {} deleted successfully", name);
        } catch (Exception e) {
            throw new VectorStoreException("Failed to delete collection: " + name, e);
        }
    }

    @Override
    public void insert(List<Double[]> vectors, List<Map<String, Object>> payloads, List<String> ids) {

        log.info("Inserting {} vectors into collection {}", vectors.size(), tableName);

        String sql = String.format(
                "INSERT INTO %s (id, vector, payload) VALUES (?, ?::vector, ?::jsonb) " +
                        "ON CONFLICT (id) DO UPDATE SET vector = EXCLUDED.vector, payload = EXCLUDED.payload",
                tableName
        );

        // 批量参数
        List<Object[]> batchArgs = IntStream.range(0, vectors.size())
                .mapToObj(i -> {
                    String id = ids.get(i);
                    Double[] vector = vectors.get(i);
                    String payloadJson = toJson(payloads.get(i));
                    return new Object[]{id, vector, payloadJson};
                })
                .toList();

        // 批量插入（等价 executemany / execute_values）
        jdbcTemplate.batchUpdate(sql, batchArgs);

    }

    /**
     * 转 JSON（等价 json.dumps）
     */
    private String toJson(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Payload 转 JSON 失败", e);
            return "{}";
        }
    }

    @Override
    public List<SearchResult> search(Double[] queryVector, int topK, Map<String, Object> filters) {
        log.debug("Searching for top {} vectors with filters: {}", topK, filters);

        String distanceOperator = getDistanceOperator(distanceMetric == null ? DistanceMetric.COSINE : distanceMetric);
        StringBuilder sql = new StringBuilder(String.format(
                "SELECT id, 1 - (vector %s ?::vector) as score, payload FROM %s",
                distanceOperator, tableName
        ));

        List<Object> params = new ArrayList<>();
        params.add(vectorToString(queryVector));

        if (filters != null && !filters.isEmpty()) {
            String whereClause = buildWhereClause(filters, params);
            sql.append(" WHERE ").append(whereClause);
        }

        sql.append(" ORDER BY vector ").append(distanceOperator).append(" ?::vector LIMIT ?");
        params.add(vectorToString(queryVector));
        params.add(topK);

        try {
            return jdbcTemplate.query(sql.toString(), searchResultRowMapper(), params.toArray());
        } catch (Exception e) {
            throw new VectorStoreException("Failed to search vectors", e);
        }
    }

    @Override
    public Optional<VectorEntry> get(String id) {
        log.debug("Getting vector with id: {}", id);

        //vector(value::double precision) 低版本的pgvector不支持，手动转换。
        String sql = String.format("SELECT id, vector, payload FROM %s WHERE id = ?", tableName);

        try {
            List<VectorEntry> results = jdbcTemplate.query(sql, vectorEntryRowMapper(), id);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new VectorStoreException("Failed to get vector: " + id, e);
        }
    }

    @Override
    public void update(String id, Double[] vector, Map<String, Object> payload) {
        log.debug("Updating vector with id: {}", id);

        String sql = String.format(
                "UPDATE %s SET vector = ?::vector, payload = ?::jsonb WHERE id = ?",
                tableName
        );

        try {
            String vectorStr = vectorToString(vector);
            String payloadStr = payloadToString(payload);
            jdbcTemplate.update(sql, vectorStr, payloadStr, id);
        } catch (Exception e) {
            throw new VectorStoreException("Failed to update vector: " + id, e);
        }
    }

    @Override
    public void delete(String id) {
        log.debug("Deleting vector with id: {}", id);

        String sql = String.format("DELETE FROM %s WHERE id = ?", tableName);

        try {
            jdbcTemplate.update(sql, id);
        } catch (Exception e) {
            throw new VectorStoreException("Failed to delete vector: " + id, e);
        }
    }

    /**
     * 查询集合中的所有向量（严格对齐Python逻辑）
     *
     * @param filters 过滤条件（可选）
     * @param limit   返回数量，默认100
     * @return 嵌套列表格式的向量数据 [ [OutputData] ]
     */
    @Override
    public List<VectorEntry> list(Map<String, Object> filters, Integer limit) {
        // 1. 设置默认值：top_k 默认 100（对齐Python）
        limit = Objects.requireNonNullElse(limit, 100);
        List<Object> filterParams = new ArrayList<>();
        List<String> filterConditions = new ArrayList<>();

        // 2. 构建过滤条件：payload->>? = ?（PostgreSQL JSONB 操作符，对齐Python）
        if (filters != null && !filters.isEmpty()) {
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                String key = entry.getKey();
                String value = String.valueOf(entry.getValue());
                // 对应 Python: "payload->>%s = %s"
                filterConditions.add("payload->>? = ?");
                filterParams.add(key);
                filterParams.add(value);
            }
        }

        // 3. 拼接 WHERE 子句（对齐Python filter_clause）
        String filterClause = "";
        if (!filterConditions.isEmpty()) {
            filterClause = "WHERE " + String.join(" AND ", filterConditions);
        }

        // 4. 拼接最终 SQL（完全对齐Python的SQL格式）
        String sql = String.format("""
                SELECT id, vector, payload
                FROM %s
                %s
                LIMIT ?
                """, tableName, filterClause);

        // 5. 构建查询参数：过滤参数 + LIMIT
        List<Object> params = new ArrayList<>(filterParams);
        params.add(limit);

        // 6. 执行查询 + 结果映射（对齐Python cur.fetchall()）

        return jdbcTemplate.query(sql, vectorEntryRowMapper(), params.toArray());
    }

    @Override
    public void reset() {
        log.info("Resetting collection {}", tableName);
        String sql = String.format("TRUNCATE TABLE %s", tableName);
        try {
            jdbcTemplate.execute(sql);
            log.info("Collection {} reset successfully", tableName);
        } catch (Exception e) {
            throw new VectorStoreException("Failed to reset collection", e);
        }
    }

    @Override
    public long count() {
        String sql = String.format("SELECT COUNT(*) FROM %s", tableName);
        try {
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            throw new VectorStoreException("Failed to count vectors", e);
        }
    }

    @Override
    public String getName() {
        return "pgvector";
    }

    // Helper methods

    private String getOperatorClass(DistanceMetric metric) {
        return switch (metric) {
            case EUCLIDEAN -> "vector_l2_ops";
            case COSINE -> "vector_cosine_ops";
            case INNER_PRODUCT -> "vector_ip_ops";
        };
    }

    // vector <=> %s::vector AS distance
    private String getDistanceOperator(DistanceMetric metric) {
        return switch (metric) {
            case EUCLIDEAN -> "<->";      // L2 distance
            case COSINE -> "<=>";         // Cosine distance
            case INNER_PRODUCT -> "<#>";  // Inner product
        };
    }

    private String vectorToString(Double[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(String.format("%.6f", vector[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    private String payloadToString(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        // Simple JSON conversion - in production, use Jackson
        return payload.entrySet().stream()
                .map(e -> String.format("\"%s\": \"%s\"", e.getKey(), e.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private String buildWhereClause(Map<String, Object> filters, List<Object> params) {
        return filters.entrySet().stream()
                .map(e -> {
                    params.add(e.getValue().toString());
                    return String.format("payload->>'%s' = ?", e.getKey());
                })
                .collect(Collectors.joining(" AND "));
    }

    private RowMapper<MemoryVectorEntry> memoryVectorEntryRowMapper() {
        return (rs, rowNum) -> {
            String id = rs.getString("id");
            String payloadStr = rs.getString("payload");
            Map<String, Object> payload = parsePayload(payloadStr);
            return new MemoryVectorEntry(id, payload);
        };
    }

    /*
     * 旧版本 = vector（0.5.1/0.6.0）→ 无 SQL 转换，需要自己手动转换
     */
    private RowMapper<VectorEntry> vectorEntryRowMapper() {
        return (rs, rowNum) -> {
            String id = rs.getString("id");
            //rs.getObject("vector"),对应的PGobject属性;  String type; String value;
            String vectorArray = rs.getString("vector");
            Double[] vector = parse(vectorArray);
            String payloadStr = rs.getString("payload");
            Map<String, Object> payload = parsePayload(payloadStr);
            return new VectorEntry(id, vector, payload);
        };
    }

    public static Double[] parse(String vectorStr) {
        if (vectorStr == null || vectorStr.isBlank() || "[]".equals(vectorStr)) {
            return new Double[0];
        }
        return Arrays.stream(vectorStr.substring(1, vectorStr.length() - 1).split(","))
                .map(String::trim)
                .map(Double::valueOf)
                .toArray(Double[]::new);
    }

    private RowMapper<SearchResult> searchResultRowMapper() {
        return (rs, rowNum) -> {
            String id = rs.getString("id");
            double score = rs.getDouble("score");
            String payloadStr = rs.getString("payload");
            Map<String, Object> payload = parsePayload(payloadStr);
            String hash = "";
            if (payload.containsKey("hash")) {
                hash = String.valueOf(payload.get("hash"));
            }
            return new SearchResult(id, score, hash, payload);
        };
    }

    private Map<String, Object> parsePayload(String payloadStr) {
        // Simple JSON parsing - in production, use Jackson
        if (payloadStr == null || "{}".equals(payloadStr)) {
            return new HashMap<>();
        }
        // For now, return empty map - proper JSON parsing needed
        Map<String, Object> payload = null;
        try {
            payload = new ObjectMapper().readValue(payloadStr, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("读取向量库PgVector payload转换失败");
        }
        return payload;
    }


    @Override
    public List<List<SearchResult>> searchBatch(List<String> query, List<Double[]> queryVectors, int topK, Map<String, Object> filters) {
        try {
            // 批量查询
            return searchBatch(queryVectors, topK, filters);
        } catch (Exception e) {
            log.warn("Batch search failed, falling back to sequential: {}", e.getMessage());

            // 降级：逐条查询
            List<List<SearchResult>> fallbackResults = new ArrayList<>();
            for (Double[] vec : queryVectors) {
                List<SearchResult> res = search(vec, topK, filters);
                fallbackResults.add(res);
            }
            return fallbackResults;
        }
    }

    @Override
    public List<MemoryVectorEntry> listWithoutVector(Map<String, Object> filters, int limit) {

        log.debug("Listing vectors with filters: {} and limit: {}", filters, limit);

        StringBuilder sql = new StringBuilder(String.format("SELECT id, payload FROM %s", tableName));
        List<Object> params = new ArrayList<>();

        if (filters != null && !filters.isEmpty()) {
            String whereClause = buildWhereClause(filters, params);
            sql.append(" WHERE ").append(whereClause);
        }

        sql.append(" LIMIT ?");
        params.add(limit);

        try {
            return jdbcTemplate.query(sql.toString(), memoryVectorEntryRowMapper(), params.toArray());
        } catch (Exception e) {
            throw new VectorStoreException("Failed to list vectors", e);
        }
    }

    @Override
    public Optional<MemoryVectorEntry> getByIdWithoutVector(String id) {
        log.debug("Getting vector with id: {}", id);

        //vector(value::double precision) 低版本的pgvector不支持，手动转换。
        String sql = String.format("SELECT id, payload FROM %s WHERE id = ?", tableName);

        try {
            List<MemoryVectorEntry> results = jdbcTemplate.query(sql, memoryVectorEntryRowMapper(), id);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new VectorStoreException("Failed to get vector: " + id, e);
        }
    }


    /**
     * 1:1 复刻 Python keyword_search 关键词搜索
     * 基于 PostgreSQL 全文检索 lemmatized 文本
     * @param query 搜索关键词
     * @param top_k 返回结果数，默认5
     * @param filters 过滤条件
     * @return 相关性排序的搜索结果，失败返回null
     */
    @Override
    public List<SearchResult> keywordSearch(String query, Integer top_k, Map<String, Object> filters) {
        // 1. 设置默认值：top_k 默认为5（对齐Python）
        int limit = Objects.requireNonNullElse(top_k, 5);
        List<Object> filterParams = new ArrayList<>();
        List<String> filterConditions = new ArrayList<>();

        // 2. 构建过滤条件：payload->>? = ? （严格对齐Python）
        if (filters != null && !filters.isEmpty()) {
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                String key = entry.getKey();
                String value = String.valueOf(entry.getValue());
                filterConditions.add("payload->>? = ?");
                filterParams.add(key);
                filterParams.add(value);
            }
        }

        // 3. 拼接过滤子句：AND 连接（对齐Python filter_clause）
        String filterClause = "";
        if (!filterConditions.isEmpty()) {
            filterClause = "AND " + String.join(" AND ", filterConditions);
        }

        try {
            // 4. 原生SQL（完全复刻Python的全文检索语句）
            String sql = String.format("""
                    SELECT id,
                           ts_rank_cd(to_tsvector('simple', payload->>'text_lemmatized'), plainto_tsquery('simple', ?)) AS score,
                           payload
                    FROM %s
                    WHERE to_tsvector('simple', payload->>'text_lemmatized') @@ plainto_tsquery('simple', ?)
                    %s
                    ORDER BY score DESC
                    LIMIT ?
                    """, tableName, filterClause);

            // 5. 构建参数：严格对齐Python参数顺序 (query, query, *filter_params, top_k)
            List<Object> params = new ArrayList<>();
            params.add(query);  // 第一个query参数
            params.add(query);  // 第二个query参数
            params.addAll(filterParams); // 过滤参数
            params.add(limit);  // 分页参数

            // 6. 执行查询 + 结果映射
            return jdbcTemplate.query(sql, params.toArray(), this::mapKeywordResult);

        } catch (Exception e) {
            // 7. 异常捕获：debug日志 + 返回null（对齐Python）
            log.debug("Keyword search failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 结果映射器：复刻Python OutputData 构造逻辑
     * id=str(r[0]), score=float(r[1]), payload=r[2]
     */
    private SearchResult mapKeywordResult(ResultSet rs, int rowNum) throws SQLException {
        String id = rs.getString("id");
        Double score = rs.getDouble("score");
        Map<String, Object> payload = (Map<String, Object>) rs.getObject("payload");
        String hash = "";
        if (payload.containsKey("hash")) {
            hash = payload.get("hash").toString();
        }
        return new SearchResult(id, score, hash,payload);
    }
}
