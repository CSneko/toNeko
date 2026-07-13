package org.cneko.toneko.common.util;

import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProviderRegistry;

import java.nio.file.Path;
import java.time.LocalDate;

public class ConfigUtil {
    public static String CONFIG_FILE = "config/toneko.json";

    public static String AI_URL = "https://s.cneko.org/toNekoAI";

    public static final ConfigBuilder CONFIG_BUILDER = ConfigBuilder.create(Path.of(CONFIG_FILE))
            .addString("language", "zh_cn", null,
                    "语言选项（支持 zh_cn,zh_tw,en_us,ko_kr）,可自定义语言，详细查看 https://github.com/CSneko/toNeko/docs/CUSTOM_LANGUAGE.md",
                    "Language Option (Supported zh_cn,zh_tw,en_us,ko_kr), you can customize the language, see https://github.com/CSneko/toNeko/docs/CUSTOM_LANGUAGE.md")
            .addBoolean("chat.enable", true, null,
                    "是否启用聊天修改 （开启后会对MCDr造成一定影响）",
                    "Whether to enable chat modification (it will cause some effects on MCDr)" )
            .addString("chat.format","%prefix%§e%name% §d>> §f%msg%",null,
                    "聊天格式，占位符 %name% = 玩家名称, %msg% = 玩家消息, %prefix% = 前缀",
                    "Chat format, placeholder %name% = player name, %msg% = player message, %prefix% = prefix")
            // ===== AI 配置 =====
            .addBoolean("ai.enable",false,AI_URL,
                    "是否启用AI",
                    "Enable AI")
            .addString("ai.service","neko",AI_URL,
                    "AI服务商，支持: neko, openai, deepseek, google, groq, siliconflow, ollama, claude, openrouter, mistral, custom",
                    "AI provider, supports: neko, openai, deepseek, google, groq, siliconflow, ollama, claude, openrouter, mistral, custom")
            .addString("ai.key","",AI_URL,
                    "AI的API密钥（部分本地服务如ollama无需密钥）",
                    "API Key for the AI service (not needed for local services like ollama)")
            .addString("ai.model", "gemini-2.0-flash",AI_URL,
                    "使用的AI模型名称（留空则使用服务商默认模型）",
                    "AI model name (leave empty to use provider default)")
            .addString("ai.base_url","",AI_URL,
                    "自定义API地址（如 http://localhost:11434 用于Ollama，留空则使用默认地址）",
                    "Custom API base URL (e.g. http://localhost:11434 for Ollama, leave empty for default)")
            .addString("ai.prompt","你是一只可爱的%neko_type%（%neko_des%），你的名字是%neko_name%，你的身高是%neko_height%米，你的属性有%neko_moe_tags%。在回复时请不要太长且不要有太多动作。我%player_is_owner%你的主人，我的名字是%player_name%，我%player_is_neko%一只猫娘。现在是%world_time%，天气为%world_weather%。",AI_URL,
                    "AI提示词（支持占位符%neko_name%/%player_name%/%world_time%等），参阅 https://s.cneko.org/toNekoAI",
                    "AI prompt (supports placeholders like %neko_name%/%player_name%/%world_time%), see https://s.cneko.org/toNekoAI")
            .addBoolean("ai.show_think",true,AI_URL,
                    "是否显示AI思考过程",
                    "Whether to show AI thinking process")
            .addString("ai.chat_prefix","",AI_URL,
                    "自然聊天触发前缀（如 @neko），留空禁用。在聊天中以该前缀开头的消息会发送给最近的猫娘。",
                    "Natural chat trigger prefix (e.g. @neko), leave empty to disable.")
            .addBoolean("ai.debug",false,AI_URL,
                    "启用AI调试日志（请求/响应详情输出到控制台和日志文件，用于排查问题）",
                    "Enable AI debug logging (request/response details output to console and log file, for troubleshooting)")
            // TTS
            .addBoolean("ai.tts.enable",false, AI_URL,
                    "是否启用TTS语音合成",
                    "Enable TTS")
            .addString("ai.tts.service","player2",AI_URL,
                    "TTS服务商",
                    "TTS service provider")
            .addString("ai.tts.voice","01955d76-ed5b-75ad-afe3-ac5eb3d0a16e",AI_URL,
                    "TTS语音ID",
                    "TTS voice ID")
            // Proxy
            .addBoolean("ai.proxy.enable",false,AI_URL,
                    "是否启用AI代理",
                    "Enable AI proxy")
            .addString("ai.proxy.ip","",AI_URL,
                    "代理IP地址",
                    "Proxy IP address")
            .addString("ai.proxy.port","2080",AI_URL,
                    "代理端口",
                    "Proxy port")
            // Stats
            // ===== Fly Sword 配置 =====
            .addBoolean("fly_sword.enable", true, null,
                    "启用御剑飞行",
                    "Enable fly sword")
            .addFloat("fly_sword.fuel_multiplier", 1.0f, null,
                    "燃料动力倍率",
                    "Fuel power multiplier")
            .addFloat("fly_sword.mass_multiplier", 1.0f, null,
                    "质量倍率（影响惯性和碰撞）",
                    "Mass multiplier (affects inertia and collision)")
            .addFloat("fly_sword.speed_multiplier", 1.0f, null,
                    "速度倍率",
                    "Speed multiplier")
            .addFloat("fly_sword.damage_multiplier", 1.0f, null,
                    "伤害倍率",
                    "Damage multiplier")
            .addBoolean("fly_sword.tnt_enable", true, null,
                    "允许TNT御剑（撞击时产生爆炸）",
                    "Enable TNT fly sword (explodes on impact)")
            // Stats
            .addBoolean("stats", true, "https://s.cneko.org/toNekoOnlineAPI",
                    "启用统计功能，统计数据将发送到 toneko API，如何使用api请查看 https://s.cneko.org/toNekoOnlineAPI",
                    "Enable statistics, statistics data will be sent to the toneko API, how to use the api please see https://s.cneko.org/toNekoOnlineAPI")
            // ===== LoliHead 配置 =====
            .addBoolean("lolihead.enable", true, null,
                    "启用萝莉头功能（玩家缩小时自动调整头部大小）",
                    "Enable LoliHead feature (auto-adjust head size when player is scaled down)")
            .addBoolean("lolihead.algorithm.enable", true, null,
                    "启用动态算法模式（根据缩放比例自动补偿头部大小）",
                    "Enable dynamic algorithm mode (auto-compensate head size based on scale)")
            .addFloat("lolihead.algorithm.ratio", 1.0f, null,
                    "头部缩放比例（身体每缩小1倍，头部放大多少倍）",
                    "Head scale ratio (how much to enlarge head per body scale reduction)")
            .addFloat("lolihead.custom_head_scale.xScale", 1.0f, null,
                    "自定义头部X轴缩放",
                    "Custom head X-axis scale")
            .addFloat("lolihead.custom_head_scale.yScale", 1.0f, null,
                    "自定义头部Y轴缩放",
                    "Custom head Y-axis scale")
            .addFloat("lolihead.custom_head_scale.zScale", 1.0f, null,
                    "自定义头部Z轴缩放",
                    "Custom head Z-axis scale")
            .build();
    public static JsonConfiguration CONFIG = CONFIG_BUILDER.createConfig();

