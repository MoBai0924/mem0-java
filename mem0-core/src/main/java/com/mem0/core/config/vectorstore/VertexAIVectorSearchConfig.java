package com.mem0.core.config.vectorstore;

/**
 * Configuration for Vertex AI Vector Search vector store provider.
 * Ported from Python mem0/configs/vectorstores/vertex_ai_vector_search.py.
 *
 * @author MoBai

 */
public class VertexAIVectorSearchConfig {

    private String projectId;
    private String projectNumber;
    private String region;
    private String endpointId;
    private String indexId;
    private String deploymentIndexId;
    private String credentialsPath;
    private int embeddingModelDims = 1536;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getProjectNumber() { return projectNumber; }
    public void setProjectNumber(String projectNumber) { this.projectNumber = projectNumber; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getEndpointId() { return endpointId; }
    public void setEndpointId(String endpointId) { this.endpointId = endpointId; }
    public String getIndexId() { return indexId; }
    public void setIndexId(String indexId) { this.indexId = indexId; }
    public String getDeploymentIndexId() { return deploymentIndexId; }
    public void setDeploymentIndexId(String deploymentIndexId) { this.deploymentIndexId = deploymentIndexId; }
    public String getCredentialsPath() { return credentialsPath; }
    public void setCredentialsPath(String credentialsPath) { this.credentialsPath = credentialsPath; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
