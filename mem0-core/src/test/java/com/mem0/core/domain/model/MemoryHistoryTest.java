package com.mem0.core.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MemoryHistoryTest {

    @Test
    @DisplayName("addEvent - creates ADD history entry")
    void addEventCreatesEntry() {
        UUID memoryId = UUID.randomUUID();
        MemoryHistory history = MemoryHistory.addEvent(memoryId.toString(), "User likes coffee");

        assertEquals(memoryId, history.getMemoryId());
        assertEquals(MemoryEvent.ADD, history.getEvent());
        assertNull(history.getOldMemory());
        assertEquals("User likes coffee", history.getNewMemory());
    }

    @Test
    @DisplayName("updateEvent - creates UPDATE history entry")
    void updateEventCreatesEntry() {
        UUID memoryId = UUID.randomUUID();
        MemoryHistory history = MemoryHistory.updateEvent(memoryId.toString(), "old content", "new content");

        assertEquals(memoryId, history.getMemoryId());
        assertEquals(MemoryEvent.UPDATE, history.getEvent());
        assertEquals("old content", history.getOldMemory());
        assertEquals("new content", history.getNewMemory());
    }

    @Test
    @DisplayName("deleteEvent - creates DELETE history entry")
    void deleteEventCreatesEntry() {
        UUID memoryId = UUID.randomUUID();
        MemoryHistory history = MemoryHistory.deleteEvent(memoryId.toString(), "deleted content");

        assertEquals(memoryId, history.getMemoryId());
        assertEquals(MemoryEvent.DELETE, history.getEvent());
        assertEquals("deleted content", history.getOldMemory());
    }
}
