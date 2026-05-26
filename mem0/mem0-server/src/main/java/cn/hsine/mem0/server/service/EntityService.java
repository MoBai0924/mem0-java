package cn.hsine.mem0.server.service;

import cn.hsine.mem0.core.domain.model.Memory;
import cn.hsine.mem0.core.repository.MemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for entity management operations.
 * Entities represent the distinct user_id, agent_id, and run_id values
 * that scope memories.
 *
 * @author MoBai

 */
@Service
public class EntityService {

    private static final Logger log = LoggerFactory.getLogger(EntityService.class);

    private final MemoryRepository memoryRepository;

    public EntityService(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    /**
     * Lists all distinct entity IDs (user, agent, run).
     *
     * @return the list of entities
     */
    public List<EntityInfo> listEntities() {
        List<Memory> allMemories = memoryRepository.findByFilters(null, null, null);

        java.util.Set<String> users = new java.util.HashSet<>();
        java.util.Set<String> agents = new java.util.HashSet<>();
        java.util.Set<String> runs = new java.util.HashSet<>();

        for (Memory m : allMemories) {
            if (m.getUserId() != null) users.add(m.getUserId());
            if (m.getAgentId() != null) agents.add(m.getAgentId());
            if (m.getRunId() != null) runs.add(m.getRunId());
        }

        return List.of(
            new EntityInfo("user", List.copyOf(users)),
            new EntityInfo("agent", List.copyOf(agents)),
            new EntityInfo("run", List.copyOf(runs))
        );
    }

    /**
     * Deletes all memories for a given entity.
     *
     * @param entityType the entity type (user, agent, run)
     * @param entityId the entity ID
     */
    @Transactional
    public void deleteEntity(String entityType, String entityId) {
        log.info("Deleting entity: type={}, id={}", entityType, entityId);

        switch (entityType.toLowerCase()) {
            case "user" -> memoryRepository.deleteByUserId(entityId);
            case "agent" -> memoryRepository.deleteByAgentId(entityId);
            case "run" -> memoryRepository.deleteByRunId(entityId);
            default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
        }
    }

    /**
     * Information about entities of a given type.
     *
     * @param type the entity type
     * @param ids the entity IDs
     */
    public record EntityInfo(String type, List<String> ids) {}
}
