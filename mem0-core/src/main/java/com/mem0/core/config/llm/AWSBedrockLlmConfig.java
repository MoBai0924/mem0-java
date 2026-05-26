package com.mem0.core.config.llm;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration for AWS Bedrock LLM provider.
 * Ported from Python mem0/configs/llms/aws_bedrock.py.
 *
 * @author MoBai

 */
public class AWSBedrockLlmConfig extends BaseLlmConfig {

    private String awsAccessKeyId;
    private String awsSecretAccessKey;
    private String awsRegion;
    private String awsSessionToken;
    private String awsProfile;
    private Map<String, Object> modelKwargs = new HashMap<>();

    public AWSBedrockLlmConfig() {
        super();
    }

    public String getAwsAccessKeyId() { return awsAccessKeyId; }
    public void setAwsAccessKeyId(String awsAccessKeyId) { this.awsAccessKeyId = awsAccessKeyId; }
    public String getAwsSecretAccessKey() { return awsSecretAccessKey; }
    public void setAwsSecretAccessKey(String awsSecretAccessKey) { this.awsSecretAccessKey = awsSecretAccessKey; }
    public String getAwsRegion() { return awsRegion; }
    public void setAwsRegion(String awsRegion) { this.awsRegion = awsRegion; }
    public String getAwsSessionToken() { return awsSessionToken; }
    public void setAwsSessionToken(String awsSessionToken) { this.awsSessionToken = awsSessionToken; }
    public String getAwsProfile() { return awsProfile; }
    public void setAwsProfile(String awsProfile) { this.awsProfile = awsProfile; }
    public Map<String, Object> getModelKwargs() { return modelKwargs; }
    public void setModelKwargs(Map<String, Object> modelKwargs) { this.modelKwargs = modelKwargs; }

    /**
     * Extracts the provider from the model string (e.g., "anthropic.claude-v3" -> "anthropic").
     *
     * @return the provider portion of the model identifier
     */
    public String getProvider() {
        String model = getModel();
        if (model != null && model.contains(".")) {
            return model.substring(0, model.indexOf("."));
        }
        return null;
    }

    /**
     * Extracts the model name from the model string (e.g., "anthropic.claude-v3" -> "claude-v3").
     *
     * @return the model name portion of the model identifier
     */
    public String getModelName() {
        String model = getModel();
        if (model != null && model.contains(".")) {
            return model.substring(model.indexOf(".") + 1);
        }
        return model;
    }

    /**
     * Returns AWS configuration as a map for use with Bedrock client.
     *
     * @return a map containing AWS credentials and region
     */
    public Map<String, Object> getAwsConfig() {
        Map<String, Object> config = new HashMap<>();
        if (awsAccessKeyId != null) config.put("awsAccessKeyId", awsAccessKeyId);
        if (awsSecretAccessKey != null) config.put("awsSecretAccessKey", awsSecretAccessKey);
        if (awsRegion != null) config.put("awsRegion", awsRegion);
        if (awsSessionToken != null) config.put("awsSessionToken", awsSessionToken);
        if (awsProfile != null) config.put("awsProfile", awsProfile);
        return config;
    }
}
