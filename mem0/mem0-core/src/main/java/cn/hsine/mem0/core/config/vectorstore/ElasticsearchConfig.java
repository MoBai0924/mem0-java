package cn.hsine.mem0.core.config.vectorstore;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration for Elasticsearch vector store provider.
 * Ported from Python mem0/configs/vectorstores/elasticsearch.py.
 *
 * @author MoBai

 */
public class ElasticsearchConfig {

    private String host = "localhost";
    private int port = 9200;
    private String user;
    private String password;
    private String cloudId;
    private String apiKey;
    private boolean verifyCerts = true;
    private String caCerts;
    private boolean useSsl = false;
    private Map<String, Object> customSearchQuery = new HashMap<>();
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
    public String getCloudId() { return cloudId; }
    public void setCloudId(String cloudId) { this.cloudId = cloudId; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public boolean isVerifyCerts() { return verifyCerts; }
    public void setVerifyCerts(boolean verifyCerts) { this.verifyCerts = verifyCerts; }
    public String getCaCerts() { return caCerts; }
    public void setCaCerts(String caCerts) { this.caCerts = caCerts; }
    public boolean isUseSsl() { return useSsl; }
    public void setUseSsl(boolean useSsl) { this.useSsl = useSsl; }
    public Map<String, Object> getCustomSearchQuery() { return customSearchQuery; }
    public void setCustomSearchQuery(Map<String, Object> customSearchQuery) { this.customSearchQuery = customSearchQuery; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
