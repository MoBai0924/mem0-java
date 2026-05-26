package com.mem0.core.config.llm;

/**
 * Configuration for vLLM LLM provider.
 * Ported from Python mem0/configs/llms/vllm.py.
 *
 * @author MoBai

 */
public class VllmLlmConfig extends BaseLlmConfig {

    private String vllmBaseUrl;

    public VllmLlmConfig() {
        super();
        this.vllmBaseUrl = "http://localhost:8000/v1";
    }

    public String getVllmBaseUrl() { return vllmBaseUrl; }
    public void setVllmBaseUrl(String vllmBaseUrl) { this.vllmBaseUrl = vllmBaseUrl; }
}
