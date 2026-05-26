package cn.hsine.mem0.core.config.vectorstore;

/**
 * Configuration for OpenSearch vector store provider.
 * Ported from Python mem0/configs/vectorstores/opensearch.py.
 *
 * @author MoBai

 */
public class OpenSearchConfig {

    private String host = "localhost";
    private int port = 9200;
    private String user;
    private String password;
    private String apiKey;
    private String httpAuth;
    private String connectionClass;
    private int poolMaxsize = 10;
    private String collectionName = "memories";
    private int embeddingModelDims = 1536;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getHttpAuth() { return httpAuth; }
    public void setHttpAuth(String httpAuth) { this.httpAuth = httpAuth; }
    public String getConnectionClass() { return connectionClass; }
    public void setConnectionClass(String connectionClass) { this.connectionClass = connectionClass; }
    public int getPoolMaxsize() { return poolMaxsize; }
    public void setPoolMaxsize(int poolMaxsize) { this.poolMaxsize = poolMaxsize; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
