package com.mem0.core.config.vectorstore;

/**
 * Configuration for MongoDB vector store provider.
 * Ported from Python mem0/configs/vectorstores/mongodb.py.
 *
 * @author MoBai

 */
public class MongoDBConfig {

    private String dbName = "mem0";
    private String mongoUri = "mongodb://localhost:27017";
    private String collectionName = "memories";
    private int embeddingModelDims = 1536;

    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }
    public String getMongoUri() { return mongoUri; }
    public void setMongoUri(String mongoUri) { this.mongoUri = mongoUri; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
