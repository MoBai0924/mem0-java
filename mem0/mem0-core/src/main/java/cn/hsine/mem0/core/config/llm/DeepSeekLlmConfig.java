package cn.hsine.mem0.core.config.llm;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Configuration for DeepSeek LLM provider.
 * Ported from Python mem0/configs/llms/deepseek.py.
 *
 * @author MoBai

 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DeepSeekLlmConfig extends BaseLlmConfig {

    private String deepseekBaseUrl;

    public DeepSeekLlmConfig() {
        super();
        this.deepseekBaseUrl = "https://api.deepseek.com/v1";
    }
}
