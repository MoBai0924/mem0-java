package cn.hsine.mem0.core.entityextractor.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hsine.mem0.core.dto.Message;
import cn.hsine.mem0.core.entityextractor.EntityExtractorProvider;
import cn.hsine.mem0.core.entityextractor.EntityItem;
import cn.hsine.mem0.core.llm.LLMConfig;
import cn.hsine.mem0.core.llm.LLMProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 实体关系提取：
 * 1、nlp:最新的开源mem0用的还是nlp，具体模型可以参考代码
 * 2、大模型：Java改造后目前默认还是使用大模型类提取。
 */
@Slf4j
@Service
public class LlmEntityExtractor implements EntityExtractorProvider<EntityItem> {

    @Autowired
    private LLMProvider llmProvider;

    private static final LLMConfig llmConfig = new LLMConfig(0.0, 2000, null, null);


    // 抽取实体固定Prompt
    private static final String ENTITY_EXTRACT_PROMPT = """
            你是实体关系抽取专家，负责从对话中提取**持久化、有价值**的知识三元组 (subject, predicate, object)，用于构建 Agent 长期记忆图谱。
            规则：
            1. 只提取**明确陈述**的事实，不猜测、不引申、不幻觉。
            2. 主体/客体 = 人、物、地点、组织、概念、事件等实体；关系 = 实体间的稳定关联（如：是、住在、喜欢、工作于、拥有、认识、属于、位于）。
            3. 代词（我/你/他/它/这里）必须替换为**具体实体名**（用户→USER，对话中明确名字则用名字）。
            4. 忽略闲聊、临时情绪、无意义寒暄（如“你好”“哈哈”“不错”）。
            5. 同一实体统一命名，避免别名混乱（如“小明”全程用“小明”）。
            6、输出格式：严格 JSON 对象，每个元素示例 {"subject":"USER", "predicate":"姓名是", "object":"李明"}，无解释、无 markdown、无空行。
            7、若无有效信息，输出空数组 []。
            8、示例1：
            输入：我叫李明，住在上海，喜欢打篮球。
            输出：
            [
              {"subject":"USER", "predicate":"姓名是", "object":"李明"},
              {"subject":"李明", "predicate":"居住在", "object":"上海"},
              {"subject":"李明", "predicate":"喜欢", "object":"打篮球"}
            ]

            9、示例2：
            输入：今天天气真好。
            输出：
            []

            对话内容：
            %s
            """;

    /**
     * extract_entities_batch
     *
     * @param textList params
     * @return List<List<?>>
     */
    @Override
    public List<List<EntityItem>> extractEntitiesBatch(List<String> textList) {
        List<List<EntityItem>> resultList = new ArrayList<>();
        if (textList == null || textList.isEmpty()) {
            return resultList;
        }
        for (String text : textList) {
            resultList.add(singleTextExtract(text));
        }
        return resultList;
    }

    /**
     * 单文本抽取
     *
     * @param text param
     * @return List<?>
     */
    @Override
    public List<EntityItem> singleTextExtract(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }
        try {
            // 拼接提示词
            String prompt = String.format(ENTITY_EXTRACT_PROMPT, text);
            // 调用你的LLM服务
            String llmResp = llmProvider.generateResponse(Collections.singletonList(Message.system(prompt)), llmConfig);
            // 清洗代码块
            llmResp = removeCodeBlock(llmResp);
            // JSON转实体
            return parseEntityJson(llmResp);
        } catch (Exception e) {
            log.error("大模型实体抽取失败：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // 清洗```json代码块
    private String removeCodeBlock(String content) {
        if (content == null) {
            return "";
        }
        return content.replaceAll("^```[\\s\\S]*?\\n", "")
                .replaceAll("\\n```$", "")
                .trim();
    }

    // JSON解析为实体列表
    private List<EntityItem> parseEntityJson(String jsonStr) {
        try {
            //使用 TypeReference 明确指定泛型类型 List<EntityItem>
            return new ObjectMapper().readValue(jsonStr, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
