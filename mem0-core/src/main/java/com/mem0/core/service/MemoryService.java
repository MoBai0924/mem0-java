package com.mem0.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mem0.core.config.MemoryConfig;
import com.mem0.core.config.MemoryType;
import com.mem0.core.config.Prompts;
import com.mem0.core.domain.model.Memory;
import com.mem0.core.domain.model.MemoryEvent;
import com.mem0.core.domain.model.RoleEnum;
import com.mem0.core.dto.Message;
import com.mem0.core.dto.mid.FilterMetadataResult;
import com.mem0.core.dto.request.AddMemoryRequest;
import com.mem0.core.dto.request.SearchFilters;
import com.mem0.core.dto.request.UpdateMemoryRequest;
import com.mem0.core.dto.response.MemoryHistoryResponse;
import com.mem0.core.dto.response.MemoryResponse;
import com.mem0.core.dto.response.SearchResult;
import com.mem0.core.embedding.EmbeddingProvider;
import com.mem0.core.entityextractor.EntityItem;
import com.mem0.core.entityextractor.impl.LlmEntityExtractor;
import com.mem0.core.entitystore.EntityStore;
import com.mem0.core.exception.Mem0ValidationError;
import com.mem0.core.llm.LLMConfig;
import com.mem0.core.llm.LLMProvider;
import com.mem0.core.llm.LLMProviderConfig;
import com.mem0.core.message.MessageEntity;
import com.mem0.core.message.MessageRecord;
import com.mem0.core.reranker.Reranker;
import com.mem0.core.score.Bm25Params;
import com.mem0.core.utils.CopyUtil;
import com.mem0.core.utils.JsonUtil;
import com.mem0.core.utils.MetadataFilter;
import com.mem0.core.vectorstore.MemoryVectorEntry;
import com.mem0.core.vectorstore.VectorEntry;
import com.mem0.core.vectorstore.VectorStore;
import jakarta.annotation.Nullable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mem0.core.config.Prompts.*;
import static com.mem0.core.utils.HashUtil.md5Hex;
import static com.mem0.core.utils.Lemmatizer.lemmatizeForBm25;
import static com.mem0.core.utils.Scoring.*;

/**
 * Main service for memory operations.
 * Implements V3 phased batch pipeline for add() and hybrid retrieval for search(),
 * ported from Python mem0/memory/main.py.
 *
 * @author MoBai
 */
