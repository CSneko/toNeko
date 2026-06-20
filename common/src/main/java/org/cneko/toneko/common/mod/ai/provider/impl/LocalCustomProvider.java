package org.cneko.toneko.common.mod.ai.provider.impl;

import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.providers.openai.OpenAIConfig;
import org.cneko.ai.providers.openai.OpenAIService;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;

/**
 * Local / custom URL provider — any OpenAI-compatible endpoint.
 * Always applies the user-specified host/port/endpoint since there are no fixed defaults.
 */
public class LocalCustomProvider implements AIServiceProvider {

    @Override
    public String getProviderId() { return "custom"; }

    @Override
    public String getDisplayName() { return "Custom (OpenAI-Compatible)"; }

    @Override
    public boolean isOpenAICompatible() { return true; }

    @Override
    public boolean requiresApiKey() { return false; }

    @Override
    public String getDefaultHost() { return "localhost"; }

    @Override
    public int getDefaultPort() { return 8080; }

    @Override
    public String getDefaultEndpoint() { return "/v1/chat/completions"; }

    @Override
    public boolean isDefaultTls() { return false; }

    @Override
    public String getDefaultModel() { return ""; }

    @Override
    public AIResponse processRequest(AIServiceConfig config, AIRequest request) throws Exception {
        OpenAIConfig openAIConfig = new OpenAIConfig(config.getApiKey());
        openAIConfig.setModel(config.getModel());
        // custom provider: always apply connection params (user-specified host)
        OpenAIProvider.applyCommon(openAIConfig, config, "");
        OpenAIService service = new OpenAIService(openAIConfig);
        return service.processRequest(request);
    }
}
