package com.mem0.core.config.vectorstore;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration for PGVector vector store provider.
 * Ported from Python mem0/configs/vectorstores/pgvector.py.
 *
 * @author MoBai

 */
public class PGVectorConfig {

    private String host = "localhost";
    private int port = 5432;
    private String dbname = "mem0";
    private String user = "postgres";
    private String password = "postgres";
    private Map<String, Object> diskann = new HashMap<>();
    private Map<String, Object> hnsw = new HashMap<>();
    private int minConn = 1;
    private int maxConn = 10;
    private String sslmode;
    private String connectionString;
    private int embeddingModelDims = 1536;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getDbname() { return dbname; }
    public void setDbname(String dbname) { this.dbname = dbname; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Map<String, Object> getDiskann() { return diskann; }
    public void setDiskann(Map<String, Object> diskann) { this.diskann = diskann; }
    public Map<String, Object> getHnsw() { return hnsw; }
    public void setHnsw(Map<String, Object> hnsw) { this.hnsw = hnsw; }
    public int getMinConn() { return minConn; }
    public void setMinConn(int minConn) { this.minConn = minConn; }
    public int getMaxConn() { return maxConn; }
    public void setMaxConn(int maxConn) { this.maxConn = maxConn; }
    public String getSslmode() { return sslmode; }
    public void setSslmode(String sslmode) { this.sslmode = sslmode; }
    public String getConnectionString() { return connectionString; }
    public void setConnectionString(String connectionString) { this.connectionString = connectionString; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
