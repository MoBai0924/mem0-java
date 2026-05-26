package com.mem0.core.config.vectorstore;

/**
 * Configuration for Chroma vector store provider.
 * Ported from Python mem0/configs/vectorstores/chroma.py.
 *
 * @author MoBai

 */
public class ChromaConfig {

    private String collectionName = "mem0";
    private String path;
    private String host;
    private int port;
    private String apiKey;
    private String tenant;

    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getTenant() { return tenant; }
    public void setTenant(String tenant) { this.tenant = tenant; }
}
