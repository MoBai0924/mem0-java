package cn.hsine.mem0.core.config.vectorstore;

/**
 * Configuration for Azure AI Search vector store provider.
 * Ported from Python mem0/configs/vectorstores/azure_ai_search.py.
 *
 * @author MoBai

 */
public class AzureAISearchConfig {

    private String serviceName;
    private String apiKey;
    private String compressionType;
    private boolean useFloat16 = false;
    private boolean hybridSearch = false;
    private String vectorFilterMode = "full";
    private String collectionName = "memories";
    private int embeddingModelDims = 1536;

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getCompressionType() { return compressionType; }
    public void setCompressionType(String compressionType) { this.compressionType = compressionType; }
    public boolean isUseFloat16() { return useFloat16; }
    public void setUseFloat16(boolean useFloat16) { this.useFloat16 = useFloat16; }
    public boolean isHybridSearch() { return hybridSearch; }
    public void setHybridSearch(boolean hybridSearch) { this.hybridSearch = hybridSearch; }
    public String getVectorFilterMode() { return vectorFilterMode; }
    public void setVectorFilterMode(String vectorFilterMode) { this.vectorFilterMode = vectorFilterMode; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public int getEmbeddingModelDims() { return embeddingModelDims; }
    public void setEmbeddingModelDims(int embeddingModelDims) { this.embeddingModelDims = embeddingModelDims; }
}
