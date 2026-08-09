package org.cneko.toneko.common.mod.ai.provider;

import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;

import java.util.stream.Stream;

/**
 * Abstraction for an AI service provider (OpenAI, Gemini, DeepSeek, Claude, Ollama, etc.).
 * Each provider knows its default connection details and how to construct the appropriate
 * NekoAI library service to process a request.
 */
public interface AIServiceProvider {

    /** Unique identifier, e.g. "openai", "deepseek", "claude". Used in config and commands. */
    String getProviderId();

    /** Human-readable display name, e.g. "OpenAI", "DeepSeek", "Anthropic Claude". */
    String getDisplayName();

    /** Whether this provider uses the OpenAI-compatible /v1/chat/completions API format. */
    boolean isOpenAICompatible();

    /** Whether this provider requires an API key (Ollama does not). */
    boolean requiresApiKey();

    /** Default host / domain, e.g. "api.openai.com". */
    String getDefaultHost();

    /** Default port (443 for TLS, 80 for plain, 11434 for Ollama). */
    int getDefaultPort();

    /** Default endpoint path, e.g. "/v1/chat/completions". */
    String getDefaultEndpoint();

    /** Whether TLS is used by default. */
    boolean isDefaultTls();

    /** Recommended default model for this provider. */
    String getDefaultModel();

    /**
     * Process the AI request using this provider's specific service implementation.
     * @param config complete configuration for this request
     * @param request the AI request containing message, history, prompt, etc.
     * @return the AI response
     * @throws Exception if the request fails
     */
    AIResponse processRequest(AIServiceConfig config, AIRequest request) throws Exception;

    /**
     * Whether this provider can stream responses (SSE). Providers that cannot stream
     * fall back to the default single-element stream via {@link #processStream}.
     */
    default boolean supportsStream() {
        return false;
    }

    /**
     * Process the AI request and return an incremental stream of response chunks
     * (each element is a delta of text; the full response is their concatenation).
     * Connection/status errors are thrown before the stream is returned; mid-stream
     * errors surface as AIStreamException during iteration. The default implementation
     * falls back to a single-element stream of the complete response.
     */
    default Stream<AIResponse> processStream(AIServiceConfig config, AIRequest request) throws Exception {
        return Stream.of(processRequest(config, request));
    }
}
