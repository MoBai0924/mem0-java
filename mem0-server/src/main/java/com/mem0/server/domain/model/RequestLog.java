package com.mem0.server.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Model for API request logging.
 *
 * @author MoBai

 */
@Data
public class RequestLog {

    private UUID id;

    @NotBlank
    private String method;

    @NotBlank
    private String path;

    @NotNull
    private int statusCode;

    private float latencyMs;

    private String authType;

    @NotNull
    private Instant createdAt;

    public RequestLog() {
        this.createdAt = Instant.now();
    }

    public RequestLog(String method, String path, int statusCode, float latencyMs) {
        this();
        this.method = method;
        this.path = path;
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RequestLog that = (RequestLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
