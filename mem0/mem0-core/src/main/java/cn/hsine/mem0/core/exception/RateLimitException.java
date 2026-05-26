package cn.hsine.mem0.core.exception;

/**
 * Rate limit exceeded exception (RATE_001).
 */
public class RateLimitException extends Mem0Exception {
    public RateLimitException(String message) { super("RATE_001", message); }
    public RateLimitException(String message, Throwable cause) { super("RATE_001", message, cause); }
}
