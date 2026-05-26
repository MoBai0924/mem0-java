package com.mem0.core.config.vectorstore;

/**
 * Configuration for FAISS vector store provider.
 * Ported from Python mem0/configs/vectorstores/faiss.py.
 *
 * @author MoBai

 */
public class FAISSConfig {

    private String path;
    private String distanceStrategy = "METRIC_L2";
    private boolean normalizeL2 = false;
    private int embeddingModelDims = 1536;
    private String collectionName = "memories";

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getDistanceStrategy() { return distanceStrategy; }
    public void setDistanceStrategy(String distanceStrategy) { this.distanceStrategy = distanceStrategy; }
    public boolean isNormalizeL2() { return normalizeL2; }
    public void setNormalizeL2(boolean normalizeL2) { this.normalizeL2 = normalizeL2; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
}
