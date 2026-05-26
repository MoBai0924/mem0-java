package cn.hsine.mem0.server.controller;

import cn.hsine.mem0.server.service.EntityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for entity management.
 *
 * @author MoBai

 */
@RestController
@RequestMapping("/entities")
public class EntityController {

    private final EntityService entityService;

    public EntityController(EntityService entityService) {
        this.entityService = entityService;
    }

    /**
     * Lists all entities.
     */
    @GetMapping
    public ResponseEntity<List<EntityService.EntityInfo>> listEntities() {
        return ResponseEntity.ok(entityService.listEntities());
    }

    /**
     * Deletes an entity and all its associated memories.
     */
    @DeleteMapping("/{entityType}/{entityId}")
    public ResponseEntity<Void> deleteEntity(
        @PathVariable String entityType,
        @PathVariable String entityId
    ) {
        entityService.deleteEntity(entityType, entityId);
        return ResponseEntity.noContent().build();
    }
}
