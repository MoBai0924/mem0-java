package cn.hsine.mem0.core.config.vectorstore;

/**
 * Configuration for Upstash Vector vector store provider.
 * Ported from Python mem0/configs/vectorstores/upstash_vector.py.
 *
 * @author MoBai

 */
public class UpstashVectorConfig {

    private String url;
    private String token;
    private boolean enableEmbeddings = false;
    private int embeddingModelDims = 1536;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public boolean isEnableEmbeddings() { return enableEmbeddings; }
    public void setEnableEmbeddings(boolean enableEmbeddings) { this.enableEmbeddings = enableEmbeddings; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
