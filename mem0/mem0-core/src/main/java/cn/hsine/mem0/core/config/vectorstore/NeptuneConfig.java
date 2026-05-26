package cn.hsine.mem0.core.config.vectorstore;

/**
 * Configuration for Neptune vector store provider.
 * Ported from Python mem0/configs/vectorstores/neptune.py.
 *
 * @author MoBai

 */
public class NeptuneConfig {

    private String endpoint;
    private int embeddingModelDims = 1536;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
