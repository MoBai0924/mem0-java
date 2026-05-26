package com.mem0.core.vectorstore.impl;

import com.mem0.core.vectorstore.SearchResult;
import com.mem0.core.vectorstore.VectorEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FAISSVectorStoreTest {

    private FAISSVectorStore store;

    @BeforeEach
    void setUp() {
        store = new FAISSVectorStore("test_faiss_" + System.nanoTime());
    }

    @Test
    @DisplayName("insert and get - stores and retrieves vectors")
    void insertAndGet() {
        Double[] vector = {0.1f, 0.2f, 0.3f};
        Map<String, Object> payload = Map.of("content", "test fact");
        store.insert(List.of(vector), List.of(payload), List.of("id1"));

        Optional<VectorEntry> result = store.get("id1");
        assertTrue(result.isPresent());
        assertEquals("id1", result.get().id());
        assertEquals("test fact", result.get().payload().get("content"));
    }

    @Test
    @DisplayName("get - returns empty for non-existent ID")
    void getReturnsEmptyForMissing() {
        assertTrue(store.get("nonexistent").isEmpty());
    }

    @Test
    @DisplayName("search - returns results sorted by similarity")
    void searchReturnsSortedResults() {
        Double[] query = {1.0f, 0.0f, 0.0f};
        store.insert(List.of(new Double[]{1.0f, 0.0f, 0.0f}), List.of(Map.of("content", "similar")), List.of("id1"));
        store.insert(List.of(new Double[]{0.0f, 1.0f, 0.0f}), List.of(Map.of("content", "different")), List.of("id2"));

        List<SearchResult> results = store.search(query, 2, null);
        assertEquals(2, results.size());
        assertTrue(results.get(0).score() >= results.get(1).score());
        assertEquals("id1", results.get(0).id());
    }

    @Test
    @DisplayName("search - applies filters")
    void searchAppliesFilters() {
        Double[] query = {1.0f, 0.0f};
        store.insert(List.of(new Double[]{1.0f, 0.0f}), List.of(Map.of("userId", "user1")), List.of("id1"));
        store.insert(List.of(new Double[]{0.9f, 0.1f}), List.of(Map.of("userId", "user2")), List.of("id2"));

        List<SearchResult> results = store.search(query, 10, Map.of("userId", "user1"));
        assertEquals(1, results.size());
        assertEquals("id1", results.get(0).id());
    }

    @Test
    @DisplayName("update - modifies existing vector")
    void updateModifiesVector() {
        store.insert(List.of(new Double[]{0.1f, 0.2f}), List.of(Map.of("content", "old")), List.of("id1"));
        store.update("id1", new Double[]{0.9f, 0.1f}, Map.of("content", "new"));

        Optional<VectorEntry> result = store.get("id1");
        assertTrue(result.isPresent());
        assertEquals("new", result.get().payload().get("content"));
    }

    @Test
    @DisplayName("delete - removes vector")
    void deleteRemovesVector() {
        store.insert(List.of(new Double[]{0.1f, 0.2f}), List.of(Map.of()), List.of("id1"));
        store.delete("id1");
        assertTrue(store.get("id1").isEmpty());
    }

    @Test
    @DisplayName("count - returns correct count")
    void countReturnsCorrectCount() {
        assertEquals(0, store.count());
        store.insert(List.of(new Double[]{0.1f}), List.of(Map.of()), List.of("id1"));
        assertEquals(1, store.count());
        store.insert(List.of(new Double[]{0.2f}), List.of(Map.of()), List.of("id2"));
        assertEquals(2, store.count());
    }

    @Test
    @DisplayName("list - returns entries with limit")
    void listReturnsEntriesWithLimit() {
        store.insert(List.of(new Double[]{0.1f}), List.of(Map.of()), List.of("id1"));
        store.insert(List.of(new Double[]{0.2f}), List.of(Map.of()), List.of("id2"));

        List<VectorEntry> entries = store.list(null, 1);
        assertEquals(1, entries.size());
    }

    @Test
    @DisplayName("reset - clears all vectors")
    void resetClearsAll() {
        store.insert(List.of(new Double[]{0.1f}), List.of(Map.of()), List.of("id1"));
        store.reset();
        assertEquals(0, store.count());
    }

    @Test
    @DisplayName("getName - returns faiss")
    void getNameReturnsFaiss() {
        assertEquals("faiss", store.getName());
    }
}
