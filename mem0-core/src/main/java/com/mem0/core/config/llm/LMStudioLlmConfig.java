package com.mem0.core.config.llm;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration for LM Studio LLM provider.
 * Ported from Python mem0/configs/llms/lmstudio.py.
 *
 * @author MoBai

 */
public class LMStudioLlmConfig extends BaseLlmConfig {

    private String lmstudioBaseUrl;
    private Map<String, Object> lmstudioResponseFormat = new HashMap<>();

    public LMStudioLlmConfig() {
        super();
        this.lmstudioBaseUrl = "http://localhost:1234/v1";
    }

    public String getLmstudioBaseUrl() { return lmstudioBaseUrl; }
    public void setLmstudioBaseUrl(String lmstudioBaseUrl) { this.lmstudioBaseUrl = lmstudioBaseUrl; }
    public Map<String, Object> getLmstudioResponseFormat() { return lmstudioResponseFormat; }
    public void setLmstudioResponseFormat(Map<String, Object> lmstudioResponseFormat) { this.lmstudioResponseFormat = lmstudioResponseFormat; }
}
