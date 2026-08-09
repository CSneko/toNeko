package org.cneko.toneko.common.mod.client.events;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.cneko.toneko.common.mod.client.screens.ChatWithNekoScreen;
import org.cneko.toneko.common.mod.client.util.ClientConfig;
import org.cneko.toneko.common.mod.client.util.ClientTextUtil;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 猫娘头顶对话气泡（纯客户端渲染，不生成服务端实体）。
 * <p>
 * 数据流：客户端收到 {@code NekoChatDisplayPayload}（bubble 模式）→ {@link #show} 排版缓存 →
 * 每帧 {@link #render} 把实体头顶坐标投影到屏幕并绘制，显示 {@link #DURATION_MS} 后淡出。
 * <p>
 * 已知限制：潜水/岩浆（fov×0.857）、望远镜、疾跑时 fov 变化会导致投影位置有偏差；
 * 区块卸载重载后实体对象失联，气泡丢弃。
 */
public final class NekoBubbleRenderer {
    /** 显示时长与背景色读客户端配置（ClientConfig.ai.bubble.*），淡出时长固定 */
    private static final long FADE_MS = 1000;
    /** 气泡最大宽度（px），超出自动换行 */
    private static final int MAX_WIDTH = 220;
    private static final int BUBBLE_PAD = 4;
    /** 背景色不透明度（0~255），叠加在配置颜色上 */
    private static final int BG_ALPHA = 0x80;

    private record Bubble(Entity entity, List<FormattedCharSequence> lines, int width, int height, long expires) {}

    /** 所有显示中的气泡，按猫娘 UUID 索引；同一猫娘的新消息替换旧气泡。仅主线程访问 */
    private static final Map<UUID, Bubble> BUBBLES = new HashMap<>();
    /** 记录渲染时的维度，切换维度（世界重载）时清空全部气泡 */
    private static Level lastLevel = null;

    private NekoBubbleRenderer() {}

    /**
     * 显示一条气泡（客户端主线程调用）。
     *
     * @param nekoUuid 猫娘实体 UUID（找不到实体则丢弃）
     * @param rawText  带 § 格式码的显示文本
     */
    public static void show(String nekoUuid, String rawText) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || rawText == null || rawText.isEmpty()) return;
        UUID uuid;
        try {
            uuid = UUID.fromString(nekoUuid);
        } catch (IllegalArgumentException e) {
            return;
        }
        Entity entity = ClientNetworkEvents.findNearbyEntityByUuid(uuid, 128);
        if (entity == null) return;

        // 解析 § 码并按宽度换行，一次性缓存排版结果（ComponentRenderUtils 保留样式）
        List<FormattedCharSequence> wrapped = ComponentRenderUtils.wrapComponents(
                ClientTextUtil.parseLegacyFormatting(rawText), MAX_WIDTH, mc.font);
        int lineH = mc.font.lineHeight;
        int width = 0;
        for (FormattedCharSequence line : wrapped) {
            width = Math.max(width, mc.font.width(line));
        }
        int height = wrapped.size() * lineH;
        BUBBLES.put(uuid, new Bubble(entity, wrapped, width, height,
                System.currentTimeMillis() + ClientConfig.getBubbleDuration()));
    }

    /**
     * 渲染所有气泡（HudRenderCallback 每帧调用，客户端主线程）。
     * 聊天屏幕打开时不渲染（屏幕内已有打字机 + 历史，避免双重显示）。
     */
    public static void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        if (mc.screen instanceof ChatWithNekoScreen) return;
        if (mc.level == null) {
            BUBBLES.clear();
            lastLevel = null;
            return;
        }
        if (lastLevel != mc.level) {
            BUBBLES.clear();
            lastLevel = mc.level;
        }
        if (BUBBLES.isEmpty()) return;

        long now = System.currentTimeMillis();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        var it = BUBBLES.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Bubble b = entry.getValue();
            // 实体失效/跨维度/过期 → 移除
            if (b.entity().isRemoved() || b.entity().level() != mc.level || now >= b.expires()) {
                it.remove();
                continue;
            }
            int[] screen = project(mc, b.entity(), partialTick);
            if (screen == null) continue;

            // 最后 FADE_MS 内淡出
            long remain = b.expires() - now;
            int alpha = (int) (255 * Math.min(1.0, (float) remain / FADE_MS));
            // 背景：客户端配置的颜色 + 半透明，随淡出整体衰减；文字白色（行内样式颜色优先）
            int bgAlpha = (int) (BG_ALPHA * (alpha / 255.0f));
            int bg = (bgAlpha << 24) | (ClientConfig.getBubbleColor() & 0xFFFFFF);
            int textColor = (alpha << 24) | 0xFFFFFF;

            // 气泡底部贴着头顶点，向上展开
            int x = screen[0] - b.width() / 2 - BUBBLE_PAD;
            int y = screen[1] - b.height() - BUBBLE_PAD * 2;
            g.fill(x, y, x + b.width() + BUBBLE_PAD * 2, y + b.height() + BUBBLE_PAD * 2, bg);
            int ty = y + BUBBLE_PAD;
            for (FormattedCharSequence line : b.lines()) {
                g.drawString(mc.font, line, x + BUBBLE_PAD, ty, textColor);
                ty += mc.font.lineHeight;
            }
        }
    }

    /**
     * 世界坐标 → 屏幕坐标（1.21.1 相机空间约定：前向 = (0,0,-1) 经 rotation 旋转）。
     *
     * @return [屏幕x, 屏幕y]，目标在相机后方或远在屏外时返回 null
     */
    private static int[] project(Minecraft mc, Entity entity, float partialTick) {
        Camera camera = mc.gameRenderer.getMainCamera();
        // 头顶上方一点（气泡锚点）
        Vec3 pos = entity.getPosition(partialTick)
                .add(0, entity.getBbHeight() + 0.55, 0);
        Vec3 camPos = camera.getPosition();
        Vector3f rel = new Vector3f(
                (float) (pos.x - camPos.x),
                (float) (pos.y - camPos.y),
                (float) (pos.z - camPos.z));
        rel.rotate(camera.rotation().conjugate()); // 世界 → 相机空间
        if (rel.z >= -0.1F) return null;           // 相机后方/贴脸

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        float tanHalf = (float) Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
        float aspect = (float) sw / (float) sh;
        float ndcX = rel.x / (-rel.z * tanHalf * aspect);
        float ndcY = rel.y / (-rel.z * tanHalf);
        if (ndcX < -2.5F || ndcX > 2.5F || ndcY < -2.5F || ndcY > 2.5F) return null; // 屏外
        return new int[]{
                (int) ((ndcX * 0.5F + 0.5F) * sw),
                (int) ((0.5F - ndcY * 0.5F) * sh)
        };
    }
}
