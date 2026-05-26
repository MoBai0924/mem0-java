package com.mem0.server.controller;

import com.mem0.server.domain.model.ApiKey;
import com.mem0.server.dto.request.CreateApiKeyRequest;
import com.mem0.server.dto.response.ApiKeyResponse;
import com.mem0.server.service.ApiKeyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for API key management.
 *
 * @author MoBai

 */
@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    /**
     * Creates a new API key.
     */
    @PostMapping
    public ResponseEntity<ApiKeyResponse> create(
        @RequestBody @Valid CreateApiKeyRequest request,
        @RequestAttribute("userId") UUID userId
    ) {
        ApiKeyService.CreateApiKeyResult result = apiKeyService.create(userId, request.name());
        ApiKeyResponse response = new ApiKeyResponse(result.id(), result.plainKey(), result.name());
        return ResponseEntity.ok(response);
    }

    /**
     * Lists API keys for the current user.
     */
    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> list(@RequestAttribute("userId") UUID userId) {
        List<ApiKey> keys = apiKeyService.listByUser(userId);
        List<ApiKeyResponse> response = keys.stream()
            .map(k -> new ApiKeyResponse(k.getId(), null, k.getName()))
            .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Revokes an API key.
     */
    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID keyId) {
        apiKeyService.revoke(keyId);
        return ResponseEntity.noContent().build();
    }
}
