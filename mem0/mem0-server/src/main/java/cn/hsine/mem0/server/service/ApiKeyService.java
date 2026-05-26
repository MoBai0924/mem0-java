package cn.hsine.mem0.server.service;

import cn.hsine.mem0.server.domain.model.ApiKey;
import cn.hsine.mem0.server.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Service for API key management.
 *
 * @author MoBai

 */
@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

    private final ApiKeyRepository apiKeyRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Creates a new API key for a user.
     *
     * @param userId the user ID
     * @param name the key name
     * @return the plain API key (shown only once)
     */
    @Transactional
    public CreateApiKeyResult create(UUID userId, String name) {
        byte[] keyBytes = new byte[32];
        secureRandom.nextBytes(keyBytes);
        String plainKey = "mem0_" + HexFormat.of().formatHex(keyBytes);

        String keyHash = hashKey(plainKey);

        ApiKey apiKey = new ApiKey(userId, keyHash, name);
        if (apiKey.getId() == null) {
            apiKey.setId(UUID.randomUUID());
        }
        apiKeyRepository.save(apiKey);

        log.info("Created API key '{}' for user {}", name, userId);
        return new CreateApiKeyResult(apiKey.getId(), plainKey, name);
    }

    /**
     * Lists API keys for a user.
     *
     * @param userId the user ID
     * @return the list of API keys
     */
    public List<ApiKey> listByUser(UUID userId) {
        return apiKeyRepository.findByUserIdAndActive(userId, true);
    }

    /**
     * Revokes an API key.
     *
     * @param keyId the API key ID
     */
    @Transactional
    public void revoke(UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId);
        if (apiKey == null) {
            throw new IllegalArgumentException("API key not found");
        }
        apiKey.revoke();
        apiKeyRepository.update(apiKey);
        log.info("Revoked API key: {}", keyId);
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Result of creating an API key.
     *
     * @param id the key ID
     * @param plainKey the plain key (shown only once)
     * @param name the key name
     */
    public record CreateApiKeyResult(UUID id, String plainKey, String name) {}
}
