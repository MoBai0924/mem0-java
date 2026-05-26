package cn.hsine.mem0.server.dto.response;

import java.util.UUID;

/**
 * Response DTO for API key information.
 *
 * @param id the key ID
 * @param key the plain key (only returned on creation)
 * @param name the key name
 * @author MoBai

 */
public record ApiKeyResponse(
    UUID id,
    String key,
    String name
) {}
