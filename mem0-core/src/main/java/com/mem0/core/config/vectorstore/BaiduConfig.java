package com.mem0.core.config.vectorstore;

/**
 * Configuration for Baidu vector store provider.
 * Ported from Python mem0/configs/vectorstores/baidu.py.
 *
 * @author MoBai

 */
public class BaiduConfig {

    private String endpoint;
    private String account;
    private String apiKey;
    private String databaseName;
    private String tableName = "memories";
    private String metricType = "L2";
    private int embeddingModelDims = 1536;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
