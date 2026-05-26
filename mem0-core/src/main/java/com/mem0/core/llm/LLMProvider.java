package com.mem0.core.llm;

import com.mem0.core.config.llm.BaseLlmConfig;
import com.mem0.core.dto.Message;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for LLM providers that generate text responses.
 *
 * @author MoBai
 */
public interface LLMProvider {

    /**
     * Generates a response from the given messages,
     * The LLMConfig parameter is passed by the caller. This function will be retained.
     *
     * @param messages the conversation messages
     * @param config   the LLM configuration for this request
     * @return the generated response text
     */
    String generateResponse(List<Message> messages, LLMConfig config);

    /**
     * Returns the name of this provider.
     *
     * @return the provider name
     */
    String getName();


    default Map<String, Object> generateResponse(List<Message> messages,
                                                 Object responseFormat,
                                                 List<Map<String, Object>> tools,
                                                 String toolChoice,
                                                 Map<String, Object> kwargs) {
        return null;
    }

    default String generateResponse(List<Message> messages) {
        return null;
    }

    /**
     * _get_supported_params
     * 获取当前模型支持的参数（过滤推理模型不支持的参数）
     *
     * @param kwargs 额外参数
     * @return 过滤后的参数Map
     */
    default Map<String, Object> getSupportedParams(Map<String, Object> kwargs, BaseLlmConfig config) {
        // 1. 获取模型名称（等价 getattr(self.config, 'model', '')）
        String model = (config.getModel() == null) ? "" : config.getModel();

        // 2. 判断是否为推理模型
        if (isReasoningModel(model) && !CollectionUtils.isEmpty(kwargs)) {
            Map<String, Object> supportedParams = new HashMap<>();

            // 仅保留支持的4个核心参数
            if (kwargs.containsKey("messages")) {
                supportedParams.put("messages", kwargs.get("messages"));
            }
            if (kwargs.containsKey("response_format")) {
                supportedParams.put("response_format", kwargs.get("response_format"));
            }
            if (kwargs.containsKey("tools")) {
                supportedParams.put("tools", kwargs.get("tools"));
            }
            if (kwargs.containsKey("tool_choice")) {
                supportedParams.put("tool_choice", kwargs.get("tool_choice"));
            }

            // 3. 添加 reasoning_effort（等价 getattr + 非空判断）
            String reasoningEffort = config.getReasoningEffort();
            if (reasoningEffort != null && !reasoningEffort.isEmpty()) {
                supportedParams.put("reasoning_effort", reasoningEffort);
            }

            return supportedParams;
        } else {
            // 普通模型：返回通用参数
            return getCommonParams(kwargs, config);
        }
    }

    /**
     * 复刻 Python _get_common_params 方法
     * 获取所有模型提供商通用的基础参数
     */
    private Map<String, Object> getCommonParams(Map<String, Object> kwargs, BaseLlmConfig config) {
        // 1. 初始化基础通用参数（严格对应Python键名：蛇形命名）
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", config.getTemperature());
        params.put("max_tokens", config.getMaxTokens());
        params.put("top_p", config.getTopP());

        // 2. 合并额外参数（等价 Python params.update(kwargs)）
        if (kwargs != null && !kwargs.isEmpty()) {
            params.putAll(kwargs);
        }

        // 3. 返回最终参数
        return params;
    }

    /**
     * 复刻 Python _is_reasoning_model 方法
     * 判断是否为推理模型 / GPT-5 系列模型
     */
    private boolean isReasoningModel(String model) {
        // 1. 定义推理模型集合（完全对齐Python）
        final Set<String> reasoningModels = Set.of("o1", "o1-preview", "o3-mini", "o3", "gpt-5", "gpt-5o",
                "gpt-5o-mini", "gpt-5o-micro");

        // 2. 空值安全处理（model为null直接返回false）
        if (model == null || model.isBlank()) {
            return false;
        }

        // 3. 模型名转小写
        String modelLower = model.toLowerCase();

        // 4. 剥离提供商前缀（等价 Python model_lower.rsplit("/", 1)[-1]）
        // 例如："openai/o3-mini" → "o3-mini"
        int lastSlashIndex = modelLower.lastIndexOf('/');
        String baseModel = (lastSlashIndex == -1)
                ? modelLower
                : modelLower.substring(lastSlashIndex + 1);

        // 5. 精确匹配推理模型集合
        if (reasoningModels.contains(baseModel)) {
            return true;
        }

        // 6. 前缀匹配：o1- / o1. / o3- / o3. 开头（排除gpt-5.x系列）
        return baseModel.startsWith("o1-") || baseModel.startsWith("o1.")
                || baseModel.startsWith("o3-") || baseModel.startsWith("o3.");

        // 7. 非推理模型-false
    }
}