    public static boolean IS_BIRTHDAY = false;
    private static final int BIRTHDAY_MONTH = 9;
    private static final int BIRTHDAY_DAY = 26;
    public static boolean IS_FOOL_DAY = false;
    private static final int FOOL_DAY_MONTH = 4;
    private static final int FOOL_DAY_DAY = 1;

    /** Track the service at last save, so we can detect provider switches */
    private static String lastSavedService = null;

    public static void load(){
        CONFIG = CONFIG_BUILDER.createConfig();
        lastSavedService = CONFIG.getString("ai.service");
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();
        if (month == BIRTHDAY_MONTH && day == BIRTHDAY_DAY) {
            IS_BIRTHDAY = true;
        }
        if (month == FOOL_DAY_MONTH && day == FOOL_DAY_DAY) {
            IS_FOOL_DAY = true;
        }
        runAIConfigMigration();
        syncProviderConfigOnLoad();
        AIUtil.init();
    }

    // ===== General config =====
    public static boolean isChatEnable() {
        return CONFIG.getBoolean("chat.enable");
    }
    public static String getChatFormat() {
        return CONFIG.getString("chat.format");
    }
    public static boolean isStatsEnable() {
        return CONFIG.getBoolean("stats");
    }

    // ===== AI Config accessors =====
    public static boolean isAIEnabled() {
        return CONFIG.getBoolean("ai.enable");
    }
    public static String getAIPrompt() {
        return CONFIG.getString("ai.prompt");
    }
    public static boolean isAIShowThink(){
        return CONFIG.getBoolean("ai.show_think");
    }

