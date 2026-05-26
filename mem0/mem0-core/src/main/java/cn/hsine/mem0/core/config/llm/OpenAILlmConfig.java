package cn.hsine.mem0.core.config.llm;

import java.util.List;

/**
 * Configuration for OpenAI LLM provider.
 * Ported from Python mem0/configs/llms/openai.py.
 *
 * @author MoBai

 */
public class OpenAILlmConfig extends BaseLlmConfig {

    private String openaiBaseUrl;
    private List<String> models;
    private String route;
    private String openrouterBaseUrl;
    private String siteUrl;
    private String appName;
    private boolean store;

    public OpenAILlmConfig() {
        super();
        this.openaiBaseUrl = "https://api.openai.com/v1";
        this.store = false;
    }

    public String getOpenaiBaseUrl() { return openaiBaseUrl; }
    public void setOpenaiBaseUrl(String openaiBaseUrl) { this.openaiBaseUrl = openaiBaseUrl; }
    public List<String> getModels() { return models; }
    public void setModels(List<String> models) { this.models = models; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public String getOpenrouterBaseUrl() { return openrouterBaseUrl; }
    public void setOpenrouterBaseUrl(String openrouterBaseUrl) { this.openrouterBaseUrl = openrouterBaseUrl; }
    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public boolean isStore() { return store; }
    public void setStore(boolean store) { this.store = store; }
}
