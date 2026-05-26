package com.mem0.core.config.reranker;

/**
 * Configuration for HuggingFace reranker provider.
 * Ported from Python mem0/configs/rerankers/huggingface.py.
 *
 * @author MoBai

 */
public class HuggingFaceRerankerConfig extends BaseRerankerConfig {

    private String device;
    private int batchSize;
    private int maxLength;
    private boolean normalize;

    public HuggingFaceRerankerConfig() {
        super();
        this.setModel("BAAI/bge-reranker-base");
        this.batchSize = 32;
        this.maxLength = 512;
        this.normalize = true;
    }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getMaxLength() { return maxLength; }
    public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
    public boolean isNormalize() { return normalize; }
    public void setNormalize(boolean normalize) { this.normalize = normalize; }
}
