package cn.hsine.mem0.core.config.vectorstore;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration for Qdrant vector store provider.
 * Ported from Python mem0/configs/vectorstores/qdrant.py.
 *
 * @author MoBai

 */
public class QdrantConfig {

    private String host = "localhost";
    private int port = 6333;
    private String path;
    private String url;
    private String apiKey;
    private boolean onDisk = false;
    private String collectionName = "memories";
    private int embeddingModelDims = 1536;
    private Map<String, Object> hnsw = new HashMap<>();

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public boolean isOnDisk() { return onDisk; }
    public void setOnDisk(boolean onDisk) { this.onDisk = onDisk; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
    public Map<String, Object> getHnsw() { return hnsw; }
    public void setHnsw(Map<String, Object> hnsw) { this.hnsw = hnsw; }
}
