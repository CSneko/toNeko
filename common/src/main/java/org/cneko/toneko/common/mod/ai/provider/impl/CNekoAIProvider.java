package org.cneko.toneko.common.mod.ai.provider.impl;

import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.providers.gemini.GeminiConfig;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;

/**
 * CNeko AI provider — custom service at chat.ai.cneko.org using Gemini-compatible protocol.
 */
public class CNekoAIProvider implements AIServiceProvider {

    @Override
    public String getProviderId() { return "neko"; }

    @Override
    public String getDisplayName() { return "CNeko AI"; }

    @Override
    public boolean isOpenAICompatible() { return false; }

    @Override
    public boolean requiresApiKey() { return true; }

    @Override
    public String getDefaultHost() { return "chat.ai.cneko.org"; }

    @Override
    public int getDefaultPort() { return 443; }

    @Override
    public String getDefaultEndpoint() { return "/"; }

    @Override
    public boolean isDefaultTls() { return true; }

    @Override
    public String getDefaultModel() { return "gemini-2.0-flash"; }

    @Override
    public AIResponse processRequest(AIServiceConfig config, AIRequest request) throws Exception {
        GeminiConfig geminiConfig = new GeminiConfig(config.getApiKey());
        geminiConfig.setModel(config.getModel());
        if (config.getProxy() != null) geminiConfig.setProxy(config.getProxy());
        if (config.getHost() != null && !config.getHost().isEmpty()) geminiConfig.setHost(config.getHost());
        CNekoAIService service = new CNekoAIService(geminiConfig, config);
        return service.processRequest(request);
    }
}
