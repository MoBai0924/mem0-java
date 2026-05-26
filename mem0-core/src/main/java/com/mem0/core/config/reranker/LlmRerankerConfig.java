package com.mem0.core.config.reranker;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration for LLM-based reranker provider.
 * Ported from Python mem0/configs/rerankers/llm.py.
 *
 * @author MoBai

 */
public class LlmRerankerConfig extends BaseRerankerConfig {

    private String provider;
    private double temperature;
    private int maxTokens;
    private String scoringPrompt;
    private Map<String, Object> llm = new HashMap<>();

    public LlmRerankerConfig() {
        super();
        this.setModel("gpt-4o-mini");
        this.provider = "openai";
        this.temperature = 0.0;
        this.maxTokens = 100;
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public String getScoringPrompt() { return scoringPrompt; }
    public void setScoringPrompt(String scoringPrompt) { this.scoringPrompt = scoringPrompt; }
    public Map<String, Object> getLlm() { return llm; }
    public void setLlm(Map<String, Object> llm) { this.llm = llm; }
}
