package com.mem0.core.config.reranker;

/**
 * Configuration for Cohere reranker provider.
 * Ported from Python mem0/configs/rerankers/cohere.py.
 *
 * @author MoBai

 */
public class CohereRerankerConfig extends BaseRerankerConfig {

    private boolean returnDocuments;
    private int maxChunksPerDoc;

    public CohereRerankerConfig() {
        super();
        this.setModel("rerank-english-v3.0");
        this.returnDocuments = false;
        this.maxChunksPerDoc = 10;
    }

    public boolean isReturnDocuments() { return returnDocuments; }
    public void setReturnDocuments(boolean returnDocuments) { this.returnDocuments = returnDocuments; }
    public int getMaxChunksPerDoc() { return maxChunksPerDoc; }
    public void setMaxChunksPerDoc(int maxChunksPerDoc) { this.maxChunksPerDoc = maxChunksPerDoc; }
}