    /** Get the current AI service provider ID. */
    public static String getAIService(){
        return CONFIG.getString("ai.service");
    }

    /** Get the API key for the active provider. */
    public static String getAIKey(){
        return CONFIG.getString("ai.key");
    }

    /** Get the model for the active provider. */
    public static String getAIModel(){
        return CONFIG.getString("ai.model");
    }

    /** Get the custom base URL (empty = use provider default). */
    public static String getAIBaseUrl(){
        return CONFIG.getString("ai.base_url");
    }

    /**
     * Get the API key for a specific provider.
     * Checks per-provider storage first, then flat key as fallback.
     */
    public static String getAIProviderKey(String providerId) {
        if (providerId == null || providerId.isEmpty()) return "";
        String key = CONFIG.getString("ai.providers." + providerId + ".key");
        if (key != null && !key.isEmpty()) return key;
        // Fallback: if asking for the active provider, return flat key
        if (providerId.equalsIgnoreCase(getAIService())) {
            return getAIKey();
        }
        return "";
    }

    /**
     * Get the model for a specific provider.
     */
    public static String getAIProviderModel(String providerId) {
        if (providerId == null || providerId.isEmpty()) return "";
        String model = CONFIG.getString("ai.providers." + providerId + ".model");
        if (model != null && !model.isEmpty()) return model;
        if (providerId.equalsIgnoreCase(getAIService())) {
            return getAIModel();
        }
        return "";
    }

    /**
     * Get the base_url for a specific provider.
     */
    public static String getAIProviderBaseUrl(String providerId) {
        if (providerId == null || providerId.isEmpty()) return "";
        String url = CONFIG.getString("ai.providers." + providerId + ".base_url");
        if (url != null && !url.isEmpty()) return url;
        if (providerId.equalsIgnoreCase(getAIService())) {
            return getAIBaseUrl();
        }
        return "";
    }

    /** Get the chat prefix for natural proximity chat. Empty = disabled. */
    public static String getAIChatPrefix() {
        return CONFIG.getString("ai.chat_prefix");
    }

    /** Whether AI debug logging is enabled. */
    public static boolean isAIDebugEnabled() {
        return CONFIG.getBoolean("ai.debug");
    }

    public static float getFlySwordFuelMultiplier()  { return clampConfig(CONFIG.getFloat("fly_sword.fuel_multiplier"), 1.0f); }
    public static float getFlySwordMassMultiplier()  { return clampConfig(CONFIG.getFloat("fly_sword.mass_multiplier"), 1.0f); }
    public static float getFlySwordSpeedMultiplier() { return clampConfig(CONFIG.getFloat("fly_sword.speed_multiplier"), 1.0f); }
    public static float getFlySwordDamageMultiplier(){ return clampConfig(CONFIG.getFloat("fly_sword.damage_multiplier"), 1.0f); }
    public static boolean isFlySwordEnabled()         { return CONFIG.getBoolean("fly_sword.enable"); }
    public static boolean isFlySwordTntEnabled()      { return CONFIG.getBoolean("fly_sword.tnt_enable"); }
    private static float clampConfig(float v, float def) { return v > 0 ? v : def; }

    /**
     * Save the current flat AI config to per-provider storage for the given provider ID.
     * Called when switching providers.
     */
    public static void saveProviderConfig(String providerId) {
        if (providerId == null || providerId.isEmpty()) return;
        String prefix = "ai.providers." + providerId + ".";
        CONFIG.set(prefix + "key", getAIKey());
        CONFIG.set(prefix + "model", getAIModel());
        CONFIG.set(prefix + "base_url", getAIBaseUrl());
    }

