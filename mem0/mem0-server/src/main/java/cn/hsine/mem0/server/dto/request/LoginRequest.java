package cn.hsine.mem0.server.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 *
 * @param email the email
 * @param password the password
 * @author MoBai

 */
public record LoginRequest(
    @NotBlank String email,
    @NotBlank String password
) {}
