package com.mem0.core.exception;

/**
 * Exception thrown for invalid input.
 *
 * @author MoBai

 */
public class InvalidInputException extends Mem0Exception {

    /**
     * Creates an exception with a message.
     *
     * @param message the message
     */
    public InvalidInputException(String message) {
        super("INVALID_INPUT", message);
    }

    /**
     * Creates an exception with a message and cause.
     *
     * @param message the message
     * @param cause the cause
     */
    public InvalidInputException(String message, Throwable cause) {
        super("INVALID_INPUT", message, cause);
    }
}