    /**
     * Load per-provider config into flat keys for the given provider ID.
     * Always clears flat keys first to avoid carrying over old provider values,
     * then sets them only if the new provider has saved data.
     */
    public static void loadProviderConfig(String providerId) {
        if (providerId == null || providerId.isEmpty()) return;
        // Always clear flat keys — blank providers should start blank
        CONFIG.set("ai.key", "");
        CONFIG.set("ai.model", "");
        CONFIG.set("ai.base_url", "");
        // Then load saved values for this specific provider
        String key = CONFIG.getString("ai.providers." + providerId + ".key");
        String model = CONFIG.getString("ai.providers." + providerId + ".model");
        String baseUrl = CONFIG.getString("ai.providers." + providerId + ".base_url");
        if (key != null && !key.isEmpty()) CONFIG.set("ai.key", key);
        if (model != null && !model.isEmpty()) CONFIG.set("ai.model", model);
        if (baseUrl != null && !baseUrl.isEmpty()) CONFIG.set("ai.base_url", baseUrl);
    }

    /**
     * Call this after ConfigScreen save or config reload.
     * Syncs flat keys ↔ per-provider storage.
     */
    public static void syncConfigAfterSave() {
        String currentService = getAIService();
        // Save current settings to current provider's storage
        if (currentService != null && !currentService.isEmpty()) {
            saveProviderConfig(currentService);
        }
        // If provider changed, load new provider's saved settings
        if (lastSavedService != null && !lastSavedService.isEmpty()
                && !lastSavedService.equalsIgnoreCase(currentService)) {
            loadProviderConfig(currentService);
        }
        lastSavedService = currentService;
    }

    // ===== Legacy AI accessors (kept for backward compat) =====

    public static boolean isAIProxyEnabled(){
        return CONFIG.getBoolean("ai.proxy.enable");
    }
    public static String getAIProxyIp(){
        return CONFIG.getString("ai.proxy.ip");
    }
    public static String getAIProxyPort(){
        return CONFIG.getString("ai.proxy.port");
    }

    // ===== TTS =====
    public static boolean isAITTSEnabled(){
        return CONFIG.getBoolean("ai.tts.enable");
    }
    public static String getAITTSVoice(){
        return CONFIG.getString("ai.tts.voice");
    }

    /**
     * Build a complete AIServiceConfig for the given provider.
     * Reads from flat keys (which are synced with per-provider storage).
     */
    public static AIServiceConfig buildAIServiceConfig(String providerId) {
        AIServiceProvider provider = AIServiceProviderRegistry.get(providerId);
        if (provider == null) {
            provider = AIServiceProviderRegistry.get("custom");
        }

        AIServiceConfig.Builder builder = AIServiceConfig.builder(providerId);

        // API key: from per-provider storage first, then flat key
        String perProviderKey = CONFIG.getString("ai.providers." + providerId + ".key");
        String flatKey = CONFIG.getString("ai.key");
        String key = getAIProviderKey(providerId);
        if (key == null || key.isEmpty()) key = flatKey;

        if (isAIDebugEnabled()) {
            Bootstrap.LOGGER.info("[AI-DEBUG] Config resolver: providerId={} ai.key(len)={} ai.providers.{}.key(len)={} resolvedKey(len)={}",
                    providerId,
                    flatKey != null ? flatKey.length() : 0,
                    providerId, perProviderKey != null ? perProviderKey.length() : 0,
                    key != null ? key.length() : 0);
        }

        // Trim to remove any invisible characters (newlines, spaces) from config file
        if (key != null) key = key.trim();
        builder.apiKey(key);

        if (isAIDebugEnabled() && key != null && !key.isEmpty()) {
            StringBuilder hex = new StringBuilder();
            String sample = key.length() <= 8 ? key : key.substring(0, 4) + ".." + key.substring(key.length() - 4);
            for (char c : sample.toCharArray()) {
                hex.append(String.format("%02x ", (int) c));
            }
            Bootstrap.LOGGER.info("[AI-DEBUG] Key hex sample (first 4 + last 4): \"{}\" -> [{}]", sample, hex.toString().trim());
        }

        // Model
        String perProviderModel = CONFIG.getString("ai.providers." + providerId + ".model");
        String flatModel = CONFIG.getString("ai.model");
        String model = getAIProviderModel(providerId);
        if (model == null || model.isEmpty()) model = flatModel;
        if ((model == null || model.isEmpty()) && provider != null) {
            model = provider.getDefaultModel();
        }
        builder.model(model);

        // Connection params: set provider defaults (for logging), providers only apply
        // them to the underlying OpenAIConfig if they differ from library defaults.
        if (provider != null) {
            String baseUrl = getAIProviderBaseUrl(providerId);
            if (baseUrl == null || baseUrl.isEmpty()) baseUrl = getAIBaseUrl();
            if (baseUrl != null && !baseUrl.isEmpty()) {
                parseAndApplyBaseUrl(builder, baseUrl);
            } else {
                // Store defaults on AIServiceConfig for debug/logging, but
                // the provider won't forward them to OpenAIConfig unless overridden.
                builder.host(provider.getDefaultHost());
                builder.port(provider.getDefaultPort());
                builder.endpoint(provider.getDefaultEndpoint());
                builder.tls(provider.isDefaultTls());
            }
        }

        builder.prompt(getAIPrompt());
        builder.showThink(isAIShowThink());

        // Proxy
        if (isAIProxyEnabled()) {
            String proxyIp = getAIProxyIp();
            String proxyPort = getAIProxyPort();
            if (proxyIp != null && !proxyIp.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
                try {
                    builder.proxy(new org.cneko.ai.core.NetworkingProxy(proxyIp, Integer.parseInt(proxyPort)));
                } catch (Exception ignored) {}
            }
        }

        return builder.build();
    }

