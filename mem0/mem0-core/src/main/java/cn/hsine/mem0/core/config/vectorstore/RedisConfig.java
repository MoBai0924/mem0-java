package cn.hsine.mem0.core.config.vectorstore;

/**
 * Configuration for Redis vector store provider.
 * Ported from Python mem0/configs/vectorstores/redis.py.
 *
 * @author MoBai

 */
public class RedisConfig {

    private String redisUrl = "redis://localhost:6379";
    private String collectionName = "memories";
    private int embeddingModelDims = 1536;

    public String getRedisUrl() { return redisUrl; }
    public void setRedisUrl(String redisUrl) { this.redisUrl = redisUrl; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
