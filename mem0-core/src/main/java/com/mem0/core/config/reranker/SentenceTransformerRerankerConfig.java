package com.mem0.core.config.reranker;

/**
 * Configuration for SentenceTransformer reranker provider.
 * Ported from Python mem0/configs/rerankers/sentence_transformer.py.
 *
 * @author MoBai

 */
public class SentenceTransformerRerankerConfig extends BaseRerankerConfig {

    private String device;
    private int batchSize;
    private boolean showProgressBar;

    public SentenceTransformerRerankerConfig() {
        super();
        this.setModel("cross-encoder/ms-marco-MiniLM-L-6-v2");
        this.batchSize = 32;
        this.showProgressBar = false;
    }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public boolean isShowProgressBar() { return showProgressBar; }
    public void setShowProgressBar(boolean showProgressBar) { this.showProgressBar = showProgressBar; }
}
