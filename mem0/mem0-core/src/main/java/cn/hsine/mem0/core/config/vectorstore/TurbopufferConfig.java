package cn.hsine.mem0.core.config.vectorstore;

/**
 * Configuration for Turbopuffer vector store provider.
 * Ported from Python mem0/configs/vectorstores/turbopuffer.py.
 *
 * @author MoBai

 */
public class TurbopufferConfig {

    private String apiKey;
    private String region;
    private String distanceMetric = "cosine";
    private int batchSize = 100;
    private String namespace;
    private int embeddingModelDims = 1536;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getDistanceMetric() { return distanceMetric; }
    public void setDistanceMetric(String distanceMetric) { this.distanceMetric = distanceMetric; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
