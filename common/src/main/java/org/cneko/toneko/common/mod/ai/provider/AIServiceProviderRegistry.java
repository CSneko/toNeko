package org.cneko.toneko.common.mod.ai.provider;

import org.cneko.toneko.common.mod.ai.provider.impl.*;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Static registry that maps provider ID strings to AIServiceProvider instances.
 * Call {@link #init()} during mod bootstrap to register all built-in providers.
 */
public class AIServiceProviderRegistry {
    private static final Map<String, AIServiceProvider> PROVIDERS = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Register all built-in providers
        register(new OpenAIProvider());
        register(new GeminiProvider());
        register(new GroqProvider());
        register(new SiliconFlowProvider());
        register(new DeepSeekProvider());
        register(new OllamaProvider());
        register(new OpenRouterProvider());
        register(new MistralProvider());
        register(new AnthropicProvider());
        register(new LocalCustomProvider());
        register(new CNekoAIProvider());
    }

    /**
     * Register a provider. Throws if the provider ID is already registered.
     */
    public static void register(AIServiceProvider provider) {
        String id = provider.getProviderId().toLowerCase();
        if (PROVIDERS.containsKey(id)) {
            throw new IllegalStateException("AI provider already registered: " + id);
        }
        PROVIDERS.put(id, provider);
    }

    /**
     * Get a provider by its ID (case-insensitive).
     * Returns null if not found.
     */
    @Nullable
    public static AIServiceProvider get(String providerId) {
        if (providerId == null || providerId.isEmpty()) return null;
        return PROVIDERS.get(providerId.toLowerCase());
    }

    /**
     * Get all registered providers.
     */
    public static Collection<AIServiceProvider> getAll() {
        return PROVIDERS.values();
    }

    /**
     * Get all registered provider IDs.
     */
    public static Set<String> getIds() {
        return PROVIDERS.keySet();
    }

    /**
     * Check if a provider ID is registered.
     */
    public static boolean hasProvider(String providerId) {
        return providerId != null && PROVIDERS.containsKey(providerId.toLowerCase());
    }
}
