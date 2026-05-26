package com.mem0.server.dto.response;

import java.util.Date;

/**
 * Response DTO for user information.
 *
 * @param id        the user ID
 * @param email     the email
 * @param name      the name
 * @param createdAt the creation timestamp
 * @author MoBai
 */
public record UserResponse(
        String id,
        String email,
        String name,
        Date createdAt) {
}
