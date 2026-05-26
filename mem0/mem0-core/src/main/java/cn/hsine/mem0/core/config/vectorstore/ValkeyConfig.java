package cn.hsine.mem0.core.config.vectorstore;

/**
 * Configuration for Valkey vector store provider.
 * Ported from Python mem0/configs/vectorstores/valkey.py.
 *
 * @author MoBai

 */
public class ValkeyConfig {

    private String valkeyUrl = "redis://localhost:6379";
    private String timezone = "UTC";
    private String indexType = "FLAT";
    private int hnswM = 16;
    private int hnswEfConstruction = 200;
    private int hnswEfRuntime = 10;
    private boolean clusterMode = false;
    private int embeddingModelDims = 1536;

    public String getValkeyUrl() { return valkeyUrl; }
    public void setValkeyUrl(String valkeyUrl) { this.valkeyUrl = valkeyUrl; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getIndexType() { return indexType; }
    public void setIndexType(String indexType) { this.indexType = indexType; }
    public int getHnswM() { return hnswM; }
    public void setHnswM(int hnswM) { this.hnswM = hnswM; }
    public int getHnswEfConstruction() { return hnswEfConstruction; }
    public void setHnswEfConstruction(int hnswEfConstruction) { this.hnswEfConstruction = hnswEfConstruction; }
    public int getHnswEfRuntime() { return hnswEfRuntime; }
    public void setHnswEfRuntime(int hnswEfRuntime) { this.hnswEfRuntime = hnswEfRuntime; }
    public boolean isClusterMode() { return clusterMode; }
    public void setClusterMode(boolean clusterMode) { this.clusterMode = clusterMode; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
