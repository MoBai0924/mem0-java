package cn.hsine.mem0.core.exception;

/**
 * Database operation exception (DB_001).
 */
public class DatabaseException extends Mem0Exception {
    public DatabaseException(String message) { super("DB_001", message); }
    public DatabaseException(String message, Throwable cause) { super("DB_001", message, cause); }
}
