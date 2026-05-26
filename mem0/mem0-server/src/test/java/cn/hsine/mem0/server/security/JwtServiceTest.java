package cn.hsine.mem0.server.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "change-me-in-production-use-at-least-32-chars",
                86400000,
                604800000,
                "mem0"
        );
    }

    @Test
    @DisplayName("generateToken - creates valid JWT")
    void generateTokenCreatesValidJwt() {
        String userId = UUID.randomUUID().toString();
        String token = jwtService.generateToken(userId, "test@example.com");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("validateToken - returns claims for valid token")
    void validateTokenReturnsClaims() {
        String userId = UUID.randomUUID().toString();
        String token = jwtService.generateToken(userId, "test@example.com");
        Claims claims = jwtService.validateToken(token);
        assertEquals(userId, claims.getSubject());
    }

    @Test
    @DisplayName("getUserId - extracts user ID from token")
    void getUserIdExtractsId() {
        String userId = UUID.randomUUID().toString();
        String token = jwtService.generateToken(userId, "test@example.com");
        assertEquals(userId, jwtService.getUserId(token));
    }

    @Test
    @DisplayName("getEmail - extracts email from token")
    void getEmailExtractsEmail() {
        String userId = UUID.randomUUID().toString();
        String token = jwtService.generateToken(userId, "test@example.com");
        assertEquals("test@example.com", jwtService.getEmail(token));
    }

    @Test
    @DisplayName("generateRefreshToken - creates refresh token")
    void generateRefreshToken() {
        String userId = UUID.randomUUID().toString();
        String token = jwtService.generateRefreshToken(userId, "test@example.com");
        assertTrue(jwtService.isRefreshToken(token));
    }

    @Test
    @DisplayName("isRefreshToken - returns false for access token")
    void isRefreshTokenReturnsFalseForAccessToken() {
        String userId = UUID.randomUUID().toString();
        String token = jwtService.generateToken(userId, "test@example.com");
        assertFalse(jwtService.isRefreshToken(token));
    }

    @Test
    @DisplayName("validateToken - throws for invalid token")
    void validateTokenThrowsForInvalid() {
        assertThrows(Exception.class, () -> jwtService.validateToken("invalid-token"));
    }
}
