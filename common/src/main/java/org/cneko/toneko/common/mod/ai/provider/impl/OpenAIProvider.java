package org.cneko.toneko.common.mod.ai.provider.impl;

import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.providers.openai.OpenAIConfig;
import org.cneko.ai.providers.openai.OpenAIService;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;

/**
 * OpenAI chat completions provider.
 * Only overrides host/endpoint/tls if the user has explicitly set a custom base_url.
 * Otherwise, OpenAIConfig/OpenAIService use their own internal defaults.
 */
public class OpenAIProvider implements AIServiceProvider {

    @Override
    public String getProviderId() { return "openai"; }

    @Override
    public String getDisplayName() { return "OpenAI"; }

    @Override
    public boolean isOpenAICompatible() { return true; }

    @Override
    public boolean requiresApiKey() { return true; }

    @Override
    public String getDefaultHost() { return "api.openai.com"; }

    @Override
    public int getDefaultPort() { return 443; }

    @Override
    public String getDefaultEndpoint() { return "/v1/chat/completions"; }

    @Override
    public boolean isDefaultTls() { return true; }

    @Override
    public String getDefaultModel() { return "gpt-4o-mini"; }

    @Override
    public AIResponse processRequest(AIServiceConfig config, AIRequest request) throws Exception {
        OpenAIConfig openAIConfig = new OpenAIConfig(config.getApiKey());
        openAIConfig.setModel(config.getModel());
        applyCommon(openAIConfig, config, getDefaultHost());
        OpenAIService service = new OpenAIService(openAIConfig);
        return service.processRequest(request);
    }

    /**
     * Apply connection settings to OpenAIConfig.
     * Always sets host/endpoint/port/tls — these are needed for non-OpenAI providers
     * (OpenAIConfig defaults to api.openai.com:443 with TLS).
     */
    static void applyCommon(OpenAIConfig c, AIServiceConfig config, String defaultHost) {
        if (config.getProxy() != null) c.setProxy(config.getProxy());

        String configHost = config.getHost();
        if (configHost == null || configHost.isEmpty()) return;

        c.setHost(configHost);
        c.setPort(config.getPort());
        c.setEndpoint(config.getEndpoint());
        c.setTls(config.isTls());
    }
}
