package com.mem0.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON 工具类：基于 Jackson ObjectMapper 封装常用的 JSON 序列化、反序列化、转换和校验操作
 */
public final class JsonUtil {

    // 正则表达式：匹配 ```json...``` 代码块，兼容无json标识、换行（对应Python re.DOTALL）
    private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile(
            "```(?:json)?\\s*(.*?)\\s*```", Pattern.DOTALL);

    private static final ObjectMapper OBJECT_MAPPER
            = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // --- 序列化 ---

    /**
     * 将对象序列化为 JSON 字符串
     */
    public static String write(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * 将对象序列化为格式化的（带缩进的）JSON 字符串
     */
    public static String writePretty(Object obj) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to pretty JSON", e);
        }
    }

    // --- 反序列化 ---

    /**
     * 将 JSON 字符串反序列化为指定类型的对象
     */
    public static <T> T read(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            throw new RuntimeException("JSON string must not be null or empty");
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to %s".formatted(clazz.getSimpleName()), e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定泛型类型的对象（通过 TypeReference 保留完整泛型信息）
     */
    public static <T> T read(String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) {
            throw new RuntimeException("JSON string must not be null or empty");
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON with TypeReference", e);
        }
    }

    /**
     * 将 JSON 字符串解析为 JsonNode 树模型，便于灵活遍历
     */
    public static JsonNode readTree(String json) {
        if (json == null || json.isEmpty()) {
            throw new RuntimeException("JSON string must not be null or empty");
        }
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON string to JsonNode", e);
        }
    }

    // --- 转换 ---

    /**
     * 将对象转换为 Map<String, Object> 表示
     */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.convertValue(obj, new TypeReference<>() {
            });
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to convert object to Map", e);
        }
    }

    /**
     * 将 Map<String, Object> 转换为指定类型的对象
     */
    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        if (map == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(map, clazz);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to convert Map to %s".formatted(clazz.getSimpleName()), e);
        }
    }

    // --- 校验 ---

    /**
     * 校验字符串是否为合法的 JSON
     */
    public static boolean isValid(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * extract_json 工具方法 从文本中提取JSON内容，处理代码块、自动匹配{}
     * 提取字符串中的JSON内容
     *
     * @param text 原始文本
     * @return 提取后的JSON字符串
     */
    public static String extractJson(String text) {
        // 1. 空值处理 + 去除首尾空白（等价Python text.strip()）
        if (text == null || text.isBlank()) {
            return text;
        }

        String trimmedText = text.trim();

        // 2. 匹配 ```json``` 代码块
        Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(trimmedText);
        if (matcher.find()) {
            // 匹配到代码块，返回中间内容
            return matcher.group(1).trim();
        }

        // 3. 未匹配到代码块：查找第一个 { 和最后一个 }
        int startIdx = trimmedText.indexOf("{");
        int endIdx = trimmedText.lastIndexOf("}");

        // 校验索引合法
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return trimmedText.substring(startIdx, endIdx + 1);
        }

        // 4. 都未找到，返回原文本
        return trimmedText;
    }
}
