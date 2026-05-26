package cn.hsine.mem0.core.config.vectorstore;

/**
 * Configuration for Milvus vector store provider.
 * Ported from Python mem0/configs/vectorstores/milvus.py.
 *
 * @author MoBai

 */
public class MilvusConfig {

    private String url = "http://localhost:19530";
    private String token;
    private String metricType = "COSINE";
    private String dbName = "default";
    private String collectionName = "memories";
    private int embeddingModelDims = 1536;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }
    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
