package com.mem0.core.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a stored memory with content, metadata, and vector embedding.
 * Memories are extracted from conversations and stored for retrieval.
 *
 * @author MoBai

 */
public class Memory {

    @NotBlank(message = "Memory content cannot be blank")
    private String content;

//    @NotBlank(message = "Memory hash cannot be blank")
//    private String hash;

    private Map<String, Object> metadata = new HashMap<>();

    private String userId;

    private String agentId;

    private String runId;

    private Double[] embedding;

    @NotNull
    private String createdAt;

    private String updatedAt;

    private Long version;

    private String id;

    /**
     * Default constructor.
     */
    public Memory() {
        this.createdAt = LocalDateTime.now().toString();
    }

    /**
     * Creates a new memory with the given content.
     *
     * @param content the memory content
     * @param hash the content hash for deduplication
     */
    public Memory(String content, String hash) {
        this();
        this.content = Objects.requireNonNull(content, "Content cannot be null");
//        this.hash = Objects.requireNonNull(hash, "Hash cannot be null");
    }

    /**
     * Creates a new memory with all fields.
     *
     * @param content the memory content
     * @param hash the content hash
     * @param userId the user ID
     * @param agentId the agent ID
     * @param runId the run ID
     * @param metadata the metadata map
     */
    public Memory(String content, String hash, String userId, String agentId, String runId, Map<String, Object> metadata) {
        this(content, hash);
        this.userId = userId;
        this.agentId = agentId;
        this.runId = runId;
        if (metadata != null) {
            this.metadata = new HashMap<>(metadata);
        }
    }

    /**
     * Updates the memory content and sets the updated timestamp.
     *
     * @param newContent the new content
     */
    public void updateContent(String newContent) {
        this.content = Objects.requireNonNull(newContent, "Content cannot be null");
        this.updatedAt = LocalDateTime.now().toString();;
    }

    /**
     * Updates the metadata and sets the updated timestamp.
     *
     * @param newMetadata the new metadata
     */
    public void updateMetadata(Map<String, Object> newMetadata) {
        this.metadata = newMetadata != null ? new HashMap<>(newMetadata) : new HashMap<>();
        this.updatedAt =LocalDateTime.now().toString();;
    }

    /**
     * Sets the embedding vector.
     *
     * @param embedding the embedding vector
     */
    public void setEmbedding(Double[] embedding) {
        this.embedding = embedding;
        this.updatedAt = LocalDateTime.now().toString();;
    }

    /**
     * Checks if this memory belongs to the given scope.
     *
     * @param userId the user ID to check
     * @param agentId the agent ID to check
     * @param runId the run ID to check
     * @return true if the memory matches the scope
     */
    public boolean belongsToScope(String userId, String agentId, String runId) {
        boolean userMatch = userId == null || userId.equals(this.userId);
        boolean agentMatch = agentId == null || agentId.equals(this.agentId);
        boolean runMatch = runId == null || runId.equals(this.runId);
        return userMatch && agentMatch && runMatch;
    }

    /**
     * Adds a metadata entry.
     *
     * @param key the key
     * @param value the value
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        this.updatedAt = LocalDateTime.now().toString();;
    }

    /**
     * Removes a metadata entry.
     *
     * @param key the key to remove
     */
    public void removeMetadata(String key) {
        this.metadata.remove(key);
        this.updatedAt = LocalDateTime.now().toString();;
    }

    // Getters

    public String getId() { return id; }
    public String getContent() { return content; }
//    public String getHash() { return hash; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    public String getUserId() { return userId; }
    public String getAgentId() { return agentId; }
    public String getRunId() { return runId; }
    public Double[] getEmbedding() { return embedding != null ? embedding.clone() : null; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    // Setters

    public void setId(String id) { this.id = id; }
    public void setContent(String content) { this.content = content; }
//    public void setHash(String hash) { this.hash = hash; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public void setRunId(String runId) { this.runId = runId; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public void setVersion(Long version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Memory memory = (Memory) o;
        return Objects.equals(id, memory.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Memory{" +
            "id=" + id +
            ", content='" + content + '\'' +
            ", userId='" + userId + '\'' +
            ", agentId='" + agentId + '\'' +
            ", runId='" + runId + '\'' +
            ", createdAt=" + createdAt +
            '}';
    }
}
