package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.api.NekoLevelRegistry;
import org.cneko.toneko.common.mod.entities.INeko;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.mod.util.TextUtil.translatable;

public class NekoInfoScreen extends Screen {
    // ================= 可爱风调色板 =================
    private static final int PANEL_BG_COLOR = 0xE625252D;      // 半透明深灰偏紫 (背景)
    private static final int BORDER_COLOR = 0xFFFFAAE6;        // 柔和粉色 (边框)

    private static final int TEXT_TITLE = 0xFFFF77CC;          // 亮粉色 (大标题)
    private static final int TEXT_SECTION = 0xFF88DDFF;        // 糖果蓝 (区块标题)
    private static final int TEXT_LABEL = 0xFFCCCCCC;          // 浅灰 (标签名)
    private static final int TEXT_VALUE = 0xFFFFFFAA;          // 奶油黄 (具体数值)
    private static final int TEXT_SUB_VALUE = 0xFFAAFFCC;      // 薄荷绿 (次要数值/系数)

    private static final int BAR_ENERGY_BG = 0xFF443344;       // 能量条背景
    private static final int BAR_ENERGY_FG = 0xFFFF66AA;       // 能量条前景 (草莓粉)
    private static final int BAR_AGE_BG = 0xFF334444;          // 年龄条背景
    private static final int BAR_AGE_FG = 0xFF66CCFF;          // 年龄条前景 (天空蓝)

    // 面板尺寸 (为了放下左侧所有内容，稍微加高了面板)
    private final int panelWidth = 290;
    private final int panelHeight = 263;

    protected NekoInfoScreen() {
        super(translatable("screen.toneko.neko_info.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new NekoInfoScreen());
    }

    @Override
    protected void init() {
        int startX = (this.width - panelWidth) / 2;
        int startY = (this.height - panelHeight) / 2;

        // 底部居中按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.back"),
                button -> this.onClose()
        ).bounds(startX + panelWidth / 2 - 95, startY + panelHeight - 28, 90, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.toneko.neko_info.management"),
                button -> {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.connection.sendUnsignedCommand("toneko gui");
                    }
                }
        ).bounds(startX + panelWidth / 2 + 5, startY + panelHeight - 28, 90, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta); // 绘制暗色遮罩

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // 计算面板位置
        int startX = (this.width - panelWidth) / 2;
        int startY = (this.height - panelHeight) / 2;

        // 1. 绘制圆角主面板与粉色边框
        drawRoundedPanel(guiGraphics, startX, startY, panelWidth, panelHeight);

        // 2. 居中绘制总标题
        Component cuteTitle = Component.literal("❤ ").append(this.title).append(" ❤");
        int titleWidth = this.font.width(cuteTitle);
        guiGraphics.drawString(this.font, cuteTitle, startX + (panelWidth - titleWidth) / 2, startY + 12, TEXT_TITLE, true);

        // ==========================================
        // 左半区：模型、身份、年龄、能量
        // ==========================================
        int leftColX = startX + 15;
        int leftCenterX = startX + 65; // 左列的中心X坐标
        int leftY = startY + 35;

        // 渲染玩家 3D 模型 (看着鼠标)
        // 参数解释: 渲染框左上角X,Y, 右下角X,Y, 缩放大小, y偏移, 鼠标X, 鼠标Y, 实体
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics,
                leftColX, leftY, leftColX + 100, leftY + 65, 40, 0.0625F, mouseX, mouseY, player);

        leftY += 72; // 移动到模型下方

        // 名字与状态
        String nick = player.getNickName().isEmpty() ? player.getName().getString() : player.getNickName();
        guiGraphics.drawCenteredString(this.font, nick, leftCenterX, leftY, TEXT_VALUE);
        leftY += 12;

        Component statusText = translatable("screen.toneko.neko_info.status",
                translatable(player.isNeko() ? "screen.toneko.neko_info.yes" : "screen.toneko.neko_info.no"));
        guiGraphics.drawCenteredString(this.font, statusText, leftCenterX, leftY, TEXT_LABEL);
        leftY += 16;

        if (!player.isNeko()) {
            guiGraphics.drawCenteredString(this.font, translatable("screen.toneko.neko_info.not_neko"),
                    leftCenterX, leftY, 0xFF5555);
            return; // 如果不是猫娘，中止后续绘制
        }

        // 年龄 & 成长 (移到左侧)
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.age_section"), leftColX, leftY, TEXT_SECTION, true);
        leftY += 12;

        double ageScale = player.getNekoAgeScale();
        int growthPercent = (int) Math.round((ageScale - 0.3) / 0.7 * 100);
        growthPercent = Math.max(0, Math.min(100, growthPercent));

        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.apparent_age", getApparentAge(ageScale)), leftColX + 5, leftY, TEXT_VALUE, false);
        leftY += 12;

        // 年龄进度条
        drawProgressBar(guiGraphics, leftColX + 5, leftY, 90, 6, growthPercent / 100.0f, BAR_AGE_BG, BAR_AGE_FG);
        guiGraphics.drawString(this.font, Component.literal(growthPercent + "%"), leftColX + 100, leftY - 1, 0xFFFFFF);
        leftY += 16;

