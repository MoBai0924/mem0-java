package com.mem0.core.domain.model;

import java.util.Date;
import java.util.Objects;

/**
 * Represents a history record of memory changes.
 * Tracks all ADD, UPDATE, and DELETE events for audit trail.
 *
 * @author MoBai

 */
public class MemoryHistory {

    private String id;

    private String memoryId;

    private String oldMemory;

    private String newMemory;

    private String event;

    private Date createdAt;

    private Date updatedAt;

    private Integer isDeleted;

    private String actorId;

    private String role;

    /**
     * Default constructor.
     */
    public MemoryHistory() {
        this.createdAt = new Date();
    }

    /**
     * Creates a new memory history record.
     *
     * @param memoryId  the memory ID
     * @param event     the event type
     * @param oldMemory the old memory content (null for ADD)
     * @param newMemory the new memory content (null for DELETE)
     */
    public MemoryHistory(String memoryId, String event, String oldMemory, String newMemory) {
        this();
        this.memoryId = Objects.requireNonNull(memoryId, "Memory ID cannot be null");
        this.event = Objects.requireNonNull(event, "Event cannot be null");
        this.oldMemory = oldMemory;
        this.newMemory = newMemory;
    }

    public MemoryHistory(String memoryId, String prevValue, String data, String update,
                         Date createdAt, Date updatedAt, String actorId, String role) {
    }

    /**
     * Creates a history record for an ADD event.
     */
    public static MemoryHistory addEvent(String memoryId, String newMemory) {
        return new MemoryHistory(memoryId, MemoryEvent.ADD.name(), null, newMemory);
    }

    /**
     * Creates a history record for an UPDATE event.
     */
    public static MemoryHistory updateEvent(String memoryId, String oldMemory, String newMemory) {
        return new MemoryHistory(memoryId, MemoryEvent.UPDATE.name(), oldMemory, newMemory);
    }

    /**
     * Creates a history record for a DELETE event.
     */
    public static MemoryHistory deleteEvent(String memoryId, String oldMemory) {
        return new MemoryHistory(memoryId, MemoryEvent.DELETE.name(), oldMemory, null);
    }

    /**
     * Sets the actor information.
     */
    public MemoryHistory withActor(String actorId, String role) {
        this.actorId = actorId;
        this.role = role;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getMemoryId() {
        return memoryId;
    }

    public String getOldMemory() {
        return oldMemory;
    }

    public String getNewMemory() {
        return newMemory;
    }

    public String getEvent() {
        return event;
    }

    public String getActorId() {
        return actorId;
    }

    public String getRole() {
        return role;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    // Setters

    public void setId(String id) {
        this.id = id;
    }

    public void setMemoryId(String memoryId) {
        this.memoryId = memoryId;
    }

    public void setOldMemory(String oldMemory) {
        this.oldMemory = oldMemory;
    }

    public void setNewMemory(String newMemory) {
        this.newMemory = newMemory;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MemoryHistory that = (MemoryHistory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MemoryHistory{" +
                "id='" + id + '\'' +
                ", memoryId='" + memoryId + '\'' +
                ", oldMemory='" + oldMemory + '\'' +
                ", newMemory='" + newMemory + '\'' +
                ", event='" + event + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isDeleted=" + isDeleted +
                ", actorId='" + actorId + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
