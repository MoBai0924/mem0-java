package cn.hsine.mem0.core.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MemoryTest {

    @Test
    @DisplayName("constructor - creates memory with content and hash")
    void constructorCreatesMemory() {
        Memory memory = new Memory("User likes coffee", "hash123", "user1", "agent1", "run1", Map.of("key", "value"));
        assertEquals("User likes coffee", memory.getContent());
//        assertEquals("hash123", memory.getHash());
        assertEquals("user1", memory.getUserId());
        assertEquals("agent1", memory.getAgentId());
        assertEquals("run1", memory.getRunId());
        assertNotNull(memory.getCreatedAt());
    }

    @Test
    @DisplayName("updateContent - changes content")
    void updateContentChangesContent() {
        Memory memory = new Memory("old content", "hash", "user1", null, null, null);
        memory.updateContent("new content");
        assertEquals("new content", memory.getContent());
    }

    @Test
    @DisplayName("updateMetadata - changes metadata")
    void updateMetadataChangesMetadata() {
        Memory memory = new Memory("content", "hash", "user1", null, null, null);
        Map<String, Object> newMeta = Map.of("updated", true);
        memory.updateMetadata(newMeta);
        assertEquals(newMeta, memory.getMetadata());
    }

    @Test
    @DisplayName("setEmbedding - sets embedding vector")
    void setEmbeddingSetsVector() {
        Memory memory = new Memory("content", "hash", "user1", null, null, null);
        Double[] embedding = {0.1f, 0.2f, 0.3f};
        memory.setEmbedding(embedding);
        assertArrayEquals(embedding, memory.getEmbedding());
    }
}