        // 能量状态 (移到左侧)
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.energy_section"), leftColX, leftY, TEXT_SECTION, true);
        leftY += 12;

        float energy = player.getNekoEnergy();
        float maxEnergy = player.getMaxNekoEnergy();

        // 能量进度条
        drawProgressBar(guiGraphics, leftColX + 5, leftY, 90, 8, energy / maxEnergy, BAR_ENERGY_BG, BAR_ENERGY_FG);
        String energyStr = String.format("%.0f/%.0f", energy, maxEnergy);

        // 调整能量文字的位置，稍微缩小字距或右移防止重叠
        guiGraphics.drawString(this.font, Component.literal(energyStr), leftColX + 98, leftY, 0xFFFFFF);


        // ==========================================
        // 右半区：等级详细属性、能力
        // ==========================================
        int rightColX = startX + 145; // 右列起始X (留出足够的中间空隙)
        int rightY = startY + 35;
        int lineHeight = 14;

        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.level_section"), rightColX, rightY, TEXT_SECTION, true);
        rightY += lineHeight;

        float totalLevel = player.getNekoLevel();
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.total_level", String.format("%.1f", totalLevel)), rightColX + 5, rightY, TEXT_VALUE, false);
        rightY += lineHeight + 4; // 额外空隙

        // 回归垂直列表，彻底解决文字重叠问题
        int indentX = rightColX + 12; // 缩进显示子项

        drawFactor(guiGraphics, "base", indentX, rightY, player);
        rightY += lineHeight;

        drawFactor(guiGraphics, "interaction", indentX, rightY, player);
        rightY += lineHeight;

        drawFactor(guiGraphics, "combat", indentX, rightY, player);
        rightY += lineHeight;

        drawFactor(guiGraphics, "exploration", indentX, rightY, player);
        rightY += lineHeight;

        drawFactor(guiGraphics, "fishing", indentX, rightY, player);
        rightY += lineHeight;

        drawFactor(guiGraphics, "homestead", indentX, rightY, player);
        rightY += lineHeight + 12;

        // 能力
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.ability_section"), rightColX, rightY, TEXT_SECTION, true);
        rightY += lineHeight;
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.ability", player.getNekoAbility()), rightColX + 5, rightY, TEXT_VALUE, false);

        // 最后渲染按钮 (调用父类)
        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    /**
     * 绘制带边框的圆角风格背景板
     */
    private void drawRoundedPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 2, y, x + width - 2, y + height, PANEL_BG_COLOR);
        graphics.fill(x, y + 2, x + width, y + height - 2, PANEL_BG_COLOR);

        graphics.fill(x + 2, y - 1, x + width - 2, y, BORDER_COLOR);
        graphics.fill(x + 2, y + height, x + width - 2, y + height + 1, BORDER_COLOR);
        graphics.fill(x - 1, y + 2, x, y + height - 2, BORDER_COLOR);
        graphics.fill(x + width, y + 2, x + width + 1, y + height - 2, BORDER_COLOR);

        graphics.fill(x, y, x + 2, y + 2, BORDER_COLOR);
        graphics.fill(x + width - 2, y, x + width, y + 2, BORDER_COLOR);
        graphics.fill(x, y + height - 2, x + 2, y + height, BORDER_COLOR);
        graphics.fill(x + width - 2, y + height - 2, x + width, y + height, BORDER_COLOR);
    }

    /**
     * 绘制可爱的进度条
     */
    private void drawProgressBar(GuiGraphics graphics, int x, int y, int width, int height, float progress, int bgColor, int fgColor) {
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        int fillWidth = (int) (width * progress);

        graphics.fill(x, y, x + width, y + height, bgColor);
        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + height, fgColor);
        }
        graphics.fill(x, y, x + fillWidth, y + 1, 0x33FFFFFF); // 顶部高光
    }

    /**
     * 封装属性绘制
     */
    private void drawFactor(GuiGraphics graphics, String type, int x, int y, Player player) {
        double raw = player.getNekoLevelFactorRaw(type);
        double level = 0.0;

        switch (type) {
            case "base": level = NekoLevelRegistry.base().getLevel(raw); break;
            case "interaction": level = NekoLevelRegistry.interaction().getLevel(raw); break;
            case "combat": level = NekoLevelRegistry.combat().getLevel(raw); break;
            case "exploration": level = NekoLevelRegistry.exploration().getLevel(raw); break;
            case "fishing": level = NekoLevelRegistry.fishing().getLevel(raw); break;
            case "homestead": level = NekoLevelRegistry.homestead().getLevel(raw); break;
        }

        Component text = translatable("screen.toneko.neko_info." + type + "_factor",
                String.format("%.1f", level), String.format("%.0f", raw));
        graphics.drawString(this.font, text, x, y, TEXT_SUB_VALUE, false);
    }

    private static int getApparentAge(double ageScale) {
        if (ageScale >= 1.0) return 18;
        if (ageScale <= 0.3) return 6;
        return 6 + (int) Math.round((ageScale - 0.3) / 0.7 * 12);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}