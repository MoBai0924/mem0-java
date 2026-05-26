package com.mem0.core.exception;

/**
 * Dependency missing exception (DEPS_001).
 */
public class DependencyException extends Mem0Exception {
    public DependencyException(String message) { super("DEPS_001", message); }
    public DependencyException(String message, Throwable cause) { super("DEPS_001", message, cause); }
}
