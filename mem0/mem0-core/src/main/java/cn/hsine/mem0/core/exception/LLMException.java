package cn.hsine.mem0.core.exception;

/**
 * Exception thrown for LLM errors.
 *
 * @author MoBai

 */
public class LLMException extends Mem0Exception {

    /**
     * Creates an exception with a message.
     *
     * @param message the message
     */
    public LLMException(String message) {
        super("LLM_ERROR", message);
    }

    /**
     * Creates an exception with a message and cause.
     *
     * @param message the message
     * @param cause the cause
     */
    public LLMException(String message, Throwable cause) {
        super("LLM_ERROR", message, cause);
    }
}
