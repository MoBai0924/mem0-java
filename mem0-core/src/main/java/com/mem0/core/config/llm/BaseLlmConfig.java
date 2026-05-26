package com.mem0.core.config.llm;

/**
 * Base configuration for all LLM providers.
 * Ported from Python mem0/configs/llms/base.py.
 *
 * @author MoBai

 */
public abstract class BaseLlmConfig {

    private String model;
    private String apiKey;
    private int maxTokens;
    private double temperature;
    private double topP;
    private int topK;
    private boolean enableVision;
    private String visionDetails;
    private String reasoningEffort;
    private String httpClientProxies;

    public BaseLlmConfig() {
        this.temperature = 0.0;
        this.maxTokens = 2000;
        this.topP = 1.0;
        this.topK = 1;
        this.enableVision = false;
        this.visionDetails = "auto";
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public double getTopP() { return topP; }
    public void setTopP(double topP) { this.topP = topP; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public boolean isEnableVision() { return enableVision; }
    public void setEnableVision(boolean enableVision) { this.enableVision = enableVision; }
    public String getVisionDetails() { return visionDetails; }
    public void setVisionDetails(String visionDetails) { this.visionDetails = visionDetails; }
    public String getReasoningEffort() { return reasoningEffort; }
    public void setReasoningEffort(String reasoningEffort) { this.reasoningEffort = reasoningEffort; }
    public String getHttpClientProxies() { return httpClientProxies; }
    public void setHttpClientProxies(String httpClientProxies) { this.httpClientProxies = httpClientProxies; }
}
