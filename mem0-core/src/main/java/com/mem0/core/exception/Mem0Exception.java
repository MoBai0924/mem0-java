package com.mem0.core.exception;

import java.util.Map;

/**
 * Base exception for all mem0 exceptions, ported from Python mem0/exceptions.py.
 * Includes error_code, details, suggestion, and debug_info fields.
 *
 * @author MoBai

 */
public abstract class Mem0Exception extends RuntimeException {

    private final String errorCode;
    private final Map<String, Object> details;
    private final String suggestion;
    private final Map<String, Object> debugInfo;

    protected Mem0Exception(String message) {
        this(null, message, null, null, null, null);
    }

    protected Mem0Exception(String message, Throwable cause) {
        this(null, message, cause, null, null, null);
    }

    protected Mem0Exception(String errorCode, String message) {
        this(errorCode, message, null, null, null, null);
    }

    protected Mem0Exception(String errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, null, null, null);
    }

    protected Mem0Exception(String errorCode, String message, Throwable cause,
                            Map<String, Object> details, String suggestion, Map<String, Object> debugInfo) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : getClass().getSimpleName().replace("Exception", "").toUpperCase();
        this.details = details;
        this.suggestion = suggestion;
        this.debugInfo = debugInfo;
    }

    public String getErrorCode() { return errorCode; }
    public Map<String, Object> getDetails() { return details; }
    public String getSuggestion() { return suggestion; }
    public Map<String, Object> getDebugInfo() { return debugInfo; }
}
