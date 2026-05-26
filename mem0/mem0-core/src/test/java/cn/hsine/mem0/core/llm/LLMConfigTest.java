package cn.hsine.mem0.core.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LLMConfigTest {

    @Test
    @DisplayName("canonical constructor - creates config with all params")
    void canonicalConstructor() {
        LLMConfig config = new LLMConfig(0.7, 2048, 0.9, null);
        assertEquals(0.7, config.temperature());
        assertEquals(2048, config.maxTokens());
        assertEquals(0.9, config.topP());
    }

    @Test
    @DisplayName("convenience constructor - creates config with temperature and maxTokens")
    void convenienceConstructor() {
        LLMConfig config = new LLMConfig(0.5, 1000);
        assertEquals(0.5, config.temperature());
        assertEquals(1000, config.maxTokens());
    }

    @Test
    @DisplayName("default constructor - creates config with defaults")
    void defaultConstructor() {
        LLMConfig config = new LLMConfig();
        assertNotNull(config);
    }
}
