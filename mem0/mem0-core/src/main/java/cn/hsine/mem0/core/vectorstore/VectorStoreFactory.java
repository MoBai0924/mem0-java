package cn.hsine.mem0.core.vectorstore;

import cn.hsine.mem0.core.exception.ConfigurationException;
import cn.hsine.mem0.core.vectorstore.impl.*;
import cn.hsine.mem0.core.vectorstore.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Factory for creating VectorStore instances.
 *
 * @author MoBai

 */
@Component
public class VectorStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreFactory.class);

    private final JdbcTemplate jdbcTemplate;

    public VectorStoreFactory(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Creates a VectorStore instance based on configuration.
     *
     * @param config the configuration
     * @return the VectorStore instance
     */
    public VectorStore create(VectorStoreConfig config) {
        String provider = config.getProvider();
        log.info("Creating VectorStore for provider: {}", provider);

        return switch (provider.toLowerCase()) {
            case "pgvector" -> createPgVectorStore(config);
            case "qdrant" -> createQdrantStore(config);
            case "chroma" -> createChromaStore(config);
            case "pinecone" -> createPineconeStore(config);
            case "milvus" -> createMilvusStore(config);
            case "mongodb" -> createMongoDBStore(config);
            case "redis" -> createRedisStore(config);
            case "faiss" -> createFAISSStore(config);
            default -> throw new ConfigurationException("Unknown vector store provider: " + provider);
        };
    }

    private VectorStore createPgVectorStore(VectorStoreConfig config) {
        String tableName = config.get("collection-name", "memories");
        return new PgVectorStore(jdbcTemplate, tableName);
    }

    private VectorStore createQdrantStore(VectorStoreConfig config) {
        String baseUrl = config.get("base-url", "http://localhost:6333");
        String collection = config.get("collection-name", "mem0");
        String apiKey = (String) config.get("api-key");
        return new QdrantVectorStore(baseUrl, collection, apiKey);
    }

    private VectorStore createChromaStore(VectorStoreConfig config) {
        String baseUrl = config.get("base-url", "http://localhost:8000");
        String collection = config.get("collection-name", "mem0");
        return new ChromaVectorStore(baseUrl, collection);
    }

    private VectorStore createPineconeStore(VectorStoreConfig config) {
        String apiKey = (String) config.get("api-key");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ConfigurationException("Pinecone API key is required");
        }
        String baseUrl = (String) config.get("base-url");
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new ConfigurationException("Pinecone base URL is required");
        }
        String indexName = config.get("index-name", "mem0");
        return new PineconeVectorStore(apiKey, baseUrl, indexName);
    }

    private VectorStore createMilvusStore(VectorStoreConfig config) {
        String baseUrl = config.get("base-url", "http://localhost:19530");
        String collection = config.get("collection-name", "mem0");
        return new MilvusVectorStore(baseUrl, collection);
    }

    private VectorStore createMongoDBStore(VectorStoreConfig config) {
        String connectionString = (String) config.get("connection-string");
        if (connectionString == null || connectionString.isEmpty()) {
            throw new ConfigurationException("MongoDB connection string is required");
        }
        String database = config.get("database", "mem0");
        String collection = config.get("collection-name", "vectors");
        return new MongoDBVectorStore(connectionString, database, collection);
    }

    private VectorStore createRedisStore(VectorStoreConfig config) {
        String host = config.get("host", "localhost");
        int port = config.get("port", 6379);
        String indexName = config.get("index-name", "mem0");
        return new RedisVectorStore(host, port, indexName);
    }

    private VectorStore createFAISSStore(VectorStoreConfig config) {
        String indexPath = config.get("index-path", "faiss_index");
        return new FAISSVectorStore(indexPath);
    }

    /**
     * Gets the list of supported providers.
     *
     * @return the supported providers
     */
    public static String[] getSupportedProviders() {
        return new String[]{"pgvector", "qdrant", "chroma", "pinecone", "milvus", "mongodb", "redis", "faiss"};
    }
}
