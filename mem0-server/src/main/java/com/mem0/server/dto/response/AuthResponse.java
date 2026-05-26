package com.mem0.server.dto.response;

import java.util.UUID;

/**
 * Response DTO for authentication.
 *
 * @param token the JWT token
 * @param refreshToken the refresh token
 * @param userId the user ID
 * @param email the email
 * @author MoBai

 */
public record AuthResponse(
    String token,
    String refreshToken,
    String userId,
    String email
) {}
