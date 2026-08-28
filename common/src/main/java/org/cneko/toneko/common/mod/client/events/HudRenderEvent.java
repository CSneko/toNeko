package org.cneko.toneko.common.mod.client.events;
import org.cneko.toneko.common.mod.entities.INeko;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.client.ToNekoKeyBindings;
import org.cneko.toneko.common.mod.client.api.GiftSelectionManager;
import org.cneko.toneko.common.mod.client.events.ClientTickEvent;
import org.cneko.toneko.common.mod.entities.FlySwordEntity;
import org.cneko.toneko.common.mod.effects.ToNekoEffects;

import java.io.IOException;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

public class HudRenderEvent {

    public static void init() {
        // 26.x Fabric：HudRenderCallback 已移除，改用 HudElementRegistry 注册 HUD 元素
        HudElementRegistry.addLast(toNekoLoc("hud"), (HudElement) (guiGraphics, deltaTracker) -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            renderNekoEnergyBar(guiGraphics);
            // 潜行模式指示器
            if (ClientTickEvent.isStealthActive()) {
                renderStealthIndicator(guiGraphics);
            }
            // 检查玩家是否有魅惑效果
            if (player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ToNekoEffects.BEWITCHED_EFFECT))) {
                renderBewitchedOverlay(guiGraphics,player);
            }
            // 玩家被骑乘时显示提示
            if (!player.getPassengers().isEmpty()) {
                renderDismountHint(guiGraphics);
            }
            // 趴下/躺下时显示起身提示（摔倒趴下、/neko lie、/neko getDown 共用）
            // 姿势现在由原版 DATA_POSE 同步 + 客户端预测对齐，直接读真实姿势即可
            Pose lyingPose = player.getPose();
            if (lyingPose == Pose.SWIMMING || lyingPose == Pose.SLEEPING) {
                renderStandUpHint(guiGraphics);
            }
            if (player.getVehicle() instanceof FlySwordEntity flySword) {
                renderFlySwordHUD(guiGraphics, flySword);
            }
            // 送礼选择模式：快捷栏高亮 + 提示
            if (GiftSelectionManager.isActive()) {
                renderGiftSelection(guiGraphics);
            }
            // AI 回复头顶气泡（bubble 模式下由 NekoChatDisplayPayload 触发）
            NekoBubbleRenderer.render(guiGraphics, deltaTracker);
        });
    }

    /**
     * 渲染魅惑效果的粉色晕影覆盖层。
     * 此效果没有纹理，而是通过在屏幕边缘绘制带颜色梯度的顶点来动态生成。
     *
     * @param guiGraphics GuiGraphicsExtractor 实例
     * @param player      玩家实体
     */
    private static void renderBewitchedOverlay(GuiGraphicsExtractor guiGraphics, Player player) {
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        // 26.x 迁移：核心着色器/Tesselator 直绘已被移除，
        // 粉色晕影改用提取器自身的渐变/填充实现（视觉近似）。
        int r = 0xFF, g2 = 105, b = 180; // 靓粉色
        int edge = (120 << 24) | (r << 16) | (g2 << 8) | b;
        int clear = b & 0xFFFFFF;

        float innerY1 = height * 0.25f;
        float innerY2 = height * 0.75f;
        // 顶部：边缘色 → 透明
        guiGraphics.fillGradient(0, 0, width, (int) innerY1, edge, clear);
        // 底部：透明 → 边缘色
        guiGraphics.fillGradient(0, (int) innerY2, width, height, clear, edge);
        // 左右两侧近似为常量 alpha 填充
        guiGraphics.fill(0, 0, (int) (width * 0.25f), height, edge);
        guiGraphics.fill((int) (width * 0.75f), 0, width, height, edge);
    }




    /**
     * 当玩家被骑乘时，在屏幕右侧显示按键提示
     */
    private static void renderDismountHint(GuiGraphicsExtractor guiGraphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        Component hint = Component.translatable("hint.toneko.dismount_passenger",
                ToNekoKeyBindings.DISMOUNT_PASSENGER_KEY.getTranslatedKeyMessage());

        int textWidth = client.font.width(hint);
        int x = width - textWidth - 10;
        int y = height / 2 - 20;

        guiGraphics.text(client.font, hint, x, y, 0xAAFFFFFF);
    }

    /** 趴下/躺下时右侧的起身提示（与下马提示同一位置，互斥不会同时出现） */
    private static void renderStandUpHint(GuiGraphicsExtractor guiGraphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();

        Component hint = Component.translatable("hint.toneko.stand_up",
                client.options.keyShift.getTranslatedKeyMessage());

        int textWidth = client.font.width(hint);
        int x = width - textWidth - 10;
        int y = height / 2 - 20;

        guiGraphics.text(client.font, hint, x, y, 0xAAFFFFFF);
    }

    /** 送礼选择模式：金色描边高亮当前选中的快捷栏槽 + 快捷栏上方提示 */
    private static void renderGiftSelection(GuiGraphicsExtractor guiGraphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui || client.player == null) return;

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int slot = client.player.getInventory().getSelectedSlot();

        // 原版快捷栏布局：left = width/2 - 91；选中槽高亮 24x23，从 (left-1, height-23) 开始
        int left = width / 2 - 91;
        int x = left - 1 + slot * 20;
        int y = height - 23;

        guiGraphics.outline(x, y, 24, 23, 0xFFFFD700);

        Component hint = GiftSelectionManager.hint();
        guiGraphics.centeredText(client.font, hint, width / 2, y - 12, 0xFFFFFFFF);
    }

    private static final Identifier CATNIP_ICON = Identifier.fromNamespaceAndPath(MODID,"textures/item/catnip.png");
    public static void renderNekoEnergyBar(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui) return;

        Player player = client.player;

        float nekoEnergy = ((INeko) player).getNekoEnergy();
        float maxNekoEnergy = ((INeko) player).getMaxNekoEnergy();

        // 如果能量是满的，则隐藏
        if (nekoEnergy >= maxNekoEnergy) return;

        // 获取屏幕尺寸
        int width = context.guiWidth();
        int height = context.guiHeight();

        // 设置位置和尺寸
        int barWidth = 91;
        int barHeight = 4;
        int iconSize = 8;
        int margin = 10;
        int x = width - margin - barWidth;
        int y = height - margin - barHeight;

        // 计算能量百分比
        float percent = nekoEnergy / maxNekoEnergy;
        int energyWidth = (int)(barWidth * percent);

        // 绘制背景条
        context.fill(
                x, y,
                x + barWidth, y + barHeight,
                0x80000000 // 半透明黑色背景
        );

        // 绘制渐变能量条（绿色调）
        for (int i = 0; i < energyWidth; i++) {
            float progress = (float)i / barWidth;
            // 渐变从暗绿色(100,200,0)到亮绿色(150,255,100)
            int red = (int)(100 * (1 - progress) + 150 * progress);
            int green = (int)(200 * (1 - progress) + 255 * progress);
            int blue = (int)(0 * (1 - progress) + 100 * progress);
            int color = (0xFF << 24) | (red << 16) | (green << 8) | blue;

            context.fill(
                    x + i, y,
                    x + i + 1, y + barHeight,
                    color
            );
        }

        // 绘制猫薄荷图标（在能量条左侧）
        context.blit(CATNIP_ICON,
                x - iconSize - 2,  // 放在能量条左边，间隔2像素
                y - (iconSize - barHeight) / 2,
                0, 0,
                iconSize, iconSize,
                iconSize, iconSize);
    }

    /** 潜行模式指示器：屏幕右下角能量条上方显示状态文字 */
    private static void renderStealthIndicator(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui || client.player == null) return;

        int width = context.guiWidth();
        int height = context.guiHeight();
        int margin = 10;

        boolean crouching = client.player.isCrouching();
        String text = crouching
                ? Component.translatable("hud.toneko.stealth.active").getString()
                : Component.translatable("hud.toneko.stealth.idle").getString();
        int color = crouching ? 0xFF55FF55 : 0xFFAAAAAA;

        int textWidth = client.font.width(text);
        int x = width - margin - textWidth;
        int y = height - margin - 12; // 能量条上方

        context.text(client.font, text, x, y, color);
    }

    private static void renderFlySwordHUD(GuiGraphicsExtractor g, FlySwordEntity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui) return;

        int barW = 100, barH = 6, x = g.guiWidth() / 2 - barW / 2;

        // Fuel gauge — percentage of max fuel
        int fuelY = g.guiHeight() - 62;
        int maxFuel = Math.max(entity.getMaxFuelTicks(), 1);
        float fp = entity.getFuelTicks() > 0 ? Math.clamp((float) entity.getFuelTicks() / maxFuel, 0, 1) : 0;
        g.fill(x, fuelY, x + barW, fuelY + barH, 0x80000000);
        if (fp > 0) g.fill(x, fuelY, x + (int)(barW * fp), fuelY + barH, 0xFFFF8800);
        String ft = entity.getFuelTicks() > 0
                ? String.format("§6Fuel: %d%%", (int)(fp * 100))
                : "§7Fuel: --";
        g.centeredText(client.font, ft, g.guiWidth() / 2, fuelY - 10, 0xFFFFFFFF);

        // Speed bar — % of current max speed (synced from server)
        int spdY = g.guiHeight() - 46;
        double speed = entity.getClientSpeed();
        double speedMax = Math.max(entity.getSyncedMaxSpeed(), 0.1);
        float spd = Math.clamp((float) (speed / speedMax), 0, 1);

        g.fill(x, spdY, x + barW, spdY + barH, 0x80000000);
        if (spd > 0) {
            int c = spd > 0.8f ? 0xFF55FF55 : spd > 0.4f ? 0xFFFFFF55 : 0xFFFF5555;
            g.fill(x, spdY, x + (int)(barW * spd), spdY + barH, c);
        }
        g.centeredText(client.font, String.format("§f%.1f m/s", speed),
                g.guiWidth() / 2, spdY + barH + 2, 0xFFFFFF);
    }
}
