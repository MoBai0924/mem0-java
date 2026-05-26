package cn.hsine.mem0.core.exception;

/**
 * Exception thrown for embedding errors.
 *
 * @author MoBai

 */
public class EmbeddingException extends Mem0Exception {

    /**
     * Creates an exception with a message.
     *
     * @param message the message
     */
    public EmbeddingException(String message) {
        super("EMBEDDING_ERROR", message);
    }

    /**
     * Creates an exception with a message and cause.
     *
     * @param message the message
     * @param cause the cause
     */
    public EmbeddingException(String message, Throwable cause) {
        super("EMBEDDING_ERROR", message, cause);
    }
}
