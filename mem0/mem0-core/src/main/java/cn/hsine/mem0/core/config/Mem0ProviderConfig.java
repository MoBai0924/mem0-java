package cn.hsine.mem0.core.config;

import cn.hsine.mem0.core.embedding.EmbeddingConfig;
import cn.hsine.mem0.core.embedding.EmbeddingProvider;
import cn.hsine.mem0.core.embedding.EmbeddingProviderFactory;
import cn.hsine.mem0.core.entitystore.EntityStore;
import cn.hsine.mem0.core.entitystore.EntityStoreConfig;
import cn.hsine.mem0.core.entitystore.EntityStoreFactory;
import cn.hsine.mem0.core.llm.LLMProvider;
import cn.hsine.mem0.core.llm.LLMProviderConfig;
import cn.hsine.mem0.core.llm.LLMProviderFactory;
import cn.hsine.mem0.core.reranker.Reranker;
import cn.hsine.mem0.core.reranker.RerankerFactory;
import cn.hsine.mem0.core.vectorstore.VectorStore;
import cn.hsine.mem0.core.vectorstore.VectorStoreConfig;
import cn.hsine.mem0.core.vectorstore.VectorStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring {@code @Configuration} that reads mem0.* properties and uses the
 * factory classes to create provider beans (VectorStore, LLMProvider,
 * EmbeddingProvider, Reranker) which are then injected into MemoryService.
 *
 * <p>Mirrors the Python pattern where {@code Memory.__init__()} calls
 * {@code VectorStoreFactory.create()}, {@code LlmFactory.create()}, etc.
 *
 * @author MoBai

 */
@Configuration
public class Mem0ProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(Mem0ProviderConfig.class);

    // ── VectorStore ────────────────────────────────────────────────────────

    /**
     * Creates the {@link VectorStore} bean by delegating to
     * {@link VectorStoreFactory#create(VectorStoreConfig)}.
     * <p>Configured via {@code mem0.vector-store.provider} (default: pgvector).
     */
    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore vectorStore(VectorStoreFactory factory, VectorStoreConfig config) {
        log.info("Creating VectorStore bean [provider={}]", config.getProvider());
        return factory.create(config);
    }

    // ── LLMProvider ────────────────────────────────────────────────────────

    /**
     * Creates the {@link LLMProvider} bean by delegating to
     * {@link LLMProviderFactory#create(LLMProviderConfig)}.
     * <p>Configured via {@code mem0.llm.provider} (default: openai).
     */
    @Bean
    @ConditionalOnMissingBean(LLMProvider.class)
    public LLMProvider llmProvider(LLMProviderFactory factory, LLMProviderConfig config) {
        log.info("Creating LLMProvider bean [provider={}]", config.getProvider());
        return factory.create(config);
    }

    // ── EmbeddingProvider ──────────────────────────────────────────────────

    /**
     * Creates the {@link EmbeddingProvider} bean by delegating to
     * {@link EmbeddingProviderFactory#create(EmbeddingConfig)}.
     * <p>Configured via {@code mem0.embedding.provider} (default: openai).
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingProvider.class)
    public EmbeddingProvider embeddingProvider(EmbeddingProviderFactory factory, EmbeddingConfig config) {
        log.info("Creating EmbeddingProvider bean [provider={}]", config.getProvider());
        return factory.create(config);
    }

    // ── Reranker (optional) ────────────────────────────────────────────────

    /**
     * Creates the {@link Reranker} bean <em>only if</em>
     * {@code mem0.reranker.provider} is set in configuration.
     * <p>If the property is absent, no Reranker bean is registered and
     * MemoryService will receive {@code null} for this dependency.
     */
    @Bean
    @ConditionalOnProperty(prefix = "mem0.reranker", name = "provider")
    public Reranker reranker(RerankerFactory factory, MemoryConfig memoryConfig) {
        MemoryConfig.RerankerConfig rc = memoryConfig.getReranker();
        log.info("Creating Reranker bean [provider={}]", rc.getProvider());
        return factory.create(rc.getProvider(), rc.getConfig());
    }

    @Bean
    @ConditionalOnMissingBean(EntityStore.class)
    public EntityStore entityStore(EntityStoreFactory factory, EntityStoreConfig config) {
        log.info("Creating EntityStore bean [provider={}]", config.getProvider());
        return factory.create(config);
    }
}
