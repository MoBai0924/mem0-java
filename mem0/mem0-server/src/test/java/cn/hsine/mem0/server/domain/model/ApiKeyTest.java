package cn.hsine.mem0.server.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyTest {

    @Test
    @DisplayName("constructor - creates API key with required fields")
    void constructorCreatesApiKey() {
        UUID userId = UUID.randomUUID();
        ApiKey key = new ApiKey(userId, "hash123", "my-key");
        assertEquals(userId, key.getUserId());
        assertEquals("hash123", key.getKeyHash());
        assertEquals("my-key", key.getName());
        assertTrue(key.isActive());
        assertTrue(key.isValid());
    }

    @Test
    @DisplayName("revoke - deactivates the key")
    void revokeDeactivatesKey() {
        ApiKey key = new ApiKey(UUID.randomUUID(), "hash", "key");
        key.revoke();
        assertFalse(key.isActive());
        assertFalse(key.isValid());
    }

    @Test
    @DisplayName("isExpired - returns true for past expiration")
    void isExpiredReturnsTrueForPast() {
        ApiKey key = new ApiKey(UUID.randomUUID(), "hash", "key", Instant.now().minusSeconds(60));
        assertTrue(key.isExpired());
        assertFalse(key.isValid());
    }

    @Test
    @DisplayName("isExpired - returns false for future expiration")
    void isExpiredReturnsFalseForFuture() {
        ApiKey key = new ApiKey(UUID.randomUUID(), "hash", "key", Instant.now().plusSeconds(3600));
        assertFalse(key.isExpired());
        assertTrue(key.isValid());
    }

    @Test
    @DisplayName("isValid - returns true for active non-expired key")
    void isValidReturnsTrueForActiveKey() {
        ApiKey key = new ApiKey(UUID.randomUUID(), "hash", "key");
        assertTrue(key.isValid());
    }
}
