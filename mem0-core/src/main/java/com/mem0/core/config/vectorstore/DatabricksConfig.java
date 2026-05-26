package com.mem0.core.config.vectorstore;

/**
 * Configuration for Databricks vector store provider.
 * Ported from Python mem0/configs/vectorstores/databricks.py.
 *
 * @author MoBai

 */
public class DatabricksConfig {

    private String workspaceUrl;
    private String accessToken;
    private String endpointName;
    private String catalog;
    private String schema;
    private String tableName = "memories";
    private String indexType = "DELTA_SYNC";
    private String pipelineType;
    private int embeddingModelDims = 1536;

    public String getWorkspaceUrl() { return workspaceUrl; }
    public void setWorkspaceUrl(String workspaceUrl) { this.workspaceUrl = workspaceUrl; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getEndpointName() { return endpointName; }
    public void setEndpointName(String endpointName) { this.endpointName = endpointName; }
    public String getCatalog() { return catalog; }
    public void setCatalog(String catalog) { this.catalog = catalog; }
    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getIndexType() { return indexType; }
    public void setIndexType(String indexType) { this.indexType = indexType; }
    public String getPipelineType() { return pipelineType; }
    public void setPipelineType(String pipelineType) { this.pipelineType = pipelineType; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
