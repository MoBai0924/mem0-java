package cn.hsine.mem0.core.service;

import cn.hsine.mem0.core.vectorstore.SearchResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hsine.mem0.core.config.MemoryConfig;
import cn.hsine.mem0.core.domain.model.Memory;
import cn.hsine.mem0.core.domain.model.MemoryHistory;
import cn.hsine.mem0.core.dto.Message;
import cn.hsine.mem0.core.dto.request.AddMemoryRequest;
import cn.hsine.mem0.core.dto.request.SearchFilters;
import cn.hsine.mem0.core.dto.request.SearchRequest;
import cn.hsine.mem0.core.dto.response.MemoryHistoryResponse;
import cn.hsine.mem0.core.embedding.EmbeddingProvider;
import cn.hsine.mem0.core.entityextractor.impl.LlmEntityExtractor;
import cn.hsine.mem0.core.entitystore.EntityStore;
import cn.hsine.mem0.core.exception.MemoryNotFoundException;
import cn.hsine.mem0.core.llm.LLMProvider;
import cn.hsine.mem0.core.llm.LLMProviderConfig;
import cn.hsine.mem0.core.repository.HistoryRepository;
import cn.hsine.mem0.core.repository.MemoryRepository;
import cn.hsine.mem0.core.repository.MessageRepository;
import cn.hsine.mem0.core.reranker.Reranker;
import cn.hsine.mem0.core.vectorstore.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemoryServiceTest {

    @Mock
    private MemoryRepository memoryRepository;
    @Mock
    private HistoryRepository historyRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private SQLiteManager sqLiteManager;
    @Mock
    private VectorStore vectorStore;
    @Mock
    private EmbeddingProvider embeddingProvider;
    @Mock
    private LLMProvider llmProvider;
    @Mock
    private LLMProviderConfig llmProviderConfig;
    @Mock
    private MemoryConfig memoryConfig;
    @Mock
    private LlmEntityExtractor llmEntityExtractor;
    @Mock
    private EntityStore entityStore;
    @Mock
    private Reranker reranker;
    @Mock
    private TelemetryService telemetryService;
    @Mock
    private MemoryService memoryService;

    @BeforeEach
    void setUp() {
        memoryService = new MemoryService(
                sqLiteManager,
                vectorStore,
                embeddingProvider,
                llmProvider,
                llmProviderConfig,
                memoryConfig,
                llmEntityExtractor,
                entityStore,
                reranker,
                telemetryService
        );
    }

    @Test
    @DisplayName("add - returns empty list when no memories extracted")
    void addReturnsEmptyWhenNoMemories() throws JsonProcessingException {
        when(llmProvider.generateResponse(anyList(), any())).thenReturn("{\"memory\":[]}");
        when(embeddingProvider.embed(anyString())).thenReturn(new Double[]{0.1, 0.2});
        when(vectorStore.search(any(Double[].class), anyInt(), anyMap())).thenReturn(List.of());

        AddMemoryRequest request = new AddMemoryRequest(
                List.of(Message.user("Hello")), "user1", null, null, null, null, null
        );

        List<Map<String, Object>> result = memoryService.add(request);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("add - creates new memories via V3 pipeline")
    void addCreatesNewMemoriesViaV3() throws Exception {
        UUID memoryId = UUID.randomUUID();
        when(embeddingProvider.embed(anyString())).thenReturn(new Double[]{0.1, 0.2});
        when(embeddingProvider.embedBatch(anyList())).thenReturn(List.<Double[]>of(new Double[]{0.1, 0.2}));
        when(vectorStore.search(any(Double[].class), anyInt(), anyMap())).thenReturn(List.of());
        when(memoryRepository.findByFilters(any(), any(), any())).thenReturn(List.of());
        when(memoryRepository.findByHash(anyString())).thenReturn(null);
        when(memoryRepository.save(any(Memory.class))).thenReturn(1);
        when(llmProvider.generateResponse(anyList(), any()))
                .thenReturn("{\"memory\":[{\"text\":\"User likes coffee\",\"attributed_to\":\"user\"}]}");

        AddMemoryRequest request = new AddMemoryRequest(
                List.of(Message.user("I like coffee")), "user1", null, null, null, null, null
        );

        List<Map<String, Object>> result = memoryService.add(request);
        assertFalse(result.isEmpty());
        verify(vectorStore).insert(any(Double[].class), anyMap(), anyString());
        verify(historyRepository).insert(any(MemoryHistory.class));
    }

    @Test
    @DisplayName("add - falls back to legacy FactExtractor when LLM fails")
    void addFallsBackToLegacyWhenLLMFails() throws Exception {
        UUID memoryId = UUID.randomUUID();
        when(llmProvider.generateResponse(anyList(), any())).thenThrow(new RuntimeException("LLM error"));
        when(embeddingProvider.embed(anyString())).thenReturn(new Double[]{0.1, 0.2});
        when(embeddingProvider.embedBatch(anyList())).thenReturn(List.<Double[]>of(new Double[]{0.1, 0.2}));
        when(vectorStore.search(any(Double[].class), anyInt(), anyMap())).thenReturn(List.of());
        when(memoryRepository.findByFilters(any(), any(), any())).thenReturn(List.of());
        when(memoryRepository.findByHash(anyString())).thenReturn(null);
        when(memoryRepository.save(any(Memory.class))).thenReturn(1);

        AddMemoryRequest request = new AddMemoryRequest(
                List.of(Message.user("I like coffee")), "user1", null, null, null, null, null
        );

        List<Map<String, Object>> result = memoryService.add(request);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("search - returns results above threshold via hybrid retrieval")
    void searchReturnsResultsAboveThreshold() {
        // Create service without reranker for simple search test
        MemoryService searchService = new MemoryService(
                sqLiteManager,
                vectorStore,
                embeddingProvider,
                llmProvider,
                llmProviderConfig,
                memoryConfig,
                llmEntityExtractor,
                entityStore,
                reranker,
                telemetryService
        );

        UUID id = UUID.randomUUID();
        when(embeddingProvider.embed(anyString())).thenReturn(new Double[]{0.1, 0.2});
        when(vectorStore.search(any(Double[].class), anyInt(), anyMap()))
                .thenReturn(List.of(
                        new SearchResult(id.toString(), 0.95, null, Map.of("content", "fact1", "userId", "u1"))
                ));

        SearchRequest request = new SearchRequest("coffee", new SearchFilters("user1", null, null), 10, 0.5, false);
        String query = request.query();
        Integer topK = request.topK();
        SearchFilters filters = request.filters();
        Double threshold = request.threshold();
        Boolean rerank = request.rerank();

        // 1. 模型转Map（等价 search_req.model_dump()）
        Map<String, Object> requestMap = new ObjectMapper().convertValue(request, new TypeReference<>() {
        });

        Map<String, Object> params = requestMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !"query".equals(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 3. 调用搜索方法（等价 get_memory_instance().search(query=search_req.query, **params)）
        Map<String, List<Map<String, Object>>> results = memoryService.search(query, topK, filters, threshold, rerank, params);
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("search - filters results below threshold")
    void searchFiltersBelowThreshold() {
        // Create service without reranker for simple search test
        MemoryService searchService = new MemoryService(
                sqLiteManager,
                vectorStore,
                embeddingProvider,
                llmProvider,
                llmProviderConfig,
                memoryConfig,
                llmEntityExtractor,
                entityStore,
                reranker,
                telemetryService
        );

        UUID id = UUID.randomUUID();
        when(embeddingProvider.embed(anyString())).thenReturn(new Double[]{0.1, 0.2});
        when(vectorStore.search(any(Double[].class), anyInt(), anyMap()))
                .thenReturn(List.of(
                        new SearchResult(id.toString(), 0.3, null, Map.of("content", "fact1"))
                ));
        SearchRequest request = new SearchRequest("coffee", new SearchFilters("user1", null, null), 10, 0.5, false);
        String query = request.query();
        Integer topK = request.topK();
        SearchFilters filters = request.filters();
        Double threshold = request.threshold();
        Boolean rerank = request.rerank();

        // 1. 模型转Map（等价 search_req.model_dump()）
        Map<String, Object> requestMap = new ObjectMapper().convertValue(request, new TypeReference<>() {
        });

        Map<String, Object> params = requestMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !"query".equals(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 3. 调用搜索方法（等价 get_memory_instance().search(query=search_req.query, **params)）
        Map<String, List<Map<String, Object>>> results = memoryService.search(query, topK, filters, threshold, rerank, params);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("get - throws MemoryNotFoundException for missing ID")
    void getThrowsForMissingId() {
        UUID id = UUID.randomUUID();
        when(memoryRepository.findById(id.toString())).thenReturn(null);

        assertThrows(MemoryNotFoundException.class, () -> memoryService.get(id.toString()));
    }

    @Test
    @DisplayName("delete - throws MemoryNotFoundException for missing ID")
    void deleteThrowsForMissingId() {
        UUID id = UUID.randomUUID();
        when(memoryRepository.findById(id.toString())).thenReturn(null);

        assertThrows(MemoryNotFoundException.class, () -> memoryService.deleteMemory(id.toString(), null));
    }

    @Test
    @DisplayName("delete - removes from vector store and database")
    void deleteRemovesFromAllStores() throws Exception {
        UUID id = UUID.randomUUID();
        Memory memory = new Memory("content", "hash", "user1", null, null, null);
        memory.setId(id.toString());
        when(memoryRepository.findById(id.toString())).thenReturn(memory);

        memoryService.deleteMemory(id.toString(), null);

        verify(vectorStore).delete(id.toString());
        verify(memoryRepository).delete(memory);
        verify(historyRepository).insert(any(MemoryHistory.class));
    }

    @Test
    @DisplayName("getAll - returns memories with limit")
    void getAllReturnsMemoriesWithLimit() {
        when(memoryRepository.findByFilters("user1", null, null))
                .thenReturn(List.of(
                        new Memory("fact1", "h1", "user1", null, null, null),
                        new Memory("fact2", "h2", "user1", null, null, null)
                ));
        Map<String, Object> filters = new HashMap<>();
        filters.put("user_id", "user1");
        List<Map<String, Object>> result = memoryService.getAll(filters, 20, null);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("history - returns history entries")
    void historyReturnsEntries() {
        UUID memoryId = UUID.randomUUID();
        when(historyRepository.findByMemoryIdOrderByCreatedAtAsc(memoryId.toString()))
                .thenReturn(List.of());

        List<MemoryHistoryResponse> result = memoryService.history(memoryId.toString());
        assertTrue(result.isEmpty());
        verify(historyRepository).findByMemoryIdOrderByCreatedAtAsc(memoryId.toString());
    }

    @Test
    @DisplayName("deleteAll - deletes matching memories from all stores")
    void deleteAllRemovesFromAllStores() throws Exception {
        UUID id = UUID.randomUUID();
        Memory m1 = new Memory("fact1", "h1", "user1", null, null, null);
        m1.setId(id.toString());
        when(memoryRepository.findByFilters("user1", null, null)).thenReturn(List.of(m1));

        memoryService.deleteAll("user1", null, null);

        verify(vectorStore).delete(id.toString());
        verify(memoryRepository).delete(m1);
    }
}
