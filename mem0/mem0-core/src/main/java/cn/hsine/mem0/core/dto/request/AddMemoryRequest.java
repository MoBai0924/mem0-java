package cn.hsine.mem0.core.dto.request;

import cn.hsine.mem0.core.dto.Message;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Request to add memories from messages.
 *
 * @param messages           the conversation messages
 * @param userId             the user ID (optional)
 * @param agentId            the agent ID (optional)
 * @param runId              the run ID (optional)
 * @param metadata           additional metadata (optional)
 * @param filters            filters to apply (optional)
 * @param customInstructions custom extraction instructions (optional)
 * @param memoryType         the memory type (optional, only PROCEDURAL is explicitly handled)
 * @param infer              if true (default), LLM extracts facts; if false, messages are added as raw memories
 * @param prompt             optional prompt for memory creation
 * @author MoBai

 */
public record AddMemoryRequest(
    @NotNull @NotEmpty List<Message> messages,
    String userId,
    String agentId,
    String runId,
    Map<String, Object> metadata,
    Map<String, Object> filters,
    String customInstructions,
    String memoryType,
    Boolean infer,
    String prompt
) {
    /**
     * Backward-compatible constructor without memoryType, infer, prompt.
     */
    public AddMemoryRequest(List<Message> messages, String userId, String agentId,
                            String runId, Map<String, Object> metadata,
                            Map<String, Object> filters, String customInstructions) {
        this(messages, userId, agentId, runId, metadata, filters, customInstructions, null, true, null);
    }
}
