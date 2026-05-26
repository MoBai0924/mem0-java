package cn.hsine.mem0.core.llm.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import cn.hsine.mem0.core.config.llm.DeepSeekLlmConfig;
import cn.hsine.mem0.core.dto.Message;
import cn.hsine.mem0.core.exception.ConfigurationException;
import cn.hsine.mem0.core.exception.LLMException;
import cn.hsine.mem0.core.llm.LLMConfig;
import cn.hsine.mem0.core.llm.LLMProvider;
import cn.hsine.mem0.core.llm.LLMProviderConfig;
import cn.hsine.mem0.core.utils.JsonUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

/**
 * DeepSeek LLM provider implementation.
 * Uses OpenAI-compatible API format.
 */
public class DeepSeekLLM implements LLMProvider {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekLLM.class);

    private final OkHttpClient httpClient;

    private final DeepSeekLlmConfig deepSeekLlmConfig;


    public DeepSeekLLM(LLMProviderConfig config) {
        var apiKey = config.get("api-key") != null ? config.get("api-key").toString()
                : System.getenv("DEEPSEEK_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new ConfigurationException("DeepSeek API key is required");
        }

        var baseUrl = config.get("base-url") != null ? String.valueOf(config.get("base-url")) :
                (System.getenv("DEEPSEEK_API_BASE") != null ? System.getenv("DEEPSEEK_API_BASE")
                 : "https://api.deepseek.com");

        this.deepSeekLlmConfig = new DeepSeekLlmConfig() {{
            setModel(config.get("model", "deepseek-chat"));
            setApiKey(apiKey);
            setDeepseekBaseUrl(baseUrl);
            setTemperature(config.get("temperature", super.getTemperature()));
            setMaxTokens(config.get("max-tokens", super.getMaxTokens()));
            setTopP(config.get("topP", super.getTopP()));
            setTopK(config.get("topK", super.getTopK()));
            setEnableVision(config.get("enableVision", super.isEnableVision()));
            setVisionDetails(config.get("visionDetails", super.getVisionDetails()));
        }};

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String generateResponse(List<Message> messages, LLMConfig config) {
        try {
            String body = buildRequestBody(messages, config);
            log.debug("DeepSeek Request: {}", body);
            Request request = new Request.Builder()
                    .url("%s/chat/completions".formatted(deepSeekLlmConfig.getDeepseekBaseUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer %s".formatted(deepSeekLlmConfig.getApiKey()))
                    .post(RequestBody.create(body, MediaType.parse("application/json")))
                    .build();
            try (Response response = httpClient.newCall(request).execute();) {
                if (!response.isSuccessful()) {
                    throw new LLMException("DeepSeek API error: %d".formatted(response.code()));
                }
                JsonNode root = JsonUtil.readTree(response.body().string());
                return root.at("/choices/0/message/content").asText();
            }
        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            throw new LLMException("DeepSeek LLM failed", e);
        }
    }

    private String buildRequestBody(List<Message> messages, LLMConfig config) throws Exception {
        var msgList = new ArrayList<>();
        for (Message m : messages) {
            msgList.add(Map.of("role", m.role(), "content", m.content()));
        }

        Double temperature;
        Integer maxTokens;
        Double topP;
        if (config != null) {
            temperature = config.temperature();
            maxTokens = config.maxTokens();
            topP = config.topP();
        } else {
            temperature = deepSeekLlmConfig.getTemperature();
            maxTokens = deepSeekLlmConfig.getMaxTokens();
            topP = deepSeekLlmConfig.getTopP();
        }

        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("model", deepSeekLlmConfig.getModel());
        body.put("messages", msgList);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("top_p", topP);
        /*
         * OpenAI 格式 / 通义千问 / 豆包 /  Ollama 的模型需要的格式：
         *
         * "response_format": {"type": "json_object"}
         */
        body.put("response_format", Map.of("type", "json_object"));
        return JsonUtil.write(body);
    }

    @Override
    public String getName() {
        return "deepseek";
    }

    @Override
    public String generateResponse(List<Message> messages) {
        Map<String,Object> response = generateResponse(messages,null,null,null,null);
        return response.get("content").toString();
    }

    /**
     * generate_response
     * 调用 DeepSeek API 生成响应
     *
     * @param messages       消息列表
     * @param responseFormat 响应格式
     * @param tools          工具列表
     * @param toolChoice     工具选择策略
     * @param kwargs         额外参数
     * @return 处理后的响应（文本/结构化工具调用）
     */
    @Override
    public Map<String, Object> generateResponse(List<Message> messages,
                                                Object responseFormat,
                                                List<Map<String, Object>> tools,
                                                String toolChoice,
                                                Map<String, Object> kwargs) {
        // 1. 获取支持的参数（复用父类方法）
        Map<String, Object> supportedParams = getSupportedParams(kwargs, deepSeekLlmConfig);

        // 2. 组装核心参数
        supportedParams.put("model", deepSeekLlmConfig.getModel());

        var msgList = new ArrayList<>();
        for (Message m : messages) {
            msgList.add(Map.of("role", m.role(), "content", m.content()));
        }
        supportedParams.put("messages", msgList);
        supportedParams.put("temperature", deepSeekLlmConfig.getTemperature());
        supportedParams.put("max_tokens", deepSeekLlmConfig.getMaxTokens());
        supportedParams.put("top_p", deepSeekLlmConfig.getTopK());
        /*
         * OpenAI 格式 / 通义千问 / 豆包 /  Ollama 的模型需要的格式：
         *
         * "response_format": {"type": "json_object"}
         */
        supportedParams.put("response_format", Objects.requireNonNullElseGet(responseFormat,
                () -> Map.of("type", "json_object")));

        // 3. 添加可选参数
        if (tools != null && !tools.isEmpty()) {
            supportedParams.put("tools", tools);
            supportedParams.put("tool_choice", toolChoice);
        }

        // 4. 构建请求 + 调用 API
        try {
            String body = JsonUtil.write(supportedParams);

            Request request = new Request.Builder()
                    .url("%s/chat/completions".formatted(deepSeekLlmConfig.getDeepseekBaseUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer %s".formatted(deepSeekLlmConfig.getApiKey()))
                    .post(RequestBody.create(body, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute();) {
                // 5. 解析响应并返回
                return parseResponse(response, tools);
            }
        }  catch (Exception e) {
            throw new LLMException("DeepSeek LLM failed", e);
        }
    }

    /**
     * 解析 API 响应，区分工具调用和普通文本
     *
     * @param response 原始 API 响应
     * @param tools    工具列表
     * @return String（纯文本）或 Map（包含工具调用）
     */
    private Map<String, Object> parseResponse(Response response, List<Map<String, Object>> tools) throws IOException {
        if (!response.isSuccessful()) {
            throw new LLMException("DeepSeek API error: %d".formatted(response.code()));
        }

        Map<String, Object> processedResponse = new HashMap<>();
        JsonNode root = JsonUtil.readTree(response.body().string());
        String content = root.at("/choices/0/message/content").asText();
        processedResponse.put("content", content);
        // 无工具：直接返回文本内容
        if (tools == null || tools.isEmpty()) {
            return processedResponse;
        }

        // 有工具：构建结构化响应
        List<Map<String, Object>> toolCalls = new ArrayList<>();

        // 解析工具调用
        JsonNode toolsNode = root.at("/choices/0/message/tool_calls");
        if (!toolsNode.isEmpty()) {
            for (var toolCall : toolsNode) {
                Map<String, Object> callInfo = new HashMap<>();
                // 工具名称
                callInfo.put("name", toolCall.get("name"));
                // 解析参数（提取JSON → 转Map）
                try {
                    String jsonStr = JsonUtil.write(toolCall.get("arguments"));
                    Map<String, Object> arguments = JsonUtil.read(jsonStr, new TypeReference<>() {
                    });
                    callInfo.put("arguments", arguments);
                } catch (Exception e) {
                    callInfo.put("arguments", new HashMap<>());
                }
                toolCalls.add(callInfo);
            }
        }
        processedResponse.put("tool_calls", toolCalls);
        return processedResponse;
    }
}
