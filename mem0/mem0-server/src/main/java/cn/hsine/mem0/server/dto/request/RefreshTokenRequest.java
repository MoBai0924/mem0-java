package cn.hsine.mem0.server.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for token refresh.
 *
 * @param refreshToken the refresh token
 * @author MoBai

 */
public record RefreshTokenRequest(
    @NotBlank String refreshToken
) {}
