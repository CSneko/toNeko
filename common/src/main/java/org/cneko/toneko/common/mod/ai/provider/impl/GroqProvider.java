package org.cneko.toneko.common.mod.ai.provider.impl;

import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.providers.openai.OpenAIConfig;
import org.cneko.ai.providers.openai.OpenAIService;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;

/**
 * Groq provider — OpenAI-compatible, ultra-fast inference.
 */
public class GroqProvider implements AIServiceProvider {

    @Override
    public String getProviderId() { return "groq"; }

    @Override
    public String getDisplayName() { return "Groq"; }

    @Override
    public boolean isOpenAICompatible() { return true; }

    @Override
    public boolean requiresApiKey() { return true; }

    @Override
    public String getDefaultHost() { return "api.groq.com"; }

    @Override
    public int getDefaultPort() { return 443; }

    @Override
    public String getDefaultEndpoint() { return "/openai/v1/chat/completions"; }

    @Override
    public boolean isDefaultTls() { return true; }

    @Override
    public String getDefaultModel() { return "llama-3.1-8b-instant"; }

    @Override
    public AIResponse processRequest(AIServiceConfig config, AIRequest request) throws Exception {
        OpenAIConfig openAIConfig = new OpenAIConfig(config.getApiKey());
        openAIConfig.setModel(config.getModel());
        OpenAIProvider.applyCommon(openAIConfig, config, getDefaultHost());
        OpenAIService service = new OpenAIService(openAIConfig);
        return service.processRequest(request);
    }
}
