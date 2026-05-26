package cn.hsine.mem0.core.exception;

/**
 * Exception thrown for configuration errors.
 *
 * @author MoBai

 */
public class ConfigurationException extends Mem0Exception {

    /**
     * Creates an exception with a message.
     *
     * @param message the message
     */
    public ConfigurationException(String message) {
        super("CONFIGURATION_ERROR", message);
    }

    /**
     * Creates an exception with a message and cause.
     *
     * @param message the message
     * @param cause the cause
     */
    public ConfigurationException(String message, Throwable cause) {
        super("CONFIGURATION_ERROR", message, cause);
    }
}
