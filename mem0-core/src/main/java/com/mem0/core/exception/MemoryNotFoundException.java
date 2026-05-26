package com.mem0.core.exception;

import java.util.UUID;

/**
 * Exception thrown when a memory is not found.
 *
 * @author MoBai

 */
public class MemoryNotFoundException extends Mem0Exception {

    /**
     * Creates an exception for a memory not found by ID.
     *
     * @param memoryId the memory ID
     */
    public MemoryNotFoundException(UUID memoryId) {
        super("MEMORY_NOT_FOUND", "Memory with ID " + memoryId + " not found");
    }

    /**
     * Creates an exception with a custom message.
     *
     * @param message the message
     */
    public MemoryNotFoundException(String message) {
        super("MEMORY_NOT_FOUND", message);
    }
}
