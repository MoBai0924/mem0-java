package com.mem0.core.exception;

/**
 * Authentication failure exception (AUTH_001).
 */
public class AuthenticationException extends Mem0Exception {
    public AuthenticationException(String message) { super("AUTH_001", message); }
    public AuthenticationException(String message, Throwable cause) { super("AUTH_001", message, cause); }
}
