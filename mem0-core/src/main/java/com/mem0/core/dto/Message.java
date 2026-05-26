package com.mem0.core.dto;

/**
 * Represents a message in a conversation.
 *
 * @param role    the message role (system, user, assistant)
 * @param content the message content
 * @author MoBai

 */
public record Message(
        String role,
        String content,
        String name) {

    /**
     * Creates a system message.
     *
     * @param content the content
     * @return the message
     */
    public static Message system(String content) {
        return new Message("system", content, "");
    }

    /**
     * Creates a user message.
     *
     * @param content the content
     * @return the message
     */
    public static Message user(String content) {
        return new Message("user", content, "");
    }

    /**
     * Creates an assistant message.
     *
     * @param content the content
     * @return the message
     */
    public static Message assistant(String content) {
        return new Message("assistant", content, "");
    }

    public static Message define(String role,
                                 String content,
                                 String name) {
        return new Message(role, content, name);
    }
}
