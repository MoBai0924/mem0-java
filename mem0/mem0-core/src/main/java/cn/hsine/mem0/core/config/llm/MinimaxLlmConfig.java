package cn.hsine.mem0.core.config.llm;

/**
 * Configuration for Minimax LLM provider.
 * Ported from Python mem0/configs/llms/minimax.py.
 *
 * @author MoBai

 */
public class MinimaxLlmConfig extends BaseLlmConfig {

    private String minimaxBaseUrl;

    public MinimaxLlmConfig() {
        super();
        this.minimaxBaseUrl = "https://api.minimax.chat/v1";
    }

    public String getMinimaxBaseUrl() { return minimaxBaseUrl; }
    public void setMinimaxBaseUrl(String minimaxBaseUrl) { this.minimaxBaseUrl = minimaxBaseUrl; }
}
