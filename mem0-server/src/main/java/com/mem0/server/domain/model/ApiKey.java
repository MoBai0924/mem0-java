package com.mem0.server.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an API key for programmatic access.
 *
 * @author MoBai

 */
public class ApiKey {

    @NotNull
    private UUID userId;

    @NotBlank(message = "Key hash cannot be blank")
    private String keyHash;

    private String name;

    private Instant expiresAt;

    @NotNull
    private boolean active = true;

    @NotNull
    private Instant createdAt;

    private UUID id;

    /**
     * Default constructor.
     */
    public ApiKey() {
        this.createdAt = Instant.now();
    }

    /**
     * Creates a new API key.
     *
     * @param userId the user ID
     * @param keyHash the hashed key
     * @param name the key name
     */
    public ApiKey(UUID userId, String keyHash, String name) {
        this();
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.keyHash = Objects.requireNonNull(keyHash, "Key hash cannot be null");
        this.name = name;
    }

    /**
     * Creates a new API key with expiration.
     *
     * @param userId the user ID
     * @param keyHash the hashed key
     * @param name the key name
     * @param expiresAt the expiration timestamp
     */
    public ApiKey(UUID userId, String keyHash, String name, Instant expiresAt) {
        this(userId, keyHash, name);
        this.expiresAt = expiresAt;
    }

    /**
     * Revokes the API key.
     */
    public void revoke() {
        this.active = false;
    }

    /**
     * Checks if the API key is expired.
     *
     * @return true if the key is expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if the API key is valid (active and not expired).
     *
     * @return true if the key is valid
     */
    public boolean isValid() {
        return active && !isExpired();
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getName() {
        return name;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Setters

    public void setId(UUID id) {
        this.id = id;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiKey apiKey = (ApiKey) o;
        return Objects.equals(id, apiKey.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ApiKey{" +
            "id=" + id +
            ", userId=" + userId +
            ", name='" + name + '\'' +
            ", active=" + active +
            '}';
    }
}
