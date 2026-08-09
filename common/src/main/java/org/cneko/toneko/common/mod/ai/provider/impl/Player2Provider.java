package org.cneko.toneko.common.mod.ai.provider.impl;

import org.cneko.ai.core.AIRequest;
import org.cneko.ai.core.AIResponse;
import org.cneko.ai.providers.openai.OpenAIConfig;
import org.cneko.ai.providers.openai.OpenAIService;
import org.cneko.toneko.common.mod.ai.AIServiceConfig;
import org.cneko.toneko.common.mod.ai.provider.AIServiceProvider;
import org.cneko.toneko.common.util.Player2Auth;
import org.cneko.toneko.common.util.TTSUtil;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Player2 provider — 本地 AI 应用（https://player2.game），OpenAI 兼容端点。
 * 与 custom 不同，Player2 是一等公民服务商：默认连接参数固定指向本机，
 * 端口动态发现（Player2 端口被占用时会自动换端口并写入 api.port 文件），
 * 无需 API key（用户在 Player2 App 内已认证），模型由 Player2 App 内选择（留空）。
 */
public class Player2Provider implements AIServiceProvider {

    @Override
    public String getProviderId() { return "player2"; }

    @Override
    public String getDisplayName() { return "Player2"; }

    @Override
    public boolean isOpenAICompatible() { return true; }

    @Override
    public boolean requiresApiKey() { return false; }

    @Override
    public String getDefaultHost() { return "127.0.0.1"; }

    /** Player2 端口动态发现：配置未显式指定 base_url 时，每次请求解析最新实际端口 */
    @Override
    public int getDefaultPort() { return TTSUtil.resolvePort(); }

    @Override
    public String getDefaultEndpoint() { return "/v1/chat/completions"; }

    @Override
    public boolean isDefaultTls() { return false; }

    /** 模型留空：使用 Player2 App 内用户选择的默认模型 */
    @Override
    public String getDefaultModel() { return ""; }

    /**
     * 构造 OpenAI 兼容配置：
     * - player2-game-key 来源标记头（归属统计/开发者奖励）
     * - p2Key 作为 Bearer token（计入用户统计 minutes/joule 的前提；
     *   拿不到时保持无认证直连——请求仍可用，只是不计入）
     */
    private OpenAIConfig buildOpenAIConfig(AIServiceConfig config) {
        String bearer = Player2Auth.getP2Key().orElse(config.getApiKey());
        return OpenAIProvider.applyCommon(
                new OpenAIConfig(bearer).withModel(config.getModel())
                        .withHeaders(Map.of(TTSUtil.GAME_KEY_HEADER, TTSUtil.getGameKey())),
                config, getDefaultHost());
    }

    @Override
    public AIResponse processRequest(AIServiceConfig config, AIRequest request) throws Exception {
        OpenAIService service = new OpenAIService(buildOpenAIConfig(config));
        return service.processRequest(request);
    }

    @Override
    public boolean supportsStream() { return true; }

    @Override
    public Stream<AIResponse> processStream(AIServiceConfig config, AIRequest request) throws Exception {
        OpenAIService service = new OpenAIService(buildOpenAIConfig(config));
        return service.processStream(request);
    }
}