@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    // 不可变集合（和 frozenset 行为完全一致：不能增删改）
    private static final Set<String> ENTITY_PARAMS = Set.of(
            "user_id",
            "agent_id",
            "run_id"
    );

    // == 常量定义（和Python完全一致） ==
    private static final List<String> PROMOTED_PAYLOAD_KEYS = List.of(
            "user_id",
            "agent_id",
            "run_id",
            "actor_id",
            "role"
    );

    private static final Set<String> CORE_AND_PROMOTED_KEYS;

    static {
        CORE_AND_PROMOTED_KEYS = new HashSet<>();
        // 核心字段
        CORE_AND_PROMOTED_KEYS.addAll(List.of(
                "data", "hash", "created_at", "updated_at",
                "id", "text_lemmatized", "attributed_to"
        ));
        // 合并提升字段
        CORE_AND_PROMOTED_KEYS.addAll(PROMOTED_PAYLOAD_KEYS);
    }

    private final SQLiteManager sqLiteManager;
    private final VectorStore vectorStore;
    private final EmbeddingProvider embeddingProvider;
    private final LLMProvider llmProvider;
    private final EntityStore entityStore;
    private final Reranker reranker;
    private final LLMProviderConfig llmProviderConfig;
    private final MemoryConfig memoryConfig;
    private final LlmEntityExtractor llmEntityExtractor;
    private final TelemetryService telemetryService;

    @Autowired
    public MemoryService(SQLiteManager sqLiteManager,
                         VectorStore vectorStore,
                         EmbeddingProvider embeddingProvider,
                         LLMProvider llmProvider,
                         LLMProviderConfig llmProviderConfig,
                         MemoryConfig memoryConfig,
                         LlmEntityExtractor llmEntityExtractor,
                         EntityStore entityStore,
                         Reranker reranker,
                         TelemetryService telemetryService) {
        this.sqLiteManager = sqLiteManager;
        this.vectorStore = vectorStore;
        this.embeddingProvider = embeddingProvider;
        this.llmProvider = llmProvider;
        this.entityStore = entityStore;
        this.reranker = reranker;
        this.llmProviderConfig = llmProviderConfig;
        this.memoryConfig = memoryConfig;
        this.llmEntityExtractor = llmEntityExtractor;
        this.telemetryService = telemetryService;
    }

    /**
     * Adds memories using V3 phased batch pipeline.
     */
    @Transactional
    public List<Map<String, Object>> add(AddMemoryRequest request) throws JsonProcessingException {
        List<Message> msgList = request.messages();
        String userId = request.userId();
        String agentId = request.agentId();
        String runId = request.runId();
        Map<String, Object> metadata = request.metadata();
        Map<String, Object> filters = new HashMap<>();
        Boolean infer = request.infer() == null || request.infer();
        String memoryType = request.memoryType();
        String prompt = request.prompt();

        log.debug("Adding memories (V3 pipeline) for user={}, agent={}, run={}", userId, agentId, runId);
        FilterMetadataResult filterMetadataResult = buildFiltersAndMetadata(userId, agentId, runId, null, metadata, filters);
        Map<String, Object> processedMetadata = filterMetadataResult.processedMetadata();
        Map<String, Object> effectiveFilters = filterMetadataResult.effectiveFilters();

        if (memoryType != null && !MemoryType.PROCEDURAL.getValue().equals(memoryType)) {
            Map<String, Object> details = new HashMap<>();
            details.put("provided_type", memoryType);
            details.put("valid_type", MemoryType.PROCEDURAL.getValue());
            throw new Mem0ValidationError(
                    "Invalid 'memory_type'. Please pass %s to create procedural memories.".formatted(MemoryType.PROCEDURAL.getValue()),
                    "VALIDATION_002",
                    details,
                    "Use '%s' to create procedural memories.".formatted(MemoryType.PROCEDURAL.getValue())
            );
        }

        if (agentId != null && MemoryType.PROCEDURAL.getValue().equals(memoryType)) {
            Map<String, Object> proceduralMemory = createProceduralMemory(msgList, processedMetadata, prompt);
            return Collections.singletonList(proceduralMemory);
        }

        boolean enableVision = llmProviderConfig.get("enable_vision", false);

        Map<String, Object> visionDetails = llmProviderConfig.get("vision_details", null);
        if (enableVision) {
            msgList = parseVisionMessages(msgList, visionDetails);
        } else {
            msgList = parseVisionMessages(msgList);
        }
        return addToVectorStore(msgList, processedMetadata, effectiveFilters, infer, prompt);
    }

    /**
     * 对应 Python: _add_to_vector_store
     *
     * @param messages 消息列表
     * @param metadata 元数据
     * @param filters  过滤要素
     * @param infer    直接存原文：= false 直接存原文
     * @param prompt   提示词
     * @return List
     */
    private List<Map<String, Object>> addToVectorStore(
            List<Message> messages,
            Map<String, Object> metadata,
            Map<String, Object> filters,
            Boolean infer,
            String prompt) {

        //infer = false 直接存原文
        if (!infer) {
            List<Map<String, Object>> returnedMemories = new ArrayList<>();

            for (Message messageDict : messages) {
                String role = messageDict.role();
                String content = messageDict.content();
                String actorName = messageDict.name();
                if (StringUtils.isAnyEmpty(role, content)) {
                    log.warn("Skipping invalid message format: {}", messageDict);
                    continue;
                }

                if (RoleEnum.system.name().equals(role)) {
                    continue;
                }

                Map<String, Object> perMsgMeta = new HashMap<>(metadata);
                perMsgMeta.put("role", role);
                if (actorName != null) {
                    perMsgMeta.put("actor_id", actorName);
                }

                Double[] embeddings = embeddingProvider.embed(content);

                Map<String, Double[]> existingEmbeddings = new HashMap<>();
                existingEmbeddings.put(content, embeddings);
                String memId = createMemory(content, existingEmbeddings, perMsgMeta);

                Map<String, Object> item = new HashMap<>();
                item.put("id", memId);
                item.put("memory", content);
                item.put("event", MemoryEvent.ADD.name());
                item.put("actor_id", actorName);
                item.put("role", role);
                returnedMemories.add(item);
            }
            return returnedMemories;
        }

        //PHASED BATCH PIPELINE (infer = true)
        // Phase 0: Context gathering
        String sessionScope = buildSessionScope(filters);
        List<MessageEntity> lastMessageEntities = sqLiteManager.getLastMessages(sessionScope, 10);
        String parsedMessages = parseMessages(messages);

        // Phase 1: Existing memory retrieval
        Map<String, Object> searchFilters = new HashMap<>();
        if (filters.containsKey("user_id") && filters.get("user_id") != null) {
            searchFilters.put("user_id", filters.get("user_id"));
        }
        if (filters.containsKey("agent_id") && filters.get("agent_id") != null) {
            searchFilters.put("agent_id", filters.get("agent_id"));
        }
        if (filters.containsKey("run_id") && filters.get("run_id") != null) {
            searchFilters.put("run_id", filters.get("run_id"));
        }

        Double[] queryEmbedding = embeddingProvider.embed(parsedMessages);
        List<com.mem0.core.vectorstore.SearchResult> existingResults = vectorStore.search(
                queryEmbedding,
                10,
                searchFilters
        );

        Map<String, String> uuidMapping = new HashMap<>();
        List<Map<String, String>> existingMemories = new ArrayList<>();
        for (int i = 0; i < existingResults.size(); i++) {
            com.mem0.core.vectorstore.SearchResult mem = existingResults.get(i);
            uuidMapping.put(String.valueOf(i), mem.id());
            Map<String, String> item = new HashMap<>();
            item.put("id", String.valueOf(i));
            item.put("text", mem.payload() != null ? (String) mem.payload().get("data") : "");
            existingMemories.add(item);
        }

        // Phase 2: LLM extraction
        boolean isAgentScoped = filters.get("agent_id") != null && filters.get("user_id") == null;
        String systemPrompt = ADDITIVE_EXTRACTION_PROMPT;
        if (isAgentScoped) {
            systemPrompt += AGENT_CONTEXT_SUFFIX;
        }

        String customInstr = prompt != null ? prompt : memoryConfig.getCustomInstructions();
        String userPrompt = Prompts.generateAdditiveExtractionPrompt(
                null,
                null,
                existingMemories,
                parsedMessages,
                lastMessageEntities,
                null,
                null,
                customInstr,
                false);

        List<Message> llmMessages = new ArrayList<>();
        llmMessages.add(Message.system(systemPrompt));
        llmMessages.add(Message.user(userPrompt));

        String response = null;
        try {
            response = llmProvider.generateResponse(llmMessages);
        } catch (Exception e) {
            log.error("LLM extraction failed: {}", e.getMessage());
            return Collections.emptyList();
        }

        // Parse response
        List<Map<String, Object>> extractedMemories = new ArrayList<>();
        try {
            response = removeCodeBlocks(response);
            if (StringUtils.isEmpty(response)) {
                extractedMemories = Collections.emptyList();
            } else {
                Map<String, Object> respMap = JsonUtil.read(response, new TypeReference<>() {});
                extractedMemories = (List<Map<String, Object>>) respMap.getOrDefault("memory", Collections.emptyList());
            }
        } catch (Exception e) {
            log.error("Error parsing extraction response: {}", e.getMessage());
            extractedMemories = Collections.emptyList();
        }

        if (extractedMemories.isEmpty()) {
            sqLiteManager.saveMessages(messages, sessionScope);
            return Collections.emptyList();
        }

        // Phase 3: Batch embed
        List<String> memTexts = new ArrayList<>();
        for (Map<String, Object> m : extractedMemories) {
            String t = (String) m.get("text");
            if (t != null) {
                memTexts.add(t);
            }
        }

        Map<String, Double[]> embedMap = new HashMap<>();
        try {
            List<Double[]> embeddingsList = embeddingProvider.embedBatch(memTexts);
            for (int i = 0; i < memTexts.size(); i++) {
                embedMap.put(memTexts.get(i), embeddingsList.get(i));
            }
        } catch (Exception e) {
            for (String text : memTexts) {
                try {
                    Double[] vec = embeddingProvider.embed(text);
                    embedMap.put(text, vec);
                } catch (Exception ex) {
                    log.warn("Failed to embed memory text: {}", ex.getMessage());
                }
            }
        }

        // Phase 4 + 5: Deduper
        Set<String> existingHashes = new HashSet<>();
        for (com.mem0.core.vectorstore.SearchResult res : existingResults) {
            Map<String, Object> payload = res.payload();
            if (payload == null) {
                continue;
            }
            if (payload.containsKey("hash")) {
                Object h = payload.get("hash");
                if (h != null) {
                    existingHashes.add((String) h);
                }
            }
        }

        List<MessageRecord> records = new ArrayList<>();
        Set<String> seenHashes = new HashSet<>();

        for (Map<String, Object> mem : extractedMemories) {
            String text = (String) mem.get("text");
            if (text == null || !embedMap.containsKey(text)) {
                continue;
            }

            String memHash = md5Hex(text);
            if (existingHashes.contains(memHash) || seenHashes.contains(memHash)) {
                log.debug("Skipping duplicate memory (hash match): {}", text.substring(0, Math.min(text.length(), 50)));
                continue;
            }
            seenHashes.add(memHash);

            String textLem = lemmatizeForBm25(text);
            String memoryId = UUID.randomUUID().toString();

            Map<String, Object> memMeta = new HashMap<>(metadata);
            memMeta.put("data", text);
            memMeta.put("text_lemmatized", textLem);
            memMeta.put("hash", memHash);

            if (!memMeta.containsKey("created_at")) {
                String now = ZonedDateTime.now(ZoneOffset.UTC).toString();
                memMeta.put("created_at", now);
                memMeta.put("updated_at", now);
            } else {
                memMeta.put("updated_at", memMeta.get("created_at"));
            }

            if (mem.get("attributed_to") != null) {
                memMeta.put("attributed_to", mem.get("attributed_to"));
            }

            records.add(new MessageRecord(memoryId, text, embedMap.get(text), memMeta));
        }

        if (records.isEmpty()) {
            sqLiteManager.saveMessages(messages, sessionScope);
            return Collections.emptyList();
        }

        // Phase 6: Batch persist
        List<Double[]> allVectors = new ArrayList<>();
        List<String> allIds = new ArrayList<>();
        List<Map<String, Object>> allPayloads = new ArrayList<>();
        for (MessageRecord r : records) {
            allVectors.add(r.embedding());
            allIds.add(r.id());
            allPayloads.add(r.metadata());
        }

        try {
            vectorStore.insert(allVectors, allPayloads, allIds);
        } catch (Exception e) {
            for (int i = 0; i < allIds.size(); i++) {
                try {
                    vectorStore.insert(
                            List.<Double[]>of(allVectors.get(i)),
                            List.of(allPayloads.get(i)),
                            List.of(allIds.get(i))
                    );
                } catch (Exception ex) {
                    log.error("Failed to insert memory {}: {}", allIds.get(i), ex.getMessage());
                }
            }
        }

        // Batch history
        List<Map<String, Object>> historyRecords = new ArrayList<>();
        for (MessageRecord r : records) {
            Map<String, Object> hr = new HashMap<>();
            hr.put("memory_id", r.id());
            hr.put("old_memory", null);
            hr.put("new_memory", r.text());
            hr.put("event", "ADD");
            hr.put("created_at", r.metadata().get("created_at"));
            hr.put("is_deleted", 0);
            historyRecords.add(hr);
        }

        try {
            sqLiteManager.batchAddHistory(historyRecords);
        } catch (Exception e) {
            for (Map<String, Object> hr : historyRecords) {
                try {
                    sqLiteManager.addHistory(
                            (String) hr.get("memory_id"),
                            null,
                            (String) hr.get("new_memory"),
                            "ADD"
                    );
                } catch (Exception ex) {
                    log.error("Failed to add history: {}", ex.getMessage());
                }
            }
        }

        // Phase 7: Entity linking
        //实体关系提取，这一块内容没有按照mem0来实现，换成了三元组的方式。
        try {
            List<String> allTexts = records.stream().map(MessageRecord::text).toList();
            List<List<EntityItem>> allEntities = llmEntityExtractor.extractEntitiesBatch(allTexts);

            //7a: Global dedup — collect unique entities across all memories
            Map<String, Object[]> globalEntities = new HashMap<>();
            for (int i = 0; i < records.size(); i++) {
                MessageRecord r = records.get(i);
                List<EntityItem> entities = i < allEntities.size() ? allEntities.get(i) : Collections.emptyList();

                for (EntityItem et : entities) {
                    String subject = et.getSubject();
                    String predicate = et.getPredicate();
                    String object = et.getObject();
                    String key = subject + predicate + object;
                    if (globalEntities.containsKey(key)) { //将重复的记录ID关联起来，去掉重复的特征属性
                        Object[] val = globalEntities.get(key);
                        Set<String> ids = (Set<String>) val[3];
                        ids.add(r.id());
                    } else {
                        Set<String> ids = new HashSet<>();
                        ids.add(r.id());
                        globalEntities.put(key, new Object[]{subject, predicate, object, ids});
                    }
                }
            }

            if (!globalEntities.isEmpty()) {
                List<String> orderedKeys = new ArrayList<>(globalEntities.keySet());
                List<String> entityTexts = new ArrayList<>();
                //key，已经是三元组的拼装了，这里不用再提取明细。
                //todo 三元组存在向量库里的实践方案了解，目前是直接把三元组拼装在一起
                for (String k : orderedKeys) {
//                    entityTexts.add((String) globalEntities.get(k)[1]);
                    entityTexts.add(k);
                }

                // 7b: Single batch embed for all unique entities
                List<Double[]> entityEmbeddings;
                try {
                    entityEmbeddings = embeddingProvider.embedBatch(entityTexts);
                } catch (Exception e) {
                    // Fallback: embed individually, use None for failures
                    entityEmbeddings = new ArrayList<>();
                    for (String t : entityTexts) {
                        try {
                            entityEmbeddings.add(embeddingProvider.embed(t));
                        } catch (Exception ex) {
                            entityEmbeddings.add(null);
                        }
                    }
                }

                List<Integer> validIndices = new ArrayList<>();
                List<String> validKeys = new ArrayList<>();
                List<Double[]> validVectors = new ArrayList<>();
                for (int i = 0; i < entityEmbeddings.size(); i++) {
                    Double[] vec = entityEmbeddings.get(i);
                    if (vec != null) {
                        validIndices.add(i);
                        validKeys.add(orderedKeys.get(i));
                        validVectors.add(vec);
                    }
                }

                //7c: Batch search for existing entities
                if (!validKeys.isEmpty()) {
                    List<List<com.mem0.core.vectorstore.SearchResult>> existingMatches = entityStore.searchBatch(
                            entityTexts,
                            validVectors,
                            1,
                            searchFilters
                    );

                    // 7d: Separate into inserts vs updates
                    List<Double[]> toInsertVectors = new ArrayList<>();
                    List<String> toInsertIds = new ArrayList<>();
                    List<Map<String, Object>> toInsertPayloads = new ArrayList<>();

                    for (int j = 0; j < validKeys.size(); j++) {
                        String key = validKeys.get(j);
                        Object[] val = globalEntities.get(key);
                        String subject = (String) val[0];
                        String predicate = (String) val[1];
                        String object = (String) val[2];
                        Set<String> memoryIds = (Set<String>) val[3];

                        List<com.mem0.core.vectorstore.SearchResult> matches = j < existingMatches.size() ? existingMatches.get(j) : Collections.emptyList();

                        if (!matches.isEmpty() && matches.getFirst().score() >= 0.95) {
                            com.mem0.core.vectorstore.SearchResult match = matches.getFirst();
                            Map<String, Object> payload = match.payload() != null ? new HashMap<>(match.payload()) : new HashMap<>();
                            Set<String> linked = new HashSet<>((List<String>) payload.getOrDefault("linked_memory_ids", Collections.emptyList()));
                            linked.addAll(memoryIds);
                            payload.put("linked_memory_ids", new ArrayList<>(linked));
                            try {
                                entityStore.update(match.id(), null, payload);
                            } catch (Exception e) {
                                log.debug("Entity update failed for subject'{}'-predicate'{}'-object'{}': {}",
                                        subject, predicate, object, e.getMessage());
                            }
                        } else {
                            // New entity — collect for batch insert
                            toInsertVectors.add(validVectors.get(j));
                            toInsertIds.add(UUID.randomUUID().toString());
                            Map<String, Object> payload = new HashMap<>();
                            payload.put("subject", subject);
                            payload.put("predicate", predicate);
                            payload.put("object", object);
                            payload.put("linked_memory_ids", new ArrayList<>(memoryIds));
                            payload.putAll(searchFilters);
                            toInsertPayloads.add(payload);
                        }
                    }
                    //7e: Single batch insert for all new entities
                    if (!toInsertVectors.isEmpty()) {
                        try {
                            entityStore.insert(toInsertVectors, toInsertPayloads, toInsertIds);
                        } catch (Exception e) {
                            log.warn("Batch entity insert failed: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Batch entity linking failed: {}", e.getMessage());
        }

        // Phase 8: Return
        sqLiteManager.saveMessages(messages, sessionScope);

        List<Map<String, Object>> returnedMemories = new ArrayList<>();
        for (MessageRecord r : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.id());
            item.put("memory", r.text());
            item.put("event", "ADD");
            returnedMemories.add(item);
        }

        telemetryService.captureEvent("mem0.add", null, Map.of("version", "v3", "sync_type", "sync"));
        return returnedMemories;
    }

    /**
     * 等价 Python parse_messages
     * 拼接消息为纯文本：role: content\n
     */
    private String parseMessages(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            String role = msg.role();
            String content = msg.content();
            if (role == null || content == null) {
                continue;
            }
            switch (role) {
                case "system", "user", "assistant" -> sb.append(role).append(": ").append(content).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 等价 Python _build_session_scope
     */
    private String buildSessionScope(Map<String, Object> filters) {
        List<String> keys = Arrays.asList("user_id", "agent_id", "run_id");
        Collections.sort(keys); // 保证排序一致

        List<String> parts = new ArrayList<>();
        for (String key : keys) {
            Object val = filters.get(key);
            if (val != null && !val.toString().isEmpty()) {
                parts.add(key + "=" + val);
            }
        }

        return String.join("&", parts);
    }

    /**
     * 等价 Python: parse_vision_messages
     */
    private List<Message> parseVisionMessages(List<Message> messages, Object visionDetails) throws JsonProcessingException {
        List<Message> returnedMessages = new ArrayList<>();

        for (Message msg : messages) {
            String role = msg.role();

            // 1. system 消息直接保留
            if (RoleEnum.system.name().equals(role)) {
                returnedMessages.add(msg);
                continue;
            }

            Object content = msg.content();

            // 2. content 是 list → 多图片
            if (content instanceof List) {
                String description = getImageDescription(msg, visionDetails);
                returnedMessages.add(new Message(role, description, ""));
            }

            // 3. content 是 dict，且 type=image_url → 单图片
            else if (content instanceof Map) {
                Map<String, Object> contentMap = (Map<String, Object>) content;
                if ("image_url".equals(contentMap.get("type"))) {
                    Map<String, Object> imageUrlObj = (Map<String, Object>) contentMap.get("image_url");
                    String imageUrl = (String) imageUrlObj.get("url");

                    try {
                        String description = getImageDescription(imageUrl, visionDetails);
                        returnedMessages.add(new Message(role, description, ""));
                    } catch (Exception e) {
                        throw new RuntimeException("Error while downloading " + imageUrl);
                    }
                } else {
                    returnedMessages.add(msg);
                }
            }

            // 4. 普通文本
            else {
                returnedMessages.add(msg);
            }
        }

        return returnedMessages;
    }

    /**
     * 简化重载（无 llm / visionDetails）
     */
    private List<Message> parseVisionMessages(List<Message> messages) throws JsonProcessingException {
        return parseVisionMessages(messages, "auto");
    }

    /**
     * 等价 Python: get_image_description
     */
    private String getImageDescription(Object imageObj, Object visionDetails) throws JsonProcessingException {
        List<Message> messages;

        // 如果 imageObj 是字符串（图片URL）
        if (imageObj instanceof String imageUrl) {
            // 构建文本提示
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", "A user is providing an image. Provide a high level description of the image and do not include any additional text.");

            // 构建图片
            Map<String, Object> imageUrlMap = new HashMap<>();
            imageUrlMap.put("url", imageUrl);
            imageUrlMap.put("detail", visionDetails);

            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image_url");
            imageContent.put("image_url", imageUrlMap);

            // 构建 content 数组
            List<Map<String, Object>> contentList = new ArrayList<>();
            contentList.add(textContent);
            contentList.add(imageContent);

            // 构建用户消息
            messages = Collections.singletonList(Message.user(JsonUtil.write(contentList)));
        } else {
            // 否则直接使用传入的消息结构
            messages = Collections.singletonList(JsonUtil.read(JsonUtil.write(imageObj), Message.class));
        }

        // 调用大模型获取图片描述
        return llmProvider.generateResponse(messages);
    }

    /**
     * 创建过程记忆
     * 对应 Python: _create_procedural_memory
     */
    public Map<String, Object> createProceduralMemory(List<Message> messages,
                                                      Map<String, Object> metadata,
                                                      String prompt) {
        // = 1. 构建 LLM 消息 =
        List<Message> parsedMessages = new ArrayList<>();

        // system prompt
        Message systemMsg = Message.system(StringUtils.isNotEmpty(prompt) ? prompt : PROCEDURAL_MEMORY_SYSTEM_PROMPT);
        parsedMessages.add(systemMsg);

        // 原消息
        parsedMessages.addAll(messages);

        // 最后追加用户指令
        Message userMsg = Message.user("Create procedural memory of the above conversation.");

        parsedMessages.add(userMsg);

        // = 2. 调用 LLM 生成 =
        String proceduralMemory;
        try {
            proceduralMemory = llmProvider.generateResponse(parsedMessages, new LLMConfig(0.0, 2000, null, null));
            proceduralMemory = removeCodeBlocks(proceduralMemory);
        } catch (Exception e) {
            log.error("Error generating procedural memory summary: {}", e.getMessage());
            throw e;
        }

        // = 3. 元数据校验 =
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be done for procedural memory.");
        }

        // 追加 memory_type
        Map<String, Object> finalMetadata = new HashMap<>(metadata);
        finalMetadata.put("memory_type", MemoryType.PROCEDURAL.getValue());

        // = 4. 生成向量 =
        Double[] embeddings = embeddingProvider.embed(proceduralMemory);

        // = 5. 保存记忆 =
        String memoryId = createMemory(proceduralMemory,
                Map.of(proceduralMemory, embeddings),
                finalMetadata
        );

        // = 6. 埋点事件 =
        telemetryService.captureEvent("mem0._create_procedural_memory", null, Map.of("memory_id", memoryId, "sync_type", "sync"));

        // = 7. 构造返回结果 =
        Map<String, Object> resultItem = new HashMap<>();
        resultItem.put("id", memoryId);
        resultItem.put("memory", proceduralMemory);
        resultItem.put("event", MemoryEvent.ADD.name());
        return resultItem;
    }

    /**
     * 移除代码块 ```...``` 和 ... 标签
     */
    private String removeCodeBlocks(String content) {
        if (content == null) {
            return "";
        }

        // 1. 先 trim
        String stripped = content.strip();

        // 2. 匹配代码块：```[语言]\n内容\n```
        Pattern codePattern = Pattern.compile("^```[a-zA-Z0-9]*\\n([\\s\\S]*?)\\n```$");
        Matcher matcher = codePattern.matcher(stripped);

        String matchRes;
        if (matcher.matches()) {
            matchRes = matcher.group(1).strip();
        } else {
            matchRes = stripped;
        }

        // 3. 移除 ...（支持跨行）
        Pattern thinkPattern = Pattern.compile(".*?", Pattern.DOTALL);

        return thinkPattern.matcher(matchRes).replaceAll("").strip();
    }

    /**
     * 搜索记忆（完全对齐Python原版逻辑）
     */
    public Map<String, List<Map<String, Object>>> search(String query,
                                                         Integer topK,
                                                         SearchFilters filters,
                                                         Double threshold,
                                                         Boolean rerank,
                                                         Map<String, Object> params) {
        // 1. 设置默认值（对齐Python默认参数）
        topK = topK == null ? 20 : topK;
        threshold = threshold == null ? 0.1 : threshold;
        rerank = rerank != null && rerank;
        Map<String, Object> effectiveFilters = filters == null ? new HashMap<>() : new HashMap<>(Map.of(
                "user_id", filters.userId(), "agent_id", filters.agentId(), "run_id", filters.runId()));

        try {
            // 2. 拒绝顶级实体参数（对齐 _reject_top_level_entity_params）
            rejectTopLevelEntityParams(params, "search");

            // 3. 校验搜索参数（对齐 _validate_search_params）
            validateSearchParams(threshold, topK);

            // 4. 校验并修剪 entity ID（user_id/agent_id/run_id）
            if (effectiveFilters.containsKey("user_id")) {
                effectiveFilters.put("user_id", validateAndTrimEntityId(effectiveFilters.get("user_id"), "user_id"));
            }
            if (effectiveFilters.containsKey("agent_id")) {
                effectiveFilters.put("agent_id", validateAndTrimEntityId(effectiveFilters.get("agent_id"), "agent_id"));
            }
            if (effectiveFilters.containsKey("run_id")) {
                effectiveFilters.put("run_id", validateAndTrimEntityId(effectiveFilters.get("run_id"), "run_id"));
            }

            // 5. 强制校验：必须包含至少一个过滤ID
            if (Stream.of("user_id", "agent_id", "run_id").noneMatch(effectiveFilters::containsKey)) {
                throw new IllegalArgumentException(
                        "filters must contain at least one of: user_id, agent_id, run_id. " +
                                "Example: filters={'user_id': 'u1'}"
                );
            }

            int limit = topK;

            // 6. 处理高级元数据过滤器（eq/ne/in/nin/gt/gte等）
            if (MetadataFilter.hasAdvancedOperators(effectiveFilters)) {
                Map<String, Object> processedFilters = processMetadataFilters(effectiveFilters);
                // 移除逻辑运算符键
                List.of("AND", "OR", "NOT").forEach(effectiveFilters::remove);
                // 移除已处理的高级过滤键
                List<String> keysToRemove = effectiveFilters.keySet().stream()
                        .filter(fk -> !List.of("AND", "OR", "NOT", "user_id", "agent_id", "run_id").contains(fk))
                        .filter(fk -> effectiveFilters.get(fk) instanceof Map)
                        .toList();
                keysToRemove.forEach(effectiveFilters::remove);
                effectiveFilters.putAll(processedFilters);
            }

            // 7. 遥测处理（对齐 process_telemetry_filters + capture_event）
            Map<String, Object> telemetry = processTelemetryFilters(effectiveFilters);
//            captureEvent("mem0.search", Map.of(
//                    "limit", limit,
//                    "version", "v1",
//                    "keys", telemetry.get("keys"),
//                    "encoded_ids", telemetry.get("encoded_ids"),
//                    "sync_type", "sync",
//                    "threshold", threshold,
//                    "advanced_filters", hasAdvancedOperators(filters)
//            ));

            // 8. 向量库搜索（核心）
            List<Map<String, Object>> originalMemories = searchVectorStore(query, effectiveFilters, limit, threshold);

            // 9. 重排序（可选）
            if (Boolean.TRUE.equals(rerank) && reranker != null && !CollectionUtils.isEmpty(originalMemories)) {
                try {
                    originalMemories = reranker.rerank(query, originalMemories, limit);
                } catch (Exception e) {
                    log.warn("Reranking failed, using original results: {}", e.getMessage());
                }
            }

            // 10. 返回结果（对齐Python {"results": [...]}）
            Map<String, List<Map<String, Object>>> result = new HashMap<>();
            result.put("results", originalMemories);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Search failed: " + e.getMessage());
        }
    }

    /**
     * 1:1 极致复刻 Python _process_metadata_filters
     * 处理高级元数据过滤器，转换为向量库兼容格式
     * 支持：eq/ne/gt/gte/lt/lte/in/nin/contains/icontains + AND/OR/NOT逻辑运算
     */
    private Map<String, Object> processMetadataFilters(Map<String, Object> metadataFilters) {
        Map<String, Object> processedFilters = new HashMap<>();

        // 遍历所有过滤条件，处理逻辑运算符 + 普通条件
        for (Map.Entry<String, Object> entry : metadataFilters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            switch (key) {
                case "AND":
                    // 处理逻辑 AND：合并所有条件
                    if (!(value instanceof List<?>)) {
                        throw new IllegalArgumentException("AND operator requires a list of conditions");
                    }
                    List<?> andConditions = (List<?>) value;
                    for (Object cond : andConditions) {
                        if (!(cond instanceof Map<?, ?>)) {
                            continue;
                        }
                        Map<String, Object> conditionMap = (Map<String, Object>) cond;
                        for (Map.Entry<String, Object> subEntry : conditionMap.entrySet()) {
                            Map<String, Object> condResult = processCondition(subEntry.getKey(), subEntry.getValue());
                            mergeFilters(processedFilters, condResult);
                        }
                    }
                    break;

                case "OR":
                    // 处理逻辑 OR：转换为 $or 列表（向量库标准格式）
                    if (!(value instanceof List<?>) || CollectionUtils.isEmpty((List<?>) value)) {
                        throw new IllegalArgumentException("OR operator requires a non-empty list of conditions");
                    }
                    List<Map<String, Object>> orList = new ArrayList<>();
                    List<?> orConditions = (List<?>) value;
                    for (Object cond : orConditions) {
                        if (!(cond instanceof Map<?, ?>)) {
                            continue;
                        }
                        Map<String, Object> orCondition = new HashMap<>();
                        Map<String, Object> conditionMap = (Map<String, Object>) cond;
                        for (Map.Entry<String, Object> subEntry : conditionMap.entrySet()) {
                            Map<String, Object> condResult = processCondition(subEntry.getKey(), subEntry.getValue());
                            mergeFilters(orCondition, condResult);
                        }
                        orList.add(orCondition);
                    }
                    processedFilters.put("$or", orList);
                    break;

                case "NOT":
                    // 处理逻辑 NOT：转换为 $not 列表（向量库标准格式）
                    if (!(value instanceof List<?>) || CollectionUtils.isEmpty((List<?>) value)) {
                        throw new IllegalArgumentException("NOT operator requires a non-empty list of conditions");
                    }
                    List<Map<String, Object>> notList = new ArrayList<>();
                    List<?> notConditions = (List<?>) value;
                    for (Object cond : notConditions) {
                        if (!(cond instanceof Map<?, ?>)) {
                            continue;
                        }
                        Map<String, Object> notCondition = new HashMap<>();
                        Map<String, Object> conditionMap = (Map<String, Object>) cond;
                        for (Map.Entry<String, Object> subEntry : conditionMap.entrySet()) {
                            Map<String, Object> condResult = processCondition(subEntry.getKey(), subEntry.getValue());
                            mergeFilters(notCondition, condResult);
                        }
                        notList.add(notCondition);
                    }
                    processedFilters.put("$not", notList);
                    break;

                default:
                    // 普通条件：直接处理并合并
                    Map<String, Object> condResult = processCondition(key, value);
                    mergeFilters(processedFilters, condResult);
            }
        }

        return processedFilters;
    }

    /**
     * 处理单个过滤条件（等值匹配 / 通配符 / 高级运算符）
     */
    private Map<String, Object> processCondition(String key, Object condition) {
        // 非字典类型：简单等值匹配 或 通配符 *
        if (!(condition instanceof Map<?, ?>)) {
            Map<String, Object> result = new HashMap<>();
            if ("*".equals(condition)) {
                result.put(key, "*");
            } else {
                result.put(key, condition);
            }
            return result;
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> conditionMap = (Map<String, Object>) condition;

        // 支持的运算符（与Python完全一致）
        Set<String> SUPPORTED_OPERATORS = Set.of(
                "eq", "ne", "gt", "gte", "lt", "lte",
                "in", "nin", "contains", "icontains"
        );

        for (Map.Entry<String, Object> opEntry : conditionMap.entrySet()) {
            String operator = opEntry.getKey();
            Object value = opEntry.getValue();

            if (!SUPPORTED_OPERATORS.contains(operator)) {
                throw new IllegalArgumentException("Unsupported metadata filter operator: " + operator);
            }

            // 构建嵌套结构：{key: {operator: value}}
            result.computeIfAbsent(key, k -> new HashMap<>());
            ((Map<String, Object>) result.get(key)).put(operator, value);
        }

        return result;
    }

    /**
     * 深度合并过滤器：嵌套字典自动合并，普通字段直接覆盖
     */
    private void mergeFilters(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object sourceValue = entry.getValue();
            Object targetValue = target.get(key);

            // 深度合并嵌套字典
            if (target.containsKey(key) && targetValue instanceof Map && sourceValue instanceof Map) {
                ((Map<String, Object>) targetValue).putAll((Map<String, Object>) sourceValue);
            } else {
                target.put(key, sourceValue);
            }
        }
    }

    private List<Map<String, Object>> searchVectorStore(String query, Map<String, Object> filters, Integer limit, Double threshold) {
        // 1. 兼容空阈值（向后兼容）
        if (threshold == null) {
            threshold = 0.1;
        }

        // 2. 预处理查询：分词 + 提取实体
        String queryLemmatized = lemmatizeForBm25(query);
        List<EntityItem> queryEntities = llmEntityExtractor.singleTextExtract(query);

        // 3. 生成查询向量
        Double[] embeddings = embeddingProvider.embed(query);

        // 4. 语义搜索：扩大检索池（max(limit*4, 60)）
        int internalLimit = Math.max(limit * 4, 60);
        List<com.mem0.core.vectorstore.SearchResult> semanticResults = vectorStore.search(embeddings, internalLimit, filters);

        // 5. 关键词搜索（BM25，存储支持时执行）
        List<com.mem0.core.vectorstore.SearchResult> keywordResults = vectorStore.keywordSearch(queryLemmatized, internalLimit, filters);

        // 6. 计算 BM25 归一化分数
        Map<String, Double> bm25Scores = new HashMap<>();
        if (!CollectionUtils.isEmpty(keywordResults)) {
            // 获取 BM25 参数
            Bm25Params params = getBm25Params(query, queryLemmatized);
            for (com.mem0.core.vectorstore.SearchResult mem : keywordResults) {
                String memId = mem.id();
                Double rawScore = mem.score();
                if (rawScore != null && rawScore > 0) {
                    bm25Scores.put(memId, normalizeBm25(rawScore, params.midpoint(), params.steepness()));
                }
            }
        }

        // 7. 计算实体增强分数
        Map<String, Double> entityBoosts = new HashMap<>();
        if (!CollectionUtils.isEmpty(queryEntities)) {
            entityBoosts = computeEntityBoosts(queryEntities, filters);
        }

        // 8. 构建候选集（来自语义搜索结果）
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (com.mem0.core.vectorstore.SearchResult mem : semanticResults) {
            Map<String, Object> candidate = new HashMap<>();
            candidate.put("id", mem.id());
            candidate.put("score", mem.score());
            candidate.put("payload", mem.payload() == null ? new HashMap<>() : mem.payload());
            candidates.add(candidate);
        }

        // 9. 评分与排序（核心）
        List<Map<String, Object>> scoredResults = scoreAndRank(candidates, bm25Scores, entityBoosts, threshold, limit);

        // 10. 格式化结果（完全对齐 Python MemoryItem 结构）
        List<Map<String, Object>> originalMemories = new ArrayList<>();
        for (Map<String, Object> scored : scoredResults) {
            Map<String, Object> payload = (Map<String, Object>) scored.getOrDefault("payload", new HashMap<>());
            // 跳过无数据的候选
            String data = (String) payload.get("data");
            if (data == null || data.isBlank()) {
                continue;
            }

            // 构建基础 MemoryItem 结构
            Map<String, Object> memoryItem = new HashMap<>();
            memoryItem.put("id", scored.get("id"));
            memoryItem.put("memory", payload.getOrDefault("data", ""));
            memoryItem.put("hash", payload.get("hash"));
            memoryItem.put("created_at", payload.get("created_at"));
            memoryItem.put("updated_at", payload.get("updated_at"));
            memoryItem.put("score", scored.get("score"));

            // 提升关键字段到顶层
            for (String key : PROMOTED_PAYLOAD_KEYS) {
                if (payload.containsKey(key)) {
                    memoryItem.put(key, payload.get(key));
                }
            }

            // 封装额外元数据
            Map<String, Object> additionalMetadata = new HashMap<>();
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                String key = entry.getKey();
                if (!CORE_AND_PROMOTED_KEYS.contains(key)) {
                    additionalMetadata.put(key, entry.getValue());
                }
            }
            if (!additionalMetadata.isEmpty()) {
                memoryItem.putIfAbsent("metadata", new HashMap<>());
                ((Map<String, Object>) memoryItem.get("metadata")).putAll(additionalMetadata);
            }

            originalMemories.add(memoryItem);
        }

        // 11. 返回最终结果
        return originalMemories;
    }

    //_compute_entity_boosts 核心方法
    private Map<String, Double> computeEntityBoosts(List<EntityItem> queryEntities, Map<String, Object> filters) {
        // 1. 实体去重 + 限制最多8个（严格对齐Python）
        Set<String> seen = new HashSet<>();
        List<EntityItem> deduped = new ArrayList<>();

        // 截取前8个实体
        List<EntityItem> limitedEntities = queryEntities.stream().limit(8).toList();

        for (EntityItem entity : limitedEntities) {
            String subject = entity.getSubject();
            String predicate = entity.getPredicate();
            String object = entity.getObject();
            // 去重规则：strip() + 小写
            String key = subject + predicate + object;

            if (!key.isBlank() && !seen.contains(key)) {
                seen.add(key);
                deduped.add(new EntityItem(subject, predicate, object));
            }
        }

        // 无有效实体，直接返回空Map
        if (CollectionUtils.isEmpty(deduped)) {
            return Collections.emptyMap();
        }

        // 2. 构建搜索过滤条件：仅保留 user_id/agent_id/run_id 且非空
        Map<String, Object> searchFilters = new HashMap<>();
        if (filters != null && !filters.isEmpty()) {
            for (String key : Arrays.asList("user_id", "agent_id", "run_id")) {
                if (filters.containsKey(key) && filters.get(key) != null) {
                    searchFilters.put(key, filters.get(key));
                }
            }
        }

        Map<String, Double> memoryBoosts = new HashMap<>();

        try {
            // 3. 遍历去重后的实体，计算增强分数
            for (EntityItem entity : deduped) {
                String subject = entity.getSubject();
                String predicate = entity.getPredicate();
                String object = entity.getObject();
                String key = subject + predicate + object;
                // 生成实体嵌入向量
                Double[] entityEmbedding = embeddingProvider.embed(key);

                // 搜索实体库：top_k=500，匹配Python参数
                List<com.mem0.core.vectorstore.SearchResult> matches = entityStore.search(entityEmbedding, 500, searchFilters
                );

                // 4. 遍历匹配到的实体
                for (com.mem0.core.vectorstore.SearchResult match : matches) {
                    // 相似度阈值：≥0.5（严格对齐）
                    double similarity = Optional.ofNullable(match.score()).orElse(0.0);
                    if (similarity < 0.5) {
                        continue;
                    }

                    // 获取关联记忆ID，空值安全
                    Map<String, Object> payload = Optional.ofNullable(match.payload()).orElse(new HashMap<>());
                    Object linkedIdsObj = payload.get("linked_memory_ids");
                    // 校验必须是列表
                    if (!(linkedIdsObj instanceof List<?>)) {
                        continue;
                    }
                    List<String> linkedMemoryIds = (List<String>) linkedIdsObj;

                    // 5. 计算衰减权重：关联记忆越多，权重越低
                    int numLinked = Math.max(linkedMemoryIds.size(), 1);
                    double memoryCountWeight = 1.0 / (1.0 + 0.001 * Math.pow((numLinked - 1), 2));
                    // 计算最终增强分数
                    double boost = similarity * ENTITY_BOOST_WEIGHT * memoryCountWeight;

                    // 6. 更新记忆的最大增强分数
                    for (String memoryId : linkedMemoryIds) {
                        if (memoryId != null && !memoryId.isBlank()) {
                            String memoryKey = memoryId.strip();
                            // 保留最大值
                            double currentMax = memoryBoosts.getOrDefault(memoryKey, 0.0);
                            memoryBoosts.put(memoryKey, Math.max(currentMax, boost));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 异常捕获：严格对齐Python logger.warning
            log.warn("Entity boost computation failed: {}", e.getMessage());
        }

        return memoryBoosts;
    }


    /**
     * 更新记忆
     *
     * @param memoryId 记忆ID
     * @param request  记忆内容、元数据
     * @return Map<String, String>
     */
    public Map<String, String> update(String memoryId, UpdateMemoryRequest request) {

        String data = request.content();
        Map<String, Object> metadata = request.metadata();

        // 1. 遥测事件捕获
        //captureEvent("mem0.update", Map.of("memory_id", memoryId, "sync_type", "sync"));

        // 2. 生成嵌入向量 (existing_embeddings = {data: embed(data, "update")})
        Map<String, Double[]> existingEmbeddings = Map.of(data, embeddingProvider.embed(data)
        );

        // 3. 调用内部私有方法更新记忆
        updateMemory(memoryId, data, existingEmbeddings, metadata);

        // 4. 返回固定成功消息 (完全对齐)
        return Map.of("message", "Memory updated successfully!");
    }

    public void updateMemory(String memoryId,
                             String data,
                             Map<String, Double[]> existingEmbeddings,
                             Map<String, Object> metadata) {
        log.info("Updating memory with data={}", data);

        VectorEntry existingMemory;
        try {
            // 1. 获取原有记忆
            Optional<VectorEntry> optionalVectorEntry = vectorStore.get(memoryId);
            existingMemory = optionalVectorEntry.orElse(null);
        } catch (Exception e) {
            log.error("Error getting memory with ID {} during update.", memoryId, e);
            throw new IllegalArgumentException("Error getting memory with ID " + memoryId + ". Please provide a valid 'memory_id'");
        }

        // 2. 校验记忆存在
        if (existingMemory == null) {
            throw new IllegalArgumentException("Memory with id " + memoryId + " not found. Please provide a valid 'memory_id'");
        }

        Map<String, Object> existingPayload = existingMemory.payload() == null ? new HashMap<>() : existingMemory.payload();

        // 3. 获取旧数据
        String prevValue = (String) existingPayload.getOrDefault("data", "");

        //mem0没有判断输入需要更新的data是否一致，我们这里加一个判断，相同的则直接返回，不做任何操作
        if (prevValue.equals(data)) {
            return;
        }

        // 4. 深拷贝元数据（无则新建空Map）
        Map<String, Object> newMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();

        // 5. 填充核心元数据：mem0这里的update，不是像add那样归纳后再更新，所以传入什么直接memory表记录直接更新；
        newMetadata.put("data", data);
        // MD5 哈希（等价Python hashlib.md5）
        newMetadata.put("hash", md5Hex(data));
        // 文本分词（等价Python lemmatize_for_bm25）
        newMetadata.put("text_lemmatized", lemmatizeForBm25(data));
        // 保留原有创建时间
        newMetadata.put("created_at", existingPayload.get("created_at"));
        // UTC 更新时间
        newMetadata.put("updated_at", LocalDateTime.now().toString());

        // 6. 保留原有会话标识（新元数据无则继承旧的）
        if (!newMetadata.containsKey("user_id") && existingPayload.containsKey("user_id")) {
            newMetadata.put("user_id", existingPayload.get("user_id"));
        }
        if (!newMetadata.containsKey("agent_id") && existingPayload.containsKey("agent_id")) {
            newMetadata.put("agent_id", existingPayload.get("agent_id"));
        }
        if (!newMetadata.containsKey("run_id") && existingPayload.containsKey("run_id")) {
            newMetadata.put("run_id", existingPayload.get("run_id"));
        }
        // 直接继承 actor_id
        if (existingPayload.containsKey("actor_id")) {
            newMetadata.put("actor_id", existingPayload.get("actor_id"));
        }
        // 保留 role
        if (!newMetadata.containsKey("role") && existingPayload.containsKey("role")) {
            newMetadata.put("role", existingPayload.get("role"));
        }

        // 7. 获取向量（优先用现有，否则重新生成）
        Double[] embeddings;
        if (existingEmbeddings.containsKey(data)) {
            embeddings = existingEmbeddings.get(data);
        } else {
            embeddings = embeddingProvider.embed(data);
        }

        // 8. 更新向量库
        vectorStore.update(memoryId, embeddings, newMetadata);
        log.info("Updating memory with ID {} with data={}", memoryId, data);

        // 9. 添加历史记录（等价Python self.db.add_history）
        Date createdAt = parseUtcDate((String) newMetadata.get("created_at"));
        Date updatedAt = parseUtcDate((String) newMetadata.get("updated_at"));
        sqLiteManager.addHistory(
                memoryId,
                prevValue,
                data,
                "UPDATE",
                createdAt,
                updatedAt,
                false,
                (String) newMetadata.get("actor_id"),
                (String) newMetadata.get("role"));

        // 10. 实体存储清理 + 重新关联（等价Python逻辑）
        Map<String, Object> sessionFilters = new HashMap<>();
        for (String key : Arrays.asList("user_id", "agent_id", "run_id")) {
            if (newMetadata.containsKey(key)) {
                sessionFilters.put(key, newMetadata.get(key));
            }
        }
        removeMemoryFromEntityStore(memoryId, sessionFilters);
        linkEntitiesForMemory(memoryId, data, sessionFilters);
    }

    /**
     * 解析 2026-05-25T01:43:56.907648Z → java.util.Date
     * 无格式错误、支持6位微秒、自动识别UTC时区
     */
    private Date parseUtcDate(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return Date.from(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
        }

        try {
            // 带Z：直接解析
            if (timestamp.endsWith("Z")) {
                return Date.from(ZonedDateTime.parse(timestamp).toInstant());
            }
            // 不带Z：按UTC时区解析
            else {
                LocalDateTime localDateTime = LocalDateTime.parse(timestamp);
                return Date.from(localDateTime.atZone(ZoneOffset.UTC).toInstant());
            }
        } catch (Exception e) {
            return new Date();
        }
    }

    private void removeMemoryFromEntityStore(String memoryId, Map<String, Object> filters) {
        // 2. 构建搜索过滤条件：仅保留 user_id/agent_id/run_id 且非空
        Map<String, Object> searchFilters = new HashMap<>();
        if (filters != null && !filters.isEmpty()) {
            for (String key : Arrays.asList("user_id", "agent_id", "run_id")) {
                if (filters.containsKey(key) && filters.get(key) != null) {
                    searchFilters.put(key, filters.get(key));
                }
            }
        }

        try {
            // 3. 查询实体列表（top_k=10000）
            List<VectorEntry> rows = entityStore.list(searchFilters, 10000);
            // 4. 解包双层列表:查询结果构建单层，与python有差别

            // 5. 遍历所有实体
            for (VectorEntry row : rows) {
                try {
                    // 6. 获取payload，空值安全
                    Map<String, Object> payload = Optional.ofNullable(row.payload()).orElse(new HashMap<>());
                    // 7. 获取关联的内存ID列表
                    List<String> linkedMemoryIds = (List<String>) payload.getOrDefault("linked_memory_ids", new ArrayList<>());
                    // 8. 不包含当前memoryId，跳过
                    if (!linkedMemoryIds.contains(memoryId)) {
                        continue;
                    }

                    // 9. 移除当前memoryId，剩余ID
                    List<String> remainingIds = linkedMemoryIds.stream()
                            .filter(mid -> !mid.equals(memoryId))
                            .collect(Collectors.toList());

                    // 10. 剩余为空 → 删除实体
                    if (CollectionUtils.isEmpty(remainingIds)) {
                        try {
                            entityStore.delete(row.id());
                        } catch (Exception e) {
                            log.error("Entity delete failed for id={}: {}", row.id(), e.getMessage());
                        }
                    } else {
                        // 11. 剩余不为空 → 重新生成向量并更新
                        String entityText = (String) payload.get("data");
                        if (entityText == null || entityText.isBlank()) {
                            log.debug("Entity id={} missing 'data'; skipping update during cleanup", row.id());
                            continue;
                        }

                        // 重新生成嵌入向量
                        Double[] vec;
                        try {
                            vec = embeddingProvider.embed(entityText);
                        } catch (Exception e) {
                            log.debug("Entity re-embed failed for '{}': {}", entityText, e.getMessage());
                            continue;
                        }

                        // 构建新payload
                        Map<String, Object> newPayload = new HashMap<>(payload);
                        newPayload.put("linked_memory_ids", remainingIds);

                        // 更新实体
                        try {
                            entityStore.update(row.id(), vec, newPayload);
                        } catch (Exception e) {
                            log.debug("Entity update failed for id={}: {}", row.id(), e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    // 单实体异常，DEBUG吞吃
                    log.debug("Entity cleanup error: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            // 整体清理失败，WARNING日志
            log.error("Entity store cleanup failed for memory_id={}: {}", memoryId, e.getMessage());
        }
    }

    // _link_entities_for_memory
    private void linkEntitiesForMemory(String memoryId, String text, Map<String, Object> filters) {
        try {
            // 1. 提取实体（对应Python extract_entities）
            List<EntityItem> entities = llmEntityExtractor.singleTextExtract(text);
            if (CollectionUtils.isEmpty(entities)) {
                return;
            }

            // 2. 去重：小写+去空格
            Set<String> seen = new HashSet<>();
            for (EntityItem entity : entities) {
                String subject = entity.getSubject();
                String predicate = entity.getPredicate();
                String object = entity.getObject();
                String key = subject + predicate + object;
                if (key.isBlank() || seen.contains(key)) {
                    continue;
                }
                seen.add(key);

                // 3. 插入/更新实体
                try {
                    upsertEntity(subject, predicate, object, memoryId, filters);
                } catch (Exception e) {
                    log.debug("Entity link failed for subject'{}'-predicate'{}'-object'{}': {}", subject, predicate, object, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Entity linking failed for memory_id={}: {}", memoryId, e.getMessage());
        }
    }

    private void upsertEntity(String subject, String predicate, String object, String memoryId, Map<String, Object> filters) {
        // 1. 生成实体嵌入向量（任务类型：add）-- 三元组方式，拼装在一起，与python不一致。
        String entityText = subject + predicate + object;
        try {
            Double[] entityEmbedding = embeddingProvider.embed(entityText);

            // 2. 构建搜索过滤条件：仅保留 user_id/agent_id/run_id 且非空
            Map<String, Object> searchFilters = new HashMap<>();
            if (filters != null && !filters.isEmpty()) {
                for (String key : Arrays.asList("user_id", "agent_id", "run_id")) {
                    if (filters.containsKey(key) && filters.get(key) != null) {
                        searchFilters.put(key, filters.get(key));
                    }
                }
            }

            // 3. 搜索相似实体（top_k=1，匹配Python逻辑）
            List<com.mem0.core.vectorstore.SearchResult> existingEntities = entityStore.search(entityEmbedding, 1, searchFilters);

            // 4. 判断：存在匹配实体 + 相似度 ≥ 0.95 → 更新关联ID
            if (CollectionUtils.isNotEmpty(existingEntities)) {
                com.mem0.core.vectorstore.SearchResult matchEntity = existingEntities.getFirst();
                // 相似度阈值严格对齐：0.95
                if (matchEntity.score() != null && matchEntity.score() >= 0.95) {
                    Map<String, Object> payload = Optional.ofNullable(matchEntity.payload()).orElse(new HashMap<>());
                    // 获取关联内存ID列表
                    List<String> linkedMemoryIds = (List<String>) payload.getOrDefault("linked_memory_ids", new ArrayList<>());

                    // 内存ID不存在则添加
                    if (!linkedMemoryIds.contains(memoryId)) {
                        linkedMemoryIds.add(memoryId);
                        payload.put("linked_memory_ids", linkedMemoryIds);

                        // 更新实体（vector=null，仅更新payload，对齐Python）
                        entityStore.update(matchEntity.id(), null, payload);
                    }
                }
            } else {
                // 5. 无匹配实体 → 创建新实体
                String entityId = UUID.randomUUID().toString();

                // 构建实体payload（合并过滤条件 + 核心字段）
                Map<String, Object> payload = new HashMap<>();
                payload.put("subject", subject);
                payload.put("predicate", predicate);
                payload.put("object", object);
                payload.put("linked_memory_ids", Collections.singletonList(memoryId));
                // 合并搜索过滤条件（user_id/agent_id/run_id）
                payload.putAll(searchFilters);

                // 插入新实体（对齐Python vectors/ids/payloads 数组传参）
                entityStore.insert(Collections.singletonList(entityEmbedding), Collections.singletonList(payload), Collections.singletonList(entityId));
            }

        } catch (Exception e) {
            // 异常捕获：严格对齐Python logger.warning
            log.warn("Entity upsert failed for '{}': {}", entityText, e.getMessage());
        }
    }

    /**
     * 删除单条记忆（含历史记录、实体关联清理）
     *
     * @param memoryId       记忆ID
     * @param existingMemory 预加载的记忆对象（可选，为空则自动查询）
     */
    public void deleteMemory(String memoryId, @Nullable VectorEntry existingMemory) {
        // 1. 日志打印（严格对齐 Python 格式）
        log.info("Deleting memory with memory_id={}", memoryId);

        // 2. 未传入记忆对象则从向量库查询，不存在则抛异常
        if (existingMemory == null) {
            Optional<VectorEntry> vectorEntry = vectorStore.get(memoryId);
            existingMemory = vectorEntry.orElse(null);
            if (existingMemory == null) {
                throw new IllegalArgumentException("Memory with id " + memoryId + " not found. Please provide a valid 'memory_id'");
            }
        }

        // 3. 提取记忆核心数据
        Map<String, Object> payload = existingMemory.payload() == null ? new HashMap<>() : existingMemory.payload();
        String prevValue = Objects.toString(payload.get("data"), "");

        // 4. 时间标准化：created_at UTC 格式化（对齐 Python _normalize_iso_timestamp_to_utc）
        String createdAtStr = Objects.toString(payload.get("created_at"), "");
        Date createdAt = parseUtcDate(createdAtStr);

        // 5. 当前 UTC 时间作为 updated_at（对齐 Python datetime.now(timezone.utc).isoformat()）
        Date updatedAt = new Date();

        // 6. 构建会话过滤条件：提取 user_id/agent_id/run_id（仅保留非空）
        Map<String, Object> sessionFilters = new HashMap<>();
        for (String key : new String[]{"user_id", "agent_id", "run_id"}) {
            Object value = payload.get(key);
            if (value != null && !value.toString().isBlank()) {
                sessionFilters.put(key, value);
            }
        }

        // 7. 从向量库删除记忆
        vectorStore.delete(memoryId);

        // 8. 记录删除历史到数据库（对齐 Python db.add_history）
        sqLiteManager.addHistory(
                memoryId,
                prevValue,
                null,
                MemoryEvent.DELETE.name(),
                createdAt,
                updatedAt,
                true, // is_deleted=1
                Objects.toString(payload.get("actor_id"), null),
                Objects.toString(payload.get("role"), null)
        );

        // 9. 实体存储清理：从关联实体中移除该记忆ID（非致命错误，内部捕获异常）
        try {
            removeMemoryFromEntityStore(memoryId, sessionFilters);
        } catch (Exception e) {
            log.warn("Failed to remove memory from entity store: {}", e.getMessage());
        }
    }

    public Map<String, Object> get(String memoryId) {
        // 1. 遥测事件捕获
        telemetryService.captureEvent("mem0.get", null, Map.of("memory_id", memoryId, "sync_type", "sync"));

        // 2. 查询数据
        Optional<MemoryVectorEntry> optionalMemory = vectorStore.getByIdWithoutVector(memoryId);

        // 3. 空数据返回 null
        if (optionalMemory.isEmpty()) {
            return null;
        }

        MemoryVectorEntry memory = optionalMemory.get();

        Map<String, Object> payload = memory.payload() == null ? new HashMap<>() : memory.payload();

        // 4. 构建结果项（等价 MemoryItem.model_dump()）
        Map<String, Object> resultItem = new HashMap<>();
        resultItem.put("id", memory.id());
        resultItem.put("memory", payload.getOrDefault("data", ""));
        resultItem.put("hash", payload.get("hash"));
        resultItem.put("created_at", payload.get("created_at"));
        resultItem.put("updated_at", payload.get("updated_at"));

        // 5. 提升关键字段到顶层
        for (String key : PROMOTED_PAYLOAD_KEYS) {
            if (payload.containsKey(key)) {
                resultItem.put(key, payload.get(key));
            }
        }

        // 6. 封装额外元数据
        Map<String, Object> additionalMetadata = new HashMap<>();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String key = entry.getKey();
            if (!CORE_AND_PROMOTED_KEYS.contains(key)) {
                additionalMetadata.put(key, entry.getValue());
            }
        }
        if (!additionalMetadata.isEmpty()) {
            resultItem.put("metadata", additionalMetadata);
        }

        // 7. 返回最终结果
        return resultItem;
    }

    /**
     * 复刻 _reject_top_level_entity_params
     * 拒绝顶层传入实体参数，强制使用 filters 传参
     */
    public static void rejectTopLevelEntityParams(Map<String, Object> kwargs, String methodName) {
        // 计算交集：找出非法的顶层参数
        Set<String> invalidKeys = null;
        if (kwargs != null && !kwargs.isEmpty()) {
            invalidKeys = ENTITY_PARAMS.stream().filter(kwargs::containsKey).collect(java.util.stream.Collectors.toSet());
        }

        // 存在非法参数 → 抛出异常（提示信息和Python完全一致）
        if (invalidKeys != null && !invalidKeys.isEmpty()) {
            throw new IllegalArgumentException(("Top-level entity parameters %s are not supported in %s(). " +
                    "Use filters={'user_id': '...'} instead.").formatted(invalidKeys, methodName));
        }
    }

    /**
     * 1:1 复刻 Python _validate_search_params
     * 校验搜索参数合法性
     *
     * @param threshold 相似度阈值（0~1之间）
     * @param topK      返回结果数量（非负整数）
     * @throws IllegalArgumentException 参数非法时抛出
     */
    private void validateSearchParams(Double threshold, Integer topK) {
        // 校验 threshold
        if (threshold != null) {
            // Java 中 Float 天然是数字，无需额外类型判断（对应 Python isinstance 判断）
            if (threshold < 0 || threshold > 1) {
                throw new IllegalArgumentException("Invalid threshold: %s. Must be between 0 and 1 (inclusive).".formatted(threshold));
            }
        }

        // 校验 top_k
        if (topK != null) {
            // Java 中 Integer 不会是布尔型，无需处理 Python bool 兼容问题
            if (topK < 0) {
                throw new IllegalArgumentException("Invalid top_k: %d. Must be a non-negative integer.".formatted(topK));
            }
        }
    }

    /**
     * get_all() 必须包含 user_id / agent_id / run_id 至少一个
     * <p>
     * 1:1 复刻 Python get_all() 核心方法
     *
     * @param filters 过滤条件
     * @param topK    返回数量 默认20
     * @param kwargs  额外参数（对应**kwargs）
     * @return 格式化记忆结果
     */
    public List<Map<String, Object>> getAll(Map<String, Object> filters, Integer topK, Map<String, Object> kwargs) {
        //  1. 拒绝顶层实体参数（必须用filters）
        rejectTopLevelEntityParams(kwargs, "get_all");

        //  2. 校验top_k参数
        validateSearchParams(null, topK);

        //  3. 初始化并处理filters
        Map<String, Object> effectiveFilters = filters == null ? new HashMap<>() : new HashMap<>(filters);

        // 校验并修剪实体ID
        if (effectiveFilters.containsKey("user_id")) {
            effectiveFilters.put("user_id", validateAndTrimEntityId(effectiveFilters.get("user_id"), "user_id"));
        }
        if (effectiveFilters.containsKey("agent_id")) {
            effectiveFilters.put("agent_id", validateAndTrimEntityId(effectiveFilters.get("agent_id"), "agent_id"));
        }
        if (effectiveFilters.containsKey("run_id")) {
            effectiveFilters.put("run_id", validateAndTrimEntityId(effectiveFilters.get("run_id"), "run_id"));
        }

        //  4. 校验必须包含至少一个实体ID
        boolean hasValidEntityId = effectiveFilters.keySet().stream().anyMatch(ENTITY_PARAMS::contains);

        if (!hasValidEntityId) {
            throw new IllegalArgumentException("filters must contain at least one of: user_id, agent_id, run_id. Example: filters={'user_id': 'u1'}"
            );
        }

        int limit = topK;

        // 5. 遥测处理（占位，和原版逻辑一致）
        // process_telemetry_filters + capture_event 可按需实现
        //  替换为正式版遥测处理
        Map<String, Object> telemetryResult = processTelemetryFilters(effectiveFilters);
        List<String> keys = (List<String>) telemetryResult.get("keys");
        Map<String, String> encodedIds = (Map<String, String>) telemetryResult.get("encoded_ids");

        // 捕获事件（和Python一致）
        telemetryService.captureEvent("mem0.get_all", null, Map.of("limit", limit, "keys", keys, "encoded_ids", encodedIds, "sync_type", "sync"));

        //6、7. 从向量库获取并格式化记忆、返回统一格式
        return getAllFromVectorStore(effectiveFilters, limit);
    }

    /**
     * 处理遥测过滤器
     * 返回：[过滤器key列表, MD5加密后的id字典]
     * 完全对应 Python 返回值 (list(filters.keys()), encoded_ids)
     */
    public static Map<String, Object> processTelemetryFilters(Map<String, Object> filters) {
        List<String> keys = new ArrayList<>();
        Map<String, String> encodedIds = new HashMap<>();

        // 空值处理
        if (filters == null || filters.isEmpty()) {
            return Map.of("keys", keys, "encoded_ids", encodedIds);
        }

        // 获取所有key
        keys.addAll(filters.keySet());

        // 对指定字段进行 MD5 加密
        for (String key : keys) {
            if (filters.containsKey(key)) {
                Object value = filters.get(key);
                if (StringUtils.isNotEmpty(value.toString())) {
                    encodedIds.put(key, md5Hex(value.toString()));
                }
            }
        }

        // 返回格式与 Python 完全一致
        return Map.of("keys", keys, "encoded_ids", encodedIds);
    }

    //_get_all_from_vector_store
    public List<Map<String, Object>> getAllFromVectorStore(Map<String, Object> filters, int limit) {
        // 1. 调用 vector_store.list 获取原始结果
        List<MemoryVectorEntry> memoriesResult = vectorStore.listWithoutVector(filters, limit);

        // 格式化记忆数据
        List<Map<String, Object>> formattedMemories = new ArrayList<>();
        for (MemoryVectorEntry mem : memoriesResult) {
            Map<String, Object> payload = Optional.ofNullable(mem.payload()).orElse(new HashMap<>());

            // 构建 MemoryItem (等价 Python model_dump(exclude={"score"}))
            Map<String, Object> memoryItemDict = new HashMap<>();
            memoryItemDict.put("id", mem.id());
            memoryItemDict.put("memory", payload.getOrDefault("data", ""));
            memoryItemDict.put("hash", payload.get("hash"));
            memoryItemDict.put("created_at", payload.get("created_at"));
            memoryItemDict.put("updated_at", payload.get("updated_at"));

            // 4. 提升关键字段到顶层
            for (String key : PROMOTED_PAYLOAD_KEYS) {
                if (payload.containsKey(key)) {
                    memoryItemDict.put(key, payload.get(key));
                }
            }

            // 5. 剩余非核心字段封装到 metadata
            Map<String, Object> additionalMetadata = new HashMap<>();
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                String key = entry.getKey();
                if (!CORE_AND_PROMOTED_KEYS.contains(key)) {
                    additionalMetadata.put(key, entry.getValue());
                }
            }
            if (!additionalMetadata.isEmpty()) {
                memoryItemDict.put("metadata", additionalMetadata);
            }

            formattedMemories.add(memoryItemDict);
        }

        return formattedMemories;
    }

    public List<MemoryHistoryResponse> history(String memoryId) {
        return sqLiteManager.history(memoryId);
    }

    /**
     * 删除所有匹配过滤条件的记忆
     *
     * @param userId  用户ID（可选）
     * @param agentId 代理ID（可选）
     * @param runId   运行ID（可选）
     */
    public void deleteAll(@Nullable String userId, @Nullable String agentId, @Nullable String runId) {
        // 1. 构建过滤条件（仅保留非空参数，对齐Python）
        Map<String, Object> filters = buildFilters(userId, agentId, runId);

        // 2. 校验：必须至少传入一个过滤条件（异常文案逐字匹配）
        if (filters.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one filter is required to delete all memories. If you want to delete all memories, use the `reset()` method."
            );
        }

        // 3. 遥测过滤器处理（复用之前实现的方法）
        Map<String, Object> telemetryResult = processTelemetryFilters(filters);
        List<String> keys = (List<String>) telemetryResult.get("keys");
        Map<String, String> encodedIds = (Map<String, String>) telemetryResult.get("encoded_ids");

        // 4. 上报遥测事件（对齐Python capture_event）
        telemetryService.captureEvent("mem0.delete_all", null, Map.of("keys", keys, "encoded_ids", encodedIds, "sync_type", "sync"));

        // 5. 查询向量库：获取匹配的记忆列表（Python取[0]，对齐底层返回结构）
        List<VectorEntry> memories = vectorStore.list(filters, null);

        // 6. 循环删除每一条记忆（调用内部_delete_memory方法）
        for (VectorEntry memory : memories) {
            deleteMemory(memory.id(), null);
        }

        // 7. 打印日志（对齐Python logger.info）
        log.info("Deleted {} memories", memories.size());
    }

    private String createMemory(String data, Map<String, Double[]> existingEmbeddings, Map<String, Object> metadata) {
        // 1. 获取向量
        Double[] embeddings;
        if (existingEmbeddings.containsKey(data)) {
            embeddings = existingEmbeddings.get(data);
        } else {
            embeddings = embeddingProvider.embed(data);
        }

        // 2. 生成 UUID
        String memoryId = UUID.randomUUID().toString();

        // 3. 深拷贝元数据
        Map<String, Object> newMetadata = new HashMap<>();
        if (metadata != null) {
            newMetadata.putAll(metadata);
        }

        // 4. 基础数据
        newMetadata.put("data", data);
        newMetadata.put("hash", md5Hex(data));

        // 5. 时间
        if (!newMetadata.containsKey("created_at")) {
            String createdAt = ZonedDateTime.now(ZoneOffset.UTC).toString();
            newMetadata.put("created_at", createdAt);
            newMetadata.put("updated_at", createdAt);
        } else {
            newMetadata.put("updated_at", newMetadata.get("created_at"));
        }

        // 6. BM25 分词
        newMetadata.put("text_lemmatized", lemmatizeForBm25(data));

        // 7. 插入向量库
        vectorStore.insert(List.of(), List.of(newMetadata), List.of(memoryId)
        );

        // 8. 写入历史
        sqLiteManager.addHistory(memoryId, null, data, "ADD");

        return memoryId;
    }


    private Map<String, Object> buildFilters(String userId, String agentId, String runId) {
        Map<String, Object> filters = new HashMap<>();
        if (userId != null) {
            filters.put("userId", userId);
        }
        if (agentId != null) {
            filters.put("agentId", agentId);
        }
        if (runId != null) {
            filters.put("runId", runId);
        }
        return filters;
    }


    private String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private MemoryResponse mapMemoryResponse(Memory memory) {
        return new MemoryResponse(
                memory.getId(), memory.getContent(),
//                memory.getHash(),
                "",//目前未单独一个字段存hash值
                memory.getMetadata(), null, memory.getCreatedAt(), memory.getUpdatedAt()
        );
    }

    private SearchResult mapSearchResult(com.mem0.core.vectorstore.SearchResult vectorResult) {
        Map<String, Object> payload = vectorResult.payload();
        UUID id;
        try {
            id = UUID.fromString(vectorResult.id());
        } catch (Exception e) {
            id = UUID.nameUUIDFromBytes(vectorResult.id().getBytes());
        }

        String content = payload != null
                ? (String) payload.getOrDefault("data", payload.getOrDefault("content", ""))
                : "";

        @SuppressWarnings("unchecked")
        Map<String, Object> meta = payload != null
                ? (Map<String, Object>) payload.getOrDefault("metadata", Map.of())
                : Map.of();

        return new SearchResult(
                id, content, vectorResult.score(), meta,
                payload != null ? (String) payload.get("userId") : null,
                payload != null ? (String) payload.get("agentId") : null,
                payload != null ? (String) payload.get("runId") : null
        );
    }


    public static FilterMetadataResult buildFiltersAndMetadata(String userId, String agentId,
                                                               String runId, String actorId,
                                                               Map<String, Object> inputMetadata,
                                                               Map<String, Object> inputFilters) {
        // 深拷贝（等价 deepcopy）
        Map<String, Object> baseMetadataTemplate = CopyUtil.deepCopy(inputMetadata);
        Map<String, Object> effectiveQueryFilters = CopyUtil.deepCopy(inputFilters);

        List<String> sessionIdsProvided = new ArrayList<>();

        // 校验并修剪 ID
        userId = validateAndTrimEntityId(userId, "user_id");
        agentId = validateAndTrimEntityId(agentId, "agent_id");
        runId = validateAndTrimEntityId(runId, "run_id");

        // 处理 user_id
        if (userId != null && !userId.isBlank()) {
            baseMetadataTemplate.put("user_id", userId);
            effectiveQueryFilters.put("user_id", userId);
            sessionIdsProvided.add("user_id");
        }

        // 处理 agent_id
        if (agentId != null && !agentId.isBlank()) {
            baseMetadataTemplate.put("agent_id", agentId);
            effectiveQueryFilters.put("agent_id", agentId);
            sessionIdsProvided.add("agent_id");
        }

        // 处理 run_id
        if (runId != null && !runId.isBlank()) {
            baseMetadataTemplate.put("run_id", runId);
            effectiveQueryFilters.put("run_id", runId);
            sessionIdsProvided.add("run_id");
        }

        // 至少一个 ID 必须存在
        if (sessionIdsProvided.isEmpty()) {
            Map<String, Object> details = new HashMap<>();
            details.put("user_id", userId);
            details.put("agent_id", agentId);
            details.put("run_id", runId);

            throw new Mem0ValidationError(
                    "At least one of 'user_id', 'agent_id', or 'run_id' must be provided.",
                    "VALIDATION_001",
                    details,
                    "Please provide at least one identifier to scope the memory operation."
            );
        }

        // 处理 actor_id
        String resolvedActorId = actorId != null ? actorId : (String) effectiveQueryFilters.get("actor_id");
        if (resolvedActorId != null && !resolvedActorId.isBlank()) {
            effectiveQueryFilters.put("actor_id", resolvedActorId);
        }

        return new FilterMetadataResult(baseMetadataTemplate, effectiveQueryFilters);
    }

    // 等价 Python _validate_and_trim_entity_id
    private static String validateAndTrimEntityId(String id, String fieldName) {
        if (id == null) {
            return null;
        }
        String trimmed = id.trim();
        if (trimmed.isEmpty()) {
            throw new Mem0ValidationError(
                    fieldName + " cannot be empty or whitespace.",
                    "VALIDATION_004",
                    Map.of("field", fieldName, "value", id),
                    "Provide a valid non-empty string."
            );
        }
        return trimmed;
    }

    /**
     * 复刻 _validate_and_trim_entity_id
     * 校验并修剪实体ID
     */
    private String validateAndTrimEntityId(Object idValue, String fieldName) {
        if (idValue == null) {
            throw new IllegalArgumentException("%s cannot be null".formatted(fieldName));
        }
        String trimmed = idValue.toString().trim();
        if (StringUtils.isEmpty(trimmed)) {
            throw new IllegalArgumentException("%s cannot be empty or blank".formatted(fieldName));
        }
        return trimmed;
    }
}