    private static void parseAndApplyBaseUrl(AIServiceConfig.Builder builder, String url) {
        boolean tls = true;
        if (url.startsWith("http://")) {
            url = url.substring("http://".length());
            tls = false;
        } else if (url.startsWith("https://")) {
            url = url.substring("https://".length());
        }
        String[] parts = url.split("/", 2);
        String hostPort = parts[0];
        String endpoint = parts.length > 1 ? "/" + parts[1] : "/";
        int colonIdx = hostPort.indexOf(':');
        if (colonIdx != -1) {
            builder.host(hostPort.substring(0, colonIdx));
            builder.port(Integer.parseInt(hostPort.substring(colonIdx + 1)));
        } else {
            builder.host(hostPort);
            builder.port(tls ? 443 : 80);
        }
        builder.endpoint(endpoint);
        builder.tls(tls);
    }

    // ===== LoliHead 配置访问方法 =====
    public static boolean isLoliHeadEnabled() {
        return CONFIG.getBoolean("lolihead.enable");
    }
    public static boolean isLoliHeadAlgorithmEnabled() {
        return CONFIG.getBoolean("lolihead.algorithm.enable");
    }
    public static float getLoliHeadAlgorithmRatio() {
        return CONFIG.getFloat("lolihead.algorithm.ratio");
    }
    public static float getLoliHeadCustomXScale() {
        return CONFIG.getFloat("lolihead.custom_head_scale.xScale");
    }
    public static float getLoliHeadCustomYScale() {
        return CONFIG.getFloat("lolihead.custom_head_scale.yScale");
    }
    public static float getLoliHeadCustomZScale() {
        return CONFIG.getFloat("lolihead.custom_head_scale.zScale");
    }

    // ===== Migration & sync =====

    /**
     * One-time migration: if old flat legacy keys exist but no per-provider storage,
     * save the current config to per-provider storage.
     */
    private static void runAIConfigMigration() {
        String service = CONFIG.getString("ai.service");
        if (service == null || service.isEmpty()) return;

        String providerKey = CONFIG.getString("ai.providers." + service + ".key");
        if (providerKey != null && !providerKey.isEmpty()) return; // Already migrated

        // Save current flat config to per-provider storage
        saveProviderConfig(service);
        try {
            CONFIG.save(Path.of(CONFIG_FILE));
        } catch (Exception ignored) {}
    }

    /**
     * On load, if the current service has saved per-provider config, load it into flat keys.
     */
    private static void syncProviderConfigOnLoad() {
        String service = getAIService();
        if (service == null || service.isEmpty()) return;

        String savedKey = getAIProviderKey(service);
        String savedModel = getAIProviderModel(service);

        // If per-provider storage has data, use it to populate flat keys
        if (savedKey != null && !savedKey.isEmpty()) {
            if (getAIKey() == null || getAIKey().isEmpty()) {
                CONFIG.set("ai.key", savedKey);
            }
        }
        if (savedModel != null && !savedModel.isEmpty()) {
            if (getAIModel() == null || getAIModel().isEmpty() || getAIModel().equals("gemini-2.0-flash")) {
                CONFIG.set("ai.model", savedModel);
            }
        }
    }

}
