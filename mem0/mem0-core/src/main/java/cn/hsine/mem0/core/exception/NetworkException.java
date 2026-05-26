package cn.hsine.mem0.core.exception;

/**
 * Network connectivity exception (NET_001).
 */
public class NetworkException extends Mem0Exception {
    public NetworkException(String message) { super("NET_001", message); }
    public NetworkException(String message, Throwable cause) { super("NET_001", message, cause); }
}
