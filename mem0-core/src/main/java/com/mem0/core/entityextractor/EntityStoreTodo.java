package com.mem0.core.entityextractor;

import com.mem0.core.embedding.EmbeddingProvider;
import com.mem0.core.utils.EntityExtractorUtil;
import com.mem0.core.vectorstore.SearchResult;
import com.mem0.core.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Entity Store for managing entity linking and boosting.
 * Ported from Python mem0/memory/main.py entity store methods.
 *
 * Uses a separate vector store collection named {baseCollection}_entities.
 * Each entity has payload: {data, entity_type, linked_memory_ids, **search_filters}
 *
 * @author MoBai

 */
public class EntityStoreTodo {

    private static final Logger log = LoggerFactory.getLogger(EntityStoreTodo.class);
    private static final double ENTITY_SIMILARITY_THRESHOLD = 0.95;
    private static final double ENTITY_BOOST_THRESHOLD = 0.5;
    public static final double ENTITY_BOOST_WEIGHT = 0.5;
    private static final int MAX_ENTITIES_PER_QUERY = 8;

    private final VectorStore entityVectorStore;
    private final EmbeddingProvider embeddingProvider;

    public EntityStoreTodo(VectorStore entityVectorStore, EmbeddingProvider embeddingProvider) {
        this.entityVectorStore = entityVectorStore;
        this.embeddingProvider = embeddingProvider;
    }

    /**
     * Upserts an entity: if a similar entity exists (score >= 0.95), update its linked_memory_ids;
     * otherwise, create a new entity.
     *
     * @param entityText the entity text
     * @param entityType the entity type (PROPER, QUOTED, ACRONYM, COMPOUND)
     * @param memoryId   the memory ID to link
     * @param filters    search filters (user_id, agent_id, etc.)
     */
    public void upsertEntity(String entityText, String entityType, String memoryId, Map<String, Object> filters) {
        try {
            Double[] embedding = embeddingProvider.embed(entityText);
            List<SearchResult> results = entityVectorStore.search(embedding, 1, filters);

            if (!results.isEmpty() && results.get(0).score() >= ENTITY_SIMILARITY_THRESHOLD) {
                // Update existing entity
                String existingId = results.get(0).id();
                Map<String, Object> existingPayload = results.get(0).payload();
                @SuppressWarnings("unchecked")
                List<String> linkedIds = existingPayload.get("linked_memory_ids") instanceof List
                    ? new ArrayList<>((List<String>) existingPayload.get("linked_memory_ids"))
                    : new ArrayList<>();
                if (!linkedIds.contains(memoryId)) {
                    linkedIds.add(memoryId);
                    existingPayload.put("linked_memory_ids", linkedIds);
                    entityVectorStore.update(existingId, embedding, existingPayload);
                }
            } else {
                // Create new entity
                String entityId = UUID.randomUUID().toString();
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("data", entityText);
                payload.put("entity_type", entityType);
                payload.put("linked_memory_ids", List.of(memoryId));
                if (filters != null) {
                    payload.putAll(filters);
                }
                entityVectorStore.insert(embedding, payload, entityId);
            }
        } catch (Exception e) {
            log.warn("Failed to upsert entity '{}': {}", entityText, e.getMessage());
        }
    }

    /**
     * Links entities extracted from a memory text to the given memory ID.
     *
     * @param text     the memory text
     * @param memoryId the memory ID
     * @param filters  search filters
     */
    public void linkEntitiesForMemory(String text, String memoryId, Map<String, Object> filters) {
        List<String> entities = EntityExtractorUtil.extractEntities(text);
        // Deduplicate by normalized key
        Set<String> seen = new HashSet<>();
        for (String entity : entities) {
            String normalized = entity.toLowerCase();
            if (seen.add(normalized)) {
                upsertEntity(entity, "EXTRACTED", memoryId, filters);
            }
        }
    }

    /**
     * Removes a memory ID from all entities that reference it.
     * If an entity's linked_memory_ids becomes empty, the entity is deleted.
     *
     * @param memoryId the memory ID to remove
     * @param filters  search filters
     */
    public void removeMemoryFromEntityStore(String memoryId, Map<String, Object> filters) {
        try {
            List<com.mem0.core.vectorstore.VectorEntry> entities = entityVectorStore.list(filters, 10000);
            for (com.mem0.core.vectorstore.VectorEntry entry : entities) {
                Map<String, Object> payload = entry.payload();
                @SuppressWarnings("unchecked")
                List<String> linkedIds = payload.get("linked_memory_ids") instanceof List
                    ? new ArrayList<>((List<String>) payload.get("linked_memory_ids"))
                    : new ArrayList<>();

                if (linkedIds.contains(memoryId)) {
                    linkedIds.remove(memoryId);
                    if (linkedIds.isEmpty()) {
                        entityVectorStore.delete(entry.id());
                    } else {
                        payload.put("linked_memory_ids", linkedIds);
                        String entityText = (String) payload.getOrDefault("data", "");
                        Double[] embedding = embeddingProvider.embed(entityText);
                        entityVectorStore.update(entry.id(), embedding, payload);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to remove memory {} from entity store: {}", memoryId, e.getMessage());
        }
    }

    /**
     * Computes entity boosts for search results.
     * For each extracted entity, searches the entity store and computes
     * spread-attenuated boosts for linked memories.
     *
     * @param queryText the query text
     * @param filters   search filters
     * @return map of memory_id → boost score
     */
    public Map<String, Double> computeEntityBoosts(String queryText, Map<String, Object> filters) {
        List<String> entities = EntityExtractorUtil.extractEntities(queryText);
        // Limit to MAX_ENTITIES_PER_QUERY, deduplicated
        Set<String> seen = new HashSet<>();
        List<String> uniqueEntities = new ArrayList<>();
        for (String entity : entities) {
            if (seen.add(entity.toLowerCase()) && uniqueEntities.size() < MAX_ENTITIES_PER_QUERY) {
                uniqueEntities.add(entity);
            }
        }

        Map<String, Double> boosts = new HashMap<>();
        for (String entity : uniqueEntities) {
            try {
                Double[] embedding = embeddingProvider.embed(entity);
                List<SearchResult> results = entityVectorStore.search(embedding, 500, filters);

                for (SearchResult result : results) {
                    if (result.score() < ENTITY_BOOST_THRESHOLD) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    List<String> linkedIds = result.payload().get("linked_memory_ids") instanceof List
                        ? (List<String>) result.payload().get("linked_memory_ids")
                        : List.of();

                    int numLinked = linkedIds.size();
                    double memoryCountWeight = 1.0 / (1.0 + 0.001 * Math.pow(numLinked - 1, 2));
                    double boost = result.score() * ENTITY_BOOST_WEIGHT * memoryCountWeight;

                    for (String memoryId : linkedIds) {
                        boosts.merge(memoryId, boost, Math::max);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to compute entity boosts for '{}': {}", entity, e.getMessage());
            }
        }
        return boosts;
    }
}
