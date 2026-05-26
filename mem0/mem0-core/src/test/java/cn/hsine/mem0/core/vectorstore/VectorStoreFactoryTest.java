package cn.hsine.mem0.core.vectorstore;

import cn.hsine.mem0.core.exception.ConfigurationException;
import cn.hsine.mem0.core.vectorstore.impl.*;
import cn.hsine.mem0.core.vectorstore.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VectorStoreFactoryTest {

    private VectorStoreFactory factory;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        factory = new VectorStoreFactory(jdbcTemplate);
    }

    @Test
    @DisplayName("getSupportedProviders - returns all provider names")
    void getSupportedProviders() {
        String[] providers = VectorStoreFactory.getSupportedProviders();
        assertEquals(8, providers.length);
        assertArrayEquals(new String[]{"pgvector", "qdrant", "chroma", "pinecone", "milvus", "mongodb", "redis", "faiss"}, providers);
    }

    @Test
    @DisplayName("create - creates PgVectorStore")
    void createPgVector() {
        VectorStoreConfig config = new VectorStoreConfig();
        config.setProvider("pgvector");
        config.setConfig(new HashMap<>());

        VectorStore store = factory.create(config);
        assertInstanceOf(PgVectorStore.class, store);
        assertEquals("pgvector", store.getName());
    }

    @Test
    @DisplayName("create - creates QdrantVectorStore")
    void createQdrant() {
        VectorStoreConfig config = new VectorStoreConfig();
        config.setProvider("qdrant");
        config.setConfig(new HashMap<>());

        VectorStore store = factory.create(config);
        assertInstanceOf(QdrantVectorStore.class, store);
        assertEquals("qdrant", store.getName());
    }

    @Test
    @DisplayName("create - creates ChromaVectorStore")
    void createChroma() {
        VectorStoreConfig config = new VectorStoreConfig();
        config.setProvider("chroma");
        config.setConfig(new HashMap<>());

        VectorStore store = factory.create(config);
        assertInstanceOf(ChromaVectorStore.class, store);
        assertEquals("chroma", store.getName());
    }

    @Test
    @DisplayName("create - creates PineconeVectorStore")
    void createPinecone() {
        VectorStoreConfig config = new VectorStoreConfig();
        config.setProvider("pinecone");
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("api-key", "pinecone-key");
        cfg.put("base-url", "https://test.pinecone.io");
        config.setConfig(cfg);

        VectorStore store = factory.create(config);
        assertInstanceOf(PineconeVectorStore.class, store);
        assertEquals("pinecone", store.getName());
    }

    @Test
    @DisplayName("create - creates MilvusVectorStore")
    void createMilvus() {
        VectorStoreConfig config = new VectorStoreConfig();
        config.setProvider("milvus");
        config.setConfig(new HashMap<>());

        VectorStore store = factory.create(config);
        assertInstanceOf(MilvusVectorStore.class, store);
        assertEquals("milvus", store.getName());
    }

    @Test
    @DisplayName("create - creates MongoDBVectorStore")
    void createMongoDB() {
        VectorStoreConfig config = new VectorStoreConfig();
        config.setProvider("mongodb");
        config.setConfig(Map.of("connection-string", "mongodb://localhost:27017"));

        VectorStore store = factory.create(config);
        assertInstanceOf(MongoDBVectorStore.class, store);
        assertEquals("mongodb", store.getName());
    }

    @Test
    @DisplayName("create - creates RedisVectorStore")
    void createRedis() {
        VectorStoreConfig config = new VectorStoreConfig();
        config.setProvider("redis");
        config.setConfig(new HashMap<>());

        VectorStore store = factory.create(config);
        assertInstanceOf(RedisVectorStore.class, store);
        assertEquals("redis", store.getName());
    }

    @Test
    @DisplayName("create - creates FAISSVectorStore")
    void createFAISS() {
        VectorStoreConfig config = new VectorStoreConfig();
        config.setProvider("faiss");
        config.setConfig(new HashMap<>());

        VectorStore store = factory.create(config);
        assertInstanceOf(FAISSVectorStore.class, store);
        assertEquals("faiss", store.getName());
    }

    @Test
    @DisplayName("create - throws ConfigurationException for unknown provider")
    void createThrowsForUnknownProvider() {
        VectorStoreConfig config = new VectorStoreConfig();
        config.setProvider("unknown");
        config.setConfig(new HashMap<>());

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - throws ConfigurationException for missing Pinecone API key")
    void createThrowsForMissingPineconeKey() {
        VectorStoreConfig config = new VectorStoreConfig();
        config.setProvider("pinecone");
        config.setConfig(new HashMap<>());

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - throws ConfigurationException for missing Pinecone base URL")
    void createThrowsForMissingPineconeBaseUrl() {
        VectorStoreConfig config = new VectorStoreConfig();
        config.setProvider("pinecone");
        config.setConfig(Map.of("api-key", "pinecone-key"));

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - throws ConfigurationException for missing MongoDB connection string")
    void createThrowsForMissingMongoDBConnectionString() {
        VectorStoreConfig config = new VectorStoreConfig();
        config.setProvider("mongodb");
        config.setConfig(new HashMap<>());

        assertThrows(ConfigurationException.class, () -> factory.create(config));
    }

    @Test
    @DisplayName("create - is case-insensitive for provider name")
    void createIsCaseInsensitive() {
        VectorStoreConfig config = new VectorStoreConfig();
        config.setProvider("FAISS");
        config.setConfig(new HashMap<>());

        VectorStore store = factory.create(config);
        assertInstanceOf(FAISSVectorStore.class, store);
    }
}
