package com.mem0.core.exception;

/**
 * Memory quota exceeded exception (QUOTA_001).
 */
public class MemoryQuotaExceededException extends Mem0Exception {
    public MemoryQuotaExceededException(String message) { super("QUOTA_001", message); }
    public MemoryQuotaExceededException(String message, Throwable cause) { super("QUOTA_001", message, cause); }
}
