package com.mem0.core.service;

import com.mem0.core.dto.Message;
import com.mem0.core.llm.LLMConfig;
import com.mem0.core.llm.LLMProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FactExtractorTest {

    @Mock
    private LLMProvider llmProvider;

    private FactExtractor factExtractor;

    @BeforeEach
    void setUp() {
        factExtractor = new FactExtractor(llmProvider);
    }

    @Test
    @DisplayName("extract - returns facts from JSON array response")
    void extractReturnsFactsFromJsonArray() {
        when(llmProvider.generateResponse(anyList(), any(LLMConfig.class)))
            .thenReturn("[\"User prefers dark mode\", \"User works at Acme Corp\"]");

        List<String> facts = factExtractor.extract(
            List.of(Message.user("I prefer dark mode and work at Acme Corp")),
            null
        );

        assertEquals(2, facts.size());
        assertTrue(facts.contains("User prefers dark mode"));
        assertTrue(facts.contains("User works at Acme Corp"));
    }

    @Test
    @DisplayName("extract - handles response with surrounding text")
    void extractHandlesResponseWithSurroundingText() {
        when(llmProvider.generateResponse(anyList(), any(LLMConfig.class)))
            .thenReturn("Here are the facts:\n[\"User has a cat\"]\nLet me know if you need more.");

        List<String> facts = factExtractor.extract(
            List.of(Message.user("I have a cat named Whiskers")),
            null
        );

        assertEquals(1, facts.size());
        assertEquals("User has a cat", facts.get(0));
    }

    @Test
    @DisplayName("extract - deduplicates facts")
    void extractDeduplicatesFacts() {
        when(llmProvider.generateResponse(anyList(), any(LLMConfig.class)))
            .thenReturn("[\"User likes coffee\", \"user likes coffee\"]");

        List<String> facts = factExtractor.extract(
            List.of(Message.user("I like coffee")),
            null
        );

        assertEquals(1, facts.size());
    }

    @Test
    @DisplayName("extract - falls back to line-based parsing on invalid JSON")
    void extractFallsBackToLineParsing() {
        when(llmProvider.generateResponse(anyList(), any(LLMConfig.class)))
            .thenReturn("- User prefers dark mode\n- User works remotely");

        List<String> facts = factExtractor.extract(
            List.of(Message.user("I prefer dark mode and work remotely")),
            null
        );

        assertFalse(facts.isEmpty());
    }

    @Test
    @DisplayName("extract - includes custom instructions in prompt")
    void extractIncludesCustomInstructions() {
        when(llmProvider.generateResponse(anyList(), any(LLMConfig.class)))
            .thenReturn("[\"Custom fact\"]");

        factExtractor.extract(
            List.of(Message.user("Hello")),
            "Focus on preferences only"
        );

        verify(llmProvider).generateResponse(anyList(), any(LLMConfig.class));
    }

    @Test
    @DisplayName("determineUpdates - parses new and update facts")
    void determineUpdatesParsesNewAndUpdateFacts() {
        when(llmProvider.generateResponse(anyList(), any(LLMConfig.class)))
            .thenReturn("{\"new\":[\"User now lives in Berlin\"],\"update\":[{\"old\":\"User lives in Paris\",\"new\":\"User lives in Berlin\"}]}");

        FactExtractor.UpdateResult result = factExtractor.determineUpdates(
            List.of("User lives in Paris"),
            List.of("User now lives in Berlin"),
            null
        );

        assertEquals(1, result.newFacts().size());
        assertEquals("User now lives in Berlin", result.newFacts().get(0));
        assertEquals(1, result.updates().size());
        assertEquals("User lives in Paris", result.updates().get(0).oldMemory());
        assertEquals("User lives in Berlin", result.updates().get(0).newMemory());
    }

    @Test
    @DisplayName("determineUpdates - handles empty existing memories")
    void determineUpdatesHandlesEmptyExisting() {
        when(llmProvider.generateResponse(anyList(), any(LLMConfig.class)))
            .thenReturn("{\"new\":[\"User likes tea\"],\"update\":[]}");

        FactExtractor.UpdateResult result = factExtractor.determineUpdates(
            List.of(),
            List.of("User likes tea"),
            null
        );

        assertEquals(1, result.newFacts().size());
        assertTrue(result.updates().isEmpty());
    }

    @Test
    @DisplayName("determineUpdates - falls back on invalid JSON")
    void determineUpdatesFallsBackOnInvalidJson() {
        when(llmProvider.generateResponse(anyList(), any(LLMConfig.class)))
            .thenReturn("- User likes tea\n- User has a dog");

        FactExtractor.UpdateResult result = factExtractor.determineUpdates(
            List.of(),
            List.of("User likes tea", "User has a dog"),
            null
        );

        assertFalse(result.newFacts().isEmpty());
        assertTrue(result.updates().isEmpty());
    }
}
