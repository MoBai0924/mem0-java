package com.mem0.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mem0.core.dto.request.AddMemoryRequest;
import com.mem0.core.dto.request.SearchFilters;
import com.mem0.core.dto.request.SearchRequest;
import com.mem0.core.dto.request.UpdateMemoryRequest;
import com.mem0.core.dto.response.MemoryHistoryResponse;
import com.mem0.core.service.MemoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for memory operations.
 *
 * @author MoBai

 */
@RestController
@RequestMapping("/memories")
public class MemoryController {

    private final static Integer ALL_MEMORIES_LIMIT = 1000;

    private final MemoryService memoryService;

    private final static ObjectMapper objectMapper = new ObjectMapper();

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * Creates memories from messages.
     */
    @PostMapping("/memories")
    public ResponseEntity<List<Map<String, Object>>> addMemories(@RequestBody @Valid AddMemoryRequest request) throws JsonProcessingException {
        List<Map<String, Object>> memories = memoryService.add(request);
        return ResponseEntity.ok(memories);
    }

    /**
     * Lists memories with optional filters.
     */
    @GetMapping("/memories")
    @Tag(name = "getMemories", description = "The result does not contain vectors.")
    public ResponseEntity<Map<String, Object>> getMemories(@RequestParam(name = "userId", required = false) String userId,
                                                           @RequestParam(name = "runId", required = false) String runId,
                                                           @RequestParam(name = "agentId", required = false) String agentId,
                                                           @RequestParam(name = "topK", required = false) Integer topK) {
        try {
            // 如果没有传入任何 ID → 返回全部记忆
            if (StringUtils.isAllEmpty(userId, runId, agentId)) {
                return ResponseEntity.ok(Map.of("results", memoryService.getAllFromVectorStore(null, ALL_MEMORIES_LIMIT)));
            }

            // 构建 filters（只保留非 null 字段）
            Map<String, Object> filters = new HashMap<>();
            if (userId != null) {
                filters.put("user_id", userId);
            }
            if (runId != null) {
                filters.put("run_id", runId);
            }
            if (agentId != null) {
                filters.put("agent_id", agentId);
            }

            if (null == topK || topK <= 0) {
                topK = 20;
            }
            // 调用 get_all
            return ResponseEntity.ok(Map.of("results", memoryService.getAll(filters, topK, null)));
        } catch (Exception e) {
            throw new RuntimeException("Upstream error: %s".formatted(e.getMessage()));
        }
    }

    /**
     * Gets a specific memory by ID.
     */
    @GetMapping("/memories/{memoryId}")
    public ResponseEntity<Map<String, Object>> getMemory(@PathVariable(name = "memoryId") String memoryId) {
        Map<String, Object> memory = memoryService.get(memoryId);
        return ResponseEntity.ok(memory);
    }

    /**
     * Updates a memory.
     */
    @PutMapping("/memories/{memoryId}")
    public ResponseEntity<Map<String, String>> updateMemory(@PathVariable(name = "memoryId") String memoryId,
                                                            @RequestBody @Valid UpdateMemoryRequest request) {
        Map<String, String> memory = memoryService.update(memoryId, request);
        return ResponseEntity.ok(memory);
    }

    /**
     * Gets the history of changes for a memory.
     */
    @GetMapping("/memories/{memoryId}/history")
    public ResponseEntity<List<MemoryHistoryResponse>> getMemoryHistory(@PathVariable(name = "memoryId") String memoryId) {
        List<MemoryHistoryResponse> history = memoryService.history(memoryId);
        return ResponseEntity.ok(history);
    }

    /**
     * Searches memories by semantic similarity.
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> searchMemories(@RequestBody @Valid SearchRequest request) {
        try {
            // 1. 模型转Map（等价 search_req.model_dump()）
            Map<String, Object> requestMap = objectMapper.convertValue(request, new TypeReference<>() {});

            // 2. 过滤参数：值非空 + 排除 key="query"（完全对齐Python推导式）
            Map<String, Object> params = requestMap.entrySet().stream()
                    .filter(entry -> entry.getValue() != null && !"query".equals(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            String query = request.query();
            Integer topK = request.topK();
            SearchFilters filters = request.filters();
            Double threshold = request.threshold();
            Boolean rerank = request.rerank();

            // 3. 调用搜索方法（等价 get_memory_instance().search(query=search_req.query, **params)）
            Map<String, List<Map<String, Object>>> results = memoryService.search(query, topK, filters, threshold, rerank, params);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            // 4. 异常捕获，抛上游异常（等价 raise upstream_error()）
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Deletes a memory.
     */
    @DeleteMapping("/memories/{memoryId}")
    public ResponseEntity<Void> deleteMemory(@PathVariable(name = "memoryId") String memoryId) {
        memoryService.deleteMemory(memoryId, null);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes all memories matching the given filters.
     */
    @DeleteMapping("/memories")
    public ResponseEntity<String> deleteAllMemories(@RequestParam(name = "userId", required = false) String userId,
                                                    @RequestParam(name = "agentId", required = false) String agentId,
                                                    @RequestParam(name = "runId", required = false) String runId) {
        try {
            // 1. 核心校验：至少传入一个标识符（严格对齐Python）
            if (userId == null && runId == null && agentId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one identifier is required.");
            }

            // 3. 调用删除方法（对应 get_memory_instance().delete_all(**params)）
            memoryService.deleteAll(userId, agentId, runId);

            // 4. 返回成功响应（对应MessageResponse）
            return ResponseEntity.ok("All relevant memories deleted");

        } catch (Exception e) {
            // 5. 异常捕获，抛上游异常（对应 raise upstream_error()）
            throw new RuntimeException();
        }
    }
}
