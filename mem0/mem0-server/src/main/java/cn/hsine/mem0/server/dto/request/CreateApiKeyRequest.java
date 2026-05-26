package cn.hsine.mem0.server.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating an API key.
 *
 * @param name the key name
 * @author MoBai

 */
public record CreateApiKeyRequest(
    @NotBlank String name
) {}
