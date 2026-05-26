package cn.hsine.mem0.core.service;

import cn.hsine.mem0.core.dto.response.SearchResult;
import cn.hsine.mem0.core.embedding.EmbeddingProvider;
import cn.hsine.mem0.core.entityextractor.EntityExtractor;
import cn.hsine.mem0.core.vectorstore.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HybridSearchServiceTest {

    @Mock private VectorStore vectorStore;
    @Mock private EmbeddingProvider embeddingProvider;

    private HybridSearchService hybridSearchService;

    @BeforeEach
    void setUp() {
        hybridSearchService = new HybridSearchService(vectorStore, embeddingProvider, new EntityExtractor());
    }

    @Test
    @DisplayName("search - returns results from semantic search")
    void searchReturnsSemanticResults() {
        String id = UUID.randomUUID().toString();
        when(embeddingProvider.embed(anyString())).thenReturn(new Double[]{0.1f, 0.2f});
        when(vectorStore.search(any(Double[].class), anyInt(), any()))
            .thenReturn(List.of(
                new cn.hsine.mem0.core.vectorstore.SearchResult(id, 0.9, null, Map.of("content", "fact1"))
            ));
        when(vectorStore.keywordSearch(anyString(), anyInt(), any()))
            .thenThrow(new UnsupportedOperationException("Not supported"));

        List<SearchResult> results = hybridSearchService.search(
            "query", 10, null, 0.0
        );

        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("search - returns empty when no results")
    void searchReturnsEmptyWhenNoResults() {
        when(embeddingProvider.embed(anyString())).thenReturn(new Double[]{0.1f, 0.2f});
        when(vectorStore.search(any(Double[].class), anyInt(), any()))
            .thenReturn(List.of());
        when(vectorStore.keywordSearch(anyString(), anyInt(), any()))
            .thenThrow(new UnsupportedOperationException("Not supported"));

        List<SearchResult> results = hybridSearchService.search(
            "query", 10, null, 0.0
        );

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("search - applies filters")
    void searchAppliesFilters() {
        Map<String, Object> filters = Map.of("userId", "user1");
        when(embeddingProvider.embed(anyString())).thenReturn(new Double[]{0.1f});
        when(vectorStore.search(any(Double[].class), anyInt(), any()))
            .thenReturn(List.of());
        when(vectorStore.keywordSearch(anyString(), anyInt(), any()))
            .thenThrow(new UnsupportedOperationException("Not supported"));

        hybridSearchService.search("query", 10, filters, 0.0);
        verify(vectorStore).search(any(Double[].class), anyInt(), any());
    }
}
