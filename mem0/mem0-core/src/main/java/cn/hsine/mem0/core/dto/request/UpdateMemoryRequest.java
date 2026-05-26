package cn.hsine.mem0.core.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Request to update a memory.
 *
 * @param content  the new content
 * @param metadata the new metadata (optional)
 * @author MoBai

 */
public record UpdateMemoryRequest(
        @NotBlank String content,
        Map<String, Object> metadata) {
}
