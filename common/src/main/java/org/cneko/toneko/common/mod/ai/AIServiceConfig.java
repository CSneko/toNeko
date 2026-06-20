package org.cneko.toneko.common.mod.ai;

import org.cneko.ai.core.NetworkingProxy;

/**
 * Bundles all per-request configuration for an AI service provider.
 */
public class AIServiceConfig {
    private final String providerId;
    private final String apiKey;
    private final String model;
    private final String host;
    private final int port;
    private final String endpoint;
    private final boolean tls;
    private final NetworkingProxy proxy;
    private final String prompt;
    private final boolean showThink;

    private AIServiceConfig(Builder builder) {
        this.providerId = builder.providerId;
        this.apiKey = builder.apiKey;
        this.model = builder.model;
        this.host = builder.host;
        this.port = builder.port;
        this.endpoint = builder.endpoint;
        this.tls = builder.tls;
        this.proxy = builder.proxy;
        this.prompt = builder.prompt;
        this.showThink = builder.showThink;
    }

    public String getProviderId() { return providerId; }
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getEndpoint() { return endpoint; }
    public boolean isTls() { return tls; }
    public NetworkingProxy getProxy() { return proxy; }
    public String getPrompt() { return prompt; }
    public boolean isShowThink() { return showThink; }

    public static Builder builder(String providerId) {
        return new Builder(providerId);
    }

    public static class Builder {
        private final String providerId;
        private String apiKey = "";
        private String model = "";
        private String host = "";
        private int port = 443;
        private String endpoint = "/";
        private boolean tls = true;
        private NetworkingProxy proxy;
        private String prompt = "";
        private boolean showThink = true;

        public Builder(String providerId) {
            this.providerId = providerId;
        }

        public Builder apiKey(String apiKey) { this.apiKey = apiKey != null ? apiKey : ""; return this; }
        public Builder model(String model) { this.model = model != null ? model : ""; return this; }
        public Builder host(String host) { this.host = host != null ? host : ""; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder endpoint(String endpoint) { this.endpoint = endpoint != null ? endpoint : "/"; return this; }
        public Builder tls(boolean tls) { this.tls = tls; return this; }
        public Builder proxy(NetworkingProxy proxy) { this.proxy = proxy; return this; }
        public Builder prompt(String prompt) { this.prompt = prompt != null ? prompt : ""; return this; }
        public Builder showThink(boolean showThink) { this.showThink = showThink; return this; }

        public AIServiceConfig build() {
            return new AIServiceConfig(this);
        }
    }
}
