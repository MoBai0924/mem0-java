package cn.hsine.mem0.core.dto.response;

import java.util.Date;

/**
 * Response containing memory history data.
 *
 * @param id        the history record ID
 * @param memoryId  the memory ID
 * @param oldMemory the old memory content
 * @param newMemory the new memory content
 * @param event     the event type
 * @param actorId   the actor ID
 * @param role      the actor role
 * @param createdAt the timestamp
 * @author MoBai

 */
public record MemoryHistoryResponse(
        String id,
        String memoryId,
        String oldMemory,
        String newMemory,
        String event,
        String actorId,
        String role,
        Date createdAt) {
}
