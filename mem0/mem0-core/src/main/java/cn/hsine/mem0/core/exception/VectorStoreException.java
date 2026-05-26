package cn.hsine.mem0.core.exception;

/**
 * Exception thrown for vector store errors.
 *
 * @author MoBai

 */
public class VectorStoreException extends Mem0Exception {

    /**
     * Creates an exception with a message.
     *
     * @param message the message
     */
    public VectorStoreException(String message) {
        super("VECTOR_STORE_ERROR", message);
    }

    /**
     * Creates an exception with a message and cause.
     *
     * @param message the message
     * @param cause the cause
     */
    public VectorStoreException(String message, Throwable cause) {
        super("VECTOR_STORE_ERROR", message, cause);
    }
}
