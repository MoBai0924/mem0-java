package cn.hsine.mem0.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {

    // --- 序列化测试 ---

    @Test
    @DisplayName("write - serializes Map to JSON string")
    void writeMap() {
        Map<String, Object> map = Map.of("name", "test", "value", 42);
        String json = JsonUtil.write(map);
        assertTrue(json.contains("\"name\":\"test\""));
        assertTrue(json.contains("\"value\":42"));
    }

    @Test
    @DisplayName("write - serializes List to JSON array")
    void writeList() {
        List<String> list = List.of("a", "b", "c");
        String json = JsonUtil.write(list);
        assertEquals("[\"a\",\"b\",\"c\"]", json);
    }

    @Test
    @DisplayName("write - serializes null to 'null'")
    void writeNull() {
        assertEquals("null", JsonUtil.write(null));
    }

    @Test
    @DisplayName("write - serializes primitive types")
    void writePrimitives() {
        assertEquals("42", JsonUtil.write(42));
        assertEquals("3.14", JsonUtil.write(3.14));
        assertEquals("true", JsonUtil.write(true));
        assertEquals("\"hello\"", JsonUtil.write("hello"));
    }

    @Test
    @DisplayName("writePretty - returns indented JSON string")
    void writePretty() {
        Map<String, Object> map = Map.of("name", "test");
        String json = JsonUtil.writePretty(map);
        assertTrue(json.contains("\n"));
        assertTrue(json.contains("  "));
    }

    @Test
    @DisplayName("writePretty - serializes null to 'null'")
    void writePrettyNull() {
        assertEquals("null", JsonUtil.writePretty(null));
    }

    // --- 反序列化测试 ---

    @Test
    @DisplayName("read - deserializes JSON string to object by class")
    void readByClass() {
        String json = "{\"name\":\"test\",\"value\":42}";
        Map<String, Object> map = JsonUtil.read(json, Map.class);
        assertEquals("test", map.get("name"));
        assertEquals(42, map.get("value"));
    }

    @Test
    @DisplayName("read - deserializes JSON string with TypeReference preserving generic types")
    void readWithTypeReference() {
        String json = "[{\"key\":\"value\"},{\"key\":\"other\"}]";
        List<Map<String, Object>> list = JsonUtil.read(json, new TypeReference<List<Map<String, Object>>>() {});
        assertEquals(2, list.size());
        assertEquals("value", list.get(0).get("key"));
        assertEquals("other", list.get(1).get("key"));
    }

    @Test
    @DisplayName("read - throws RuntimeException for null JSON string")
    void readNullJson() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> JsonUtil.read(null, Map.class));
        assertEquals("JSON string must not be null or empty", ex.getMessage());
    }

    @Test
    @DisplayName("read - throws RuntimeException for empty JSON string")
    void readEmptyJson() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> JsonUtil.read("", Map.class));
        assertEquals("JSON string must not be null or empty", ex.getMessage());
    }

    @Test
    @DisplayName("read - throws RuntimeException for malformed JSON")
    void readMalformedJson() {
        assertThrows(RuntimeException.class, () -> JsonUtil.read("{invalid}", Map.class));
    }

    @Test
    @DisplayName("readTree - parses JSON string to JsonNode")
    void readTree() {
        String json = "{\"name\":\"test\",\"nested\":{\"key\":123}}";
        JsonNode node = JsonUtil.readTree(json);
        assertEquals("test", node.get("name").asText());
        assertEquals(123, node.get("nested").get("key").asInt());
    }

    @Test
    @DisplayName("readTree - throws RuntimeException for null JSON string")
    void readTreeNullJson() {
        assertThrows(RuntimeException.class, () -> JsonUtil.readTree(null));
    }

    @Test
    @DisplayName("readTree - throws RuntimeException for empty JSON string")
    void readTreeEmptyJson() {
        assertThrows(RuntimeException.class, () -> JsonUtil.readTree(""));
    }

    @Test
    @DisplayName("read - FAIL_ON_UNKNOWN_PROPERTIES is disabled")
    void readIgnoreUnknownProperties() {
        String json = "{\"name\":\"test\",\"extraField\":\"ignored\"}";
        // Map will accept any field, so test with a simple POJO-like approach
        // Using readTree to verify the ObjectMapper config
        ObjectMapper om = JsonUtil.getObjectMapper();
        assertFalse(om.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    // --- 转换测试 ---

    @Test
    @DisplayName("toMap - converts object to Map")
    void toMapObject() {
        Map<String, Object> source = Map.of("name", "test", "value", 42);
        Map<String, Object> result = JsonUtil.toMap(source);
        assertEquals("test", result.get("name"));
        assertEquals(42, result.get("value"));
    }

    @Test
    @DisplayName("toMap - returns empty Map for null input")
    void toMapNull() {
        assertTrue(JsonUtil.toMap(null).isEmpty());
    }

    @Test
    @DisplayName("fromMap - converts Map to object of target type")
    void fromMapObject() {
        Map<String, Object> map = Map.of("name", "test", "value", 42);
        Map<String, Object> result = JsonUtil.fromMap(map, Map.class);
        assertEquals("test", result.get("name"));
        assertEquals(42, result.get("value"));
    }

    @Test
    @DisplayName("fromMap - returns null for null Map input")
    void fromMapNull() {
        assertNull(JsonUtil.fromMap(null, Map.class));
    }

    // --- 校验测试 ---

    @Test
    @DisplayName("isValid - returns true for valid JSON object")
    void isValidJsonObject() {
        assertTrue(JsonUtil.isValid("{\"key\":\"value\"}"));
    }

    @Test
    @DisplayName("isValid - returns true for valid JSON array")
    void isValidJsonArray() {
        assertTrue(JsonUtil.isValid("[1,2,3]"));
    }

    @Test
    @DisplayName("isValid - returns true for valid JSON primitive")
    void isValidJsonPrimitive() {
        assertTrue(JsonUtil.isValid("\"hello\""));
        assertTrue(JsonUtil.isValid("42"));
        assertTrue(JsonUtil.isValid("true"));
    }

    @Test
    @DisplayName("isValid - returns false for invalid JSON")
    void isInvalidJson() {
        assertFalse(JsonUtil.isValid("{invalid}"));
        assertFalse(JsonUtil.isValid("not json"));
    }

    @Test
    @DisplayName("isValid - returns false for null input")
    void isValidNull() {
        assertFalse(JsonUtil.isValid(null));
    }

    @Test
    @DisplayName("isValid - returns false for empty string")
    void isValidEmpty() {
        assertFalse(JsonUtil.isValid(""));
    }

    // --- ObjectMapper 访问与线程安全测试 ---

    @Test
    @DisplayName("getObjectMapper - returns non-null ObjectMapper instance")
    void getObjectMapperNotNull() {
        assertNotNull(JsonUtil.getObjectMapper());
    }

    @Test
    @DisplayName("getObjectMapper - returns same instance on repeated calls")
    void getObjectMapperSameInstance() {
        ObjectMapper om1 = JsonUtil.getObjectMapper();
        ObjectMapper om2 = JsonUtil.getObjectMapper();
        assertSame(om1, om2);
    }

    @Test
    @DisplayName("getObjectMapper - FAIL_ON_UNKNOWN_PROPERTIES is disabled")
    void getObjectMapperConfig() {
        ObjectMapper om = JsonUtil.getObjectMapper();
        assertFalse(om.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @Test
    @DisplayName("concurrent access - thread safety verification")
    void concurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        Map<String, Object> testData = Map.of("name", "concurrent", "value", 99);
        String testJson = JsonUtil.write(testData);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        String serialized = JsonUtil.write(testData);
                        Map<String, Object> deserialized = JsonUtil.read(serialized, Map.class);
                        if (!"concurrent".equals(deserialized.get("name")) || !Integer.valueOf(99).equals(deserialized.get("value"))) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        assertEquals(0, errorCount.get(), "Concurrent access produced errors or corrupt results");
    }
}
