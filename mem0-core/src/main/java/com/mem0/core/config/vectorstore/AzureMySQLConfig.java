package com.mem0.core.config.vectorstore;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration for Azure MySQL vector store provider.
 * Ported from Python mem0/configs/vectorstores/azure_mysql.py.
 *
 * @author MoBai

 */
public class AzureMySQLConfig {

    private String host;
    private int port = 3306;
    private String user;
    private String password;
    private String database;
    private boolean useAzureCredential = false;
    private String sslCa;
    private boolean sslDisabled = false;
    private Map<String, Object> connectionPool = new HashMap<>();
    private String tableName = "memories";
    private int embeddingModelDims = 1536;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    public boolean isUseAzureCredential() { return useAzureCredential; }
    public void setUseAzureCredential(boolean useAzureCredential) { this.useAzureCredential = useAzureCredential; }
    public String getSslCa() { return sslCa; }
    public void setSslCa(String sslCa) { this.sslCa = sslCa; }
    public boolean isSslDisabled() { return sslDisabled; }
    public void setSslDisabled(boolean sslDisabled) { this.sslDisabled = sslDisabled; }
    public Map<String, Object> getConnectionPool() { return connectionPool; }
    public void setConnectionPool(Map<String, Object> connectionPool) { this.connectionPool = connectionPool; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
