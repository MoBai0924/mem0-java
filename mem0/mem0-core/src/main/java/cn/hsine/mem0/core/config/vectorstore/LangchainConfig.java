package cn.hsine.mem0.core.config.vectorstore;

/**
 * Configuration for Langchain vector store provider.
 * Ported from Python mem0/configs/vectorstores/langchain.py.
 *
 * @author MoBai

 */
public class LangchainConfig {

    private Object client;
    private String collectionName = "memories";
    private int embeddingModelDims = 1536;

    public Object getClient() { return client; }
    public void setClient(Object client) { this.client = client; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
