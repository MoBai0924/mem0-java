package com.mem0.core.config.reranker;

/**
 * Base configuration for all reranker providers.
 * Ported from Python mem0/configs/rerankers/base.py.
 *
 * @author MoBai

 */
public class BaseRerankerConfig {

    private String provider;
    private String model;
    private String apiKey;
    private Integer topK;

    public BaseRerankerConfig() {
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
}
