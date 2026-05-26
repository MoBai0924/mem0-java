package com.mem0.core.domain.model;

/**
 * Enumeration of memory event types for history tracking.
 *
 * @author MoBai

 */
public enum MemoryEvent {
    /**
     * Event triggered when a new memory is created.
     */
    ADD,

    /**
     * Event triggered when an existing memory is updated.
     */
    UPDATE,

    /**
     * Event triggered when a memory is deleted.
     */
    DELETE
}
