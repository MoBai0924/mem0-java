package com.mem0.server.health;

import com.mem0.core.vectorstore.VectorStore;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the vector store.
 *
 * @author MoBai

 */
@Component
public class VectorStoreHealthIndicator extends AbstractHealthIndicator {

    private final VectorStore vectorStore;

    public VectorStoreHealthIndicator(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        long count = vectorStore.count();
        builder.up()
            .withDetail("provider", vectorStore.getName())
            .withDetail("vectorCount", count);
    }
}
