package org.cneko.toneko.common.mod.ai.provider.impl;

import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.providers.openai.OpenAIConfig;
import org.cneko.ai.providers.openai.OpenAIService;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;

/**
 * Ollama provider — local LLM runtime, OpenAI-compatible API.
 * No API key required. Default URL: http://localhost:11434
 */
public class OllamaProvider implements AIServiceProvider {

    @Override
    public String getProviderId() { return "ollama"; }

    @Override
    public String getDisplayName() { return "Ollama (Local)"; }

    @Override
    public boolean isOpenAICompatible() { return true; }

    @Override
    public boolean requiresApiKey() { return false; }

    @Override
    public String getDefaultHost() { return "localhost"; }

    @Override
    public int getDefaultPort() { return 11434; }

    @Override
    public String getDefaultEndpoint() { return "/v1/chat/completions"; }

    @Override
    public boolean isDefaultTls() { return false; }

    @Override
    public String getDefaultModel() { return "llama3"; }

    @Override
    public AIResponse processRequest(AIServiceConfig config, AIRequest request) throws Exception {
        OpenAIConfig openAIConfig = new OpenAIConfig(config.getApiKey());
        openAIConfig.setModel(config.getModel());
        OpenAIProvider.applyCommon(openAIConfig, config, getDefaultHost());
        OpenAIService service = new OpenAIService(openAIConfig);
        return service.processRequest(request);
    }
}
