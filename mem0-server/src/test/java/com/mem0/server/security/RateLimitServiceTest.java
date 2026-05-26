package com.mem0.server.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Test
    @DisplayName("allowRequest - allows requests within limit")
    void allowRequestWithinLimit() {
        assertTrue(rateLimitService.allowRequest("user1"));
    }

    @Test
    @DisplayName("allowRequest - blocks requests exceeding limit")
    void allowRequestBlocksExceedingLimit() {
        // Allow 3 requests with capacity 3
        assertTrue(rateLimitService.allowRequest("user2", 3, 3));
        assertTrue(rateLimitService.allowRequest("user2", 3, 3));
        assertTrue(rateLimitService.allowRequest("user2", 3, 3));
        // 4th should be blocked
        assertFalse(rateLimitService.allowRequest("user2", 3, 3));
    }

    @Test
    @DisplayName("allowRequest - tracks different keys independently")
    void allowRequestTracksKeysIndependently() {
        assertTrue(rateLimitService.allowRequest("userA", 1, 1));
        assertFalse(rateLimitService.allowRequest("userA", 1, 1));
        assertTrue(rateLimitService.allowRequest("userB", 1, 1));
    }

    @Test
    @DisplayName("getRemainingTokens - returns remaining count")
    void getRemainingTokensReturnsCount() {
        rateLimitService.allowRequest("user3", 5, 5);
        assertEquals(4, rateLimitService.getRemainingTokens("user3"));
    }

    @Test
    @DisplayName("reset - clears rate limit for key")
    void resetClearsRateLimit() {
        rateLimitService.allowRequest("user4", 1, 1);
        assertFalse(rateLimitService.allowRequest("user4", 1, 1));
        rateLimitService.reset("user4");
        assertTrue(rateLimitService.allowRequest("user4", 1, 1));
    }
}
