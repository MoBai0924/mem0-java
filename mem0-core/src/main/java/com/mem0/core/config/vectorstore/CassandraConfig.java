package com.mem0.core.config.vectorstore;

/**
 * Configuration for Cassandra vector store provider.
 * Ported from Python mem0/configs/vectorstores/cassandra.py.
 *
 * @author MoBai

 */
public class CassandraConfig {

    private String contactPoints;
    private int port = 9042;
    private String username;
    private String password;
    private String keyspace = "mem0";
    private String secureConnectBundle;
    private int protocolVersion = 4;
    private String tableName = "memories";
    private int embeddingModelDims = 1536;

    public String getContactPoints() { return contactPoints; }
    public void setContactPoints(String contactPoints) { this.contactPoints = contactPoints; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getKeyspace() { return keyspace; }
    public void setKeyspace(String keyspace) { this.keyspace = keyspace; }
    public String getSecureConnectBundle() { return secureConnectBundle; }
    public void setSecureConnectBundle(String secureConnectBundle) { this.secureConnectBundle = secureConnectBundle; }
    public int getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(int protocolVersion) { this.protocolVersion = protocolVersion; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
