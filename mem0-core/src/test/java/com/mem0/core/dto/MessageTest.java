package com.mem0.core.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    @DisplayName("system - creates system message")
    void systemCreatesMessage() {
        Message msg = Message.system("You are a helpful assistant");
        assertEquals("system", msg.role());
        assertEquals("You are a helpful assistant", msg.content());
    }

    @Test
    @DisplayName("user - creates user message")
    void userCreatesMessage() {
        Message msg = Message.user("Hello");
        assertEquals("user", msg.role());
        assertEquals("Hello", msg.content());
    }

    @Test
    @DisplayName("assistant - creates assistant message")
    void assistantCreatesMessage() {
        Message msg = Message.assistant("Hi there");
        assertEquals("assistant", msg.role());
        assertEquals("Hi there", msg.content());
    }
}
