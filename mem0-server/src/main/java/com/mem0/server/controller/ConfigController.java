package com.mem0.server.controller;

import com.mem0.server.service.ConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for configuration management.
 *
 * @author MoBai

 */
@RestController
@RequestMapping("/configure")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Gets the current configuration.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(configService.getCurrentConfig());
    }

    /**
     * Updates the configuration.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(configService.updateConfig(updates));
    }

    /**
     * Lists available providers.
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProviders() {
        return ResponseEntity.ok(configService.getAvailableProviders());
    }
}
