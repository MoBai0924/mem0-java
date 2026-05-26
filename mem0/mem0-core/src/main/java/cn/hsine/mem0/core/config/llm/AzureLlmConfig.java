package cn.hsine.mem0.core.config.llm;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration for Azure OpenAI LLM provider.
 * Ported from Python mem0/configs/llms/azure.py.
 *
 * @author MoBai

 */
public class AzureLlmConfig extends BaseLlmConfig {

    private AzureConfig azureKwargs;

    public AzureLlmConfig() {
        super();
        this.azureKwargs = new AzureConfig();
    }

    public AzureConfig getAzureKwargs() { return azureKwargs; }
    public void setAzureKwargs(AzureConfig azureKwargs) { this.azureKwargs = azureKwargs; }

    /**
     * Azure-specific configuration parameters.
     */
    public static class AzureConfig {
        private String apiKey;
        private String azureDeployment;
        private String azureEndpoint;
        private String apiVersion;
        private Map<String, String> defaultHeaders = new HashMap<>();

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getAzureDeployment() { return azureDeployment; }
        public void setAzureDeployment(String azureDeployment) { this.azureDeployment = azureDeployment; }
        public String getAzureEndpoint() { return azureEndpoint; }
        public void setAzureEndpoint(String azureEndpoint) { this.azureEndpoint = azureEndpoint; }
        public String getApiVersion() { return apiVersion; }
        public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
        public Map<String, String> getDefaultHeaders() { return defaultHeaders; }
        public void setDefaultHeaders(Map<String, String> defaultHeaders) { this.defaultHeaders = defaultHeaders; }
    }
}
