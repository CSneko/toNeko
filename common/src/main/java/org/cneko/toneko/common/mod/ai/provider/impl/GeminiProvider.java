package org.cneko.toneko.common.mod.ai.provider.impl;

import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.providers.gemini.GeminiConfig;
import org.cneko.ai.providers.gemini.GeminiService;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;

/**
 * Google Gemini provider.
 * IMPORTANT: Do NOT override host/endpoint on GeminiConfig unless the user has
 * explicitly set a custom base_url. GeminiConfig handles its own URL construction
 * internally (API key is passed as a query parameter ?key=xxx in the URL path).
 */
public class GeminiProvider implements AIServiceProvider {

    @Override
    public String getProviderId() { return "google"; }

    @Override
    public String getDisplayName() { return "Google Gemini"; }

    @Override
    public boolean isOpenAICompatible() { return false; }

    @Override
    public boolean requiresApiKey() { return true; }

    @Override
    public String getDefaultHost() { return "generativelanguage.googleapis.com"; }

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

        // Only override host if user explicitly set a custom base_url (different from default)
        String customHost = config.getHost();
        if (customHost != null && !customHost.isEmpty()
                && !customHost.equals(getDefaultHost())) {
            geminiConfig.setHost(customHost);
        }

        GeminiService service = new GeminiService(geminiConfig);
        return service.processRequest(request);
    }
}
