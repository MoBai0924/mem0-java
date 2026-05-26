package com.mem0.core.entitystore;

import com.mem0.core.entitystore.impl.PgEntityStore;
import com.mem0.core.exception.ConfigurationException;
import com.mem0.core.vectorstore.VectorStore;
import com.mem0.core.vectorstore.VectorStoreConfig;
import com.mem0.core.vectorstore.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 实体关系存储实现，默认使用向量库来存储。
 *
 * todo 补充图谱存储
 *
 * @author MoBai

 */
@Component
public class EntityStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(EntityStoreFactory.class);

    private final JdbcTemplate jdbcTemplate;

    public EntityStoreFactory(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Creates a EntityStore instance based on configuration.
     *
     * @param config the configuration
     * @return the VectorStore instance
     */
    public EntityStore create(EntityStoreConfig config) {
        String provider = config.getProvider();
        log.info("Creating VectorStore for provider: {}", provider);

        return switch (provider.toLowerCase()) {
            case "pgvector" -> createPgVectorStore(config);
            default -> throw new ConfigurationException("Unknown vector store provider: " + provider);
        };
    }

    private EntityStore createPgVectorStore(EntityStoreConfig config) {
        String tableName = config.get("collection-name", "memories_entities");
        return new PgEntityStore(jdbcTemplate, tableName);
    }
}
