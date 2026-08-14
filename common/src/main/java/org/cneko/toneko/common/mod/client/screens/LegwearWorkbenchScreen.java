package org.cneko.toneko.common.mod.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.blocks.LegwearWorkbenchBlock;
import org.cneko.toneko.common.mod.items.LegwearItem;
import org.cneko.toneko.common.mod.misc.ZettaiRyouiki;
import org.cneko.toneko.common.mod.packets.LegwearAdjustPayload;
import org.cneko.toneko.common.mod.packets.LegwearDyePayload;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc;

/**
 * 腿部服饰工作台 GUI：两个免费滑杆调 D 值与袜口高度。
 * 拖动时仅本地预览，松手发一次 C2S 落库。
 */
public class LegwearWorkbenchScreen extends AbstractContainerScreen<LegwearWorkbenchBlock.LegwearWorkbenchMenu> {
    // 注意：必须带 .png 后缀（SimpleTexture 不会自动补，原版 GUI 贴图路径惯例均带后缀）
    private static final ResourceLocation BG = toNekoLoc("textures/gui/legwear_workbench.png");
    // 贴图 512x512（用户自定义背景），blit 需显式传入纹理尺寸，否则按 256 采样错乱
    private static final int BG_TEXTURE_SIZE = 512;

    private DenierSlider denierSlider;
    private LengthSlider lengthSlider;
    private DyeSwatchButton leftSwatch;
    private DyeSwatchButton rightSwatch;
    private int previewDenier = 40;
    private float previewLength = 1f;
    private ItemStack lastSlotStack = ItemStack.EMPTY;

    public LegwearWorkbenchScreen(LegwearWorkbenchBlock.LegwearWorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 196;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 108;
    }

    @Override
    protected void init() {
        super.init();
        this.lastSlotStack = this.menu.getSlot(0).getItem();
        this.denierSlider = new DenierSlider(this, this.leftPos + 44, this.topPos + 42, 112, 18, 40);
        this.lengthSlider = new LengthSlider(this, this.leftPos + 44, this.topPos + 74, 112, 18, 1f);
        // 左右腿色块按钮（槽位下方，26x26 醒目色块 + 粗边框）
        this.leftSwatch = new DyeSwatchButton(this, 0, this.leftPos + 17, this.topPos + 48);
        this.rightSwatch = new DyeSwatchButton(this, 1, this.leftPos + 17, this.topPos + 80);
        this.addRenderableWidget(this.denierSlider);
        this.addRenderableWidget(this.lengthSlider);
        this.addRenderableWidget(this.leftSwatch);
        this.addRenderableWidget(this.rightSwatch);
        this.syncFromStack();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // 槽位栈实例变化（放入/取出/服务端回显 setSlot 换实例）时重置滑杆
        ItemStack now = this.menu.getSlot(0).getItem();
        if (now != this.lastSlotStack) {
            this.lastSlotStack = now;
            this.syncFromStack();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 色块按钮悬停提示
        if (this.leftSwatch != null && this.leftSwatch.isHovered()) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.toneko.legwear_workbench.dye_left",
                    swatchColorName(this.leftSwatch.rgb)), mouseX, mouseY);
        } else if (this.rightSwatch != null && this.rightSwatch.isHovered()) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gui.toneko.legwear_workbench.dye_right",
                    swatchColorName(this.rightSwatch.rgb)), mouseX, mouseY);
        }
    }

    /** 色值 → 原版颜色名（默认返回"默认"） */
    private static Component swatchColorName(int rgb) {
        if (rgb < 0) return Component.translatable("gui.toneko.legwear_workbench.dye_default");
        for (DyeColor c : DyeColor.values()) {
            if ((c.getTextureDiffuseColor() & 0xFFFFFF) == (rgb & 0xFFFFFF)) {
                return Component.translatable("color.minecraft." + c.getName());
            }
        }
        return Component.literal(String.format("#%06X", rgb & 0xFFFFFF));
    }

    /** 从槽位物品重置滑杆、色块与预览值 */
    private void syncFromStack() {
        ItemStack stack = this.menu.getSlot(0).getItem();
        boolean has = LegwearItem.isLegwear(stack);
        this.previewDenier = has ? LegwearItem.getDenier(stack) : 40;
        this.previewLength = has ? LegwearItem.getStockingTopHeight(stack) : 1f;
        this.denierSlider.setFromDenier(this.previewDenier);
        this.lengthSlider.setFromLength(this.previewLength);
        this.denierSlider.active = has;
        this.lengthSlider.active = has;
        this.leftSwatch.setRgb(has ? LegwearItem.getLeftDye(stack) : -1);
        this.rightSwatch.setRgb(has ? LegwearItem.getRightDye(stack) : -1);
        // 色块按钮始终可点（空槽点击时在 onClick 里提示，而非静默无响应）
    }

    /** 松手落库：发一次 C2S */
    private void sendAdjust() {
        ClientPlayNetworking.send(new LegwearAdjustPayload(this.previewDenier, this.previewLength));
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 11 参重载：屏幕 176x196 显示整张 512x512 贴图（用户背景为整幅画布，非左上角分块布局）
        guiGraphics.blit(BG, this.leftPos, this.topPos, this.imageWidth, this.imageHeight,
                0f, 0f, BG_TEXTURE_SIZE, BG_TEXTURE_SIZE, BG_TEXTURE_SIZE, BG_TEXTURE_SIZE);

        // 两个标签
        guiGraphics.drawString(this.font, Component.translatable("gui.toneko.legwear_workbench.denier"),
                this.leftPos + 44, this.topPos + 30, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.toneko.legwear_workbench.length"),
                this.leftPos + 44, this.topPos + 62, 0x404040, false);

        ItemStack stack = this.menu.getSlot(0).getItem();
        if (LegwearItem.isLegwear(stack)) {
            // 绝对领域：连续百分比 + 等级（按当前预览袜口高度即时计算，拖动滑杆时无级变化）
            String grade = ZettaiRyouiki.compute(this.previewLength, ZettaiRyouiki.DEFAULT_SKIRT_HEM_HEIGHT);
            Component gradeText = Component.translatable("item.toneko.legwear.zettai_ryouiki." + grade);
            if ("full".equals(grade)) {
                guiGraphics.drawString(this.font, Component.translatable("gui.toneko.legwear_workbench.zettai_full", gradeText),
                        this.leftPos + 44, this.topPos + 98, 0x404040, false);
            } else {
                guiGraphics.drawString(this.font, Component.translatable("gui.toneko.legwear_workbench.zettai",
                        Math.round(ZettaiRyouiki.computeTerritory(this.previewLength, ZettaiRyouiki.DEFAULT_SKIRT_HEM_HEIGHT) * 100),
                        gradeText),
                        this.leftPos + 44, this.topPos + 98, 0x404040, false);
            }
        } else {
            // 空槽：领域显示"无"
            guiGraphics.drawString(this.font, Component.translatable("gui.toneko.legwear_workbench.zettai_full",
                    Component.translatable("item.toneko.legwear.zettai_ryouiki.none")),
                    this.leftPos + 44, this.topPos + 98, 0x808080, false);
        }
    }

    /** D 值滑杆：0~1 ↔ 5~120D，拖动仅预览，松手发包 */
    private static final class DenierSlider extends AbstractSliderButton {
        private final LegwearWorkbenchScreen screen;

        DenierSlider(LegwearWorkbenchScreen screen, int x, int y, int width, int height, int denier) {
            super(x, y, width, height, Component.empty(), (denier - 5) / 115.0);
            this.screen = screen;
            this.updateMessage();
        }

        /** 外部重置滑杆（槽位物品变化时） */
        void setFromDenier(int denier) {
            this.value = (denier - 5) / 115.0;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.screen.previewDenier = 5 + (int) Math.round(this.value * 115);
            this.setMessage(Component.literal(this.screen.previewDenier + "D"));
        }

        @Override
        protected void applyValue() {
            // 拖动每帧调用：仅更新本地预览（updateMessage 已做），不发包
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            this.screen.sendAdjust();
        }
    }

    /** 袜口高度滑杆：0~1，0.01 步进显示，松手发包 */
    private static final class LengthSlider extends AbstractSliderButton {
        private final LegwearWorkbenchScreen screen;

        LengthSlider(LegwearWorkbenchScreen screen, int x, int y, int width, int height, float length) {
            super(x, y, width, height, Component.empty(), Math.max(0.0, Math.min(1.0, length)));
            this.screen = screen;
            this.updateMessage();
        }

        /** 外部重置滑杆（槽位物品变化时） */
        void setFromLength(float length) {
            this.value = Math.max(0.0, Math.min(1.0, length));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.screen.previewLength = Math.round(this.value * 100) / 100f;
            this.setMessage(Component.literal(Math.round(this.screen.previewLength * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            // 拖动每帧调用：仅更新本地预览，不发包
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            this.screen.sendAdjust();
        }
    }

    /** 腿色色块按钮：点击循环 默认 → 16 色 → 回默认，立即发包 */
    private static final class DyeSwatchButton extends AbstractWidget {
        private final LegwearWorkbenchScreen screen;
        private final int side;   // 0=左 1=右
        private int rgb = -1;     // 当前色，-1 = 默认（跟随整体染色）

        DyeSwatchButton(LegwearWorkbenchScreen screen, int side, int x, int y) {
            super(x, y, 26, 26, Component.empty());
            this.screen = screen;
            this.side = side;
        }

        void setRgb(int rgb) {
            this.rgb = rgb;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            // 按钮始终可点：空槽时给出提示而非静默无响应
            ItemStack stack = this.screen.menu.getSlot(0).getItem();
            if (!LegwearItem.isLegwear(stack)) {
                if (this.screen.minecraft != null && this.screen.minecraft.player != null) {
                    this.screen.minecraft.player.displayClientMessage(
                            Component.translatable("gui.toneko.legwear_workbench.no_item"), true);
                }
                return;
            }
            DyeColor[] colors = DyeColor.values();
            if (this.rgb < 0) {
                // 从黑色开始（默认是白色块，第一击必须有明显变化）
                // 注意：getTextureDiffuseColor 返回 ARGB（0xFF 前缀，int 为负），必须 & 0xFFFFFF
                this.rgb = colors[colors.length - 1].getTextureDiffuseColor() & 0xFFFFFF;
            } else {
                int idx = -1;
                for (int i = 0; i < colors.length; i++) {
                    if ((colors[i].getTextureDiffuseColor() & 0xFFFFFF) == (this.rgb & 0xFFFFFF)) {
                        idx = i;
                        break;
                    }
                }
                if (idx <= 0) {
                    this.rgb = -1;  // 回到默认（跟随整体染色缸色）
                } else {
                    this.rgb = colors[idx - 1].getTextureDiffuseColor() & 0xFFFFFF;
                }
            }
            ClientPlayNetworking.send(new LegwearDyePayload(this.side, this.rgb));
        }

        @Override
        protected void updateWidgetNarration(@NotNull net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
            this.defaultButtonNarrationText(narrationElementOutput);
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            if (this.rgb < 0) {
                // 默认状态：上半白、下半灰的对角分割——与纯色块明显区分
                for (int j = 0; j < this.height; j++) {
                    for (int i = 0; i < this.width; i++) {
                        int c = (i + j) < this.width ? 0xFFFFFFFF : 0xFFAAAAAA;
                        guiGraphics.fill(x + i, y + j, x + i + 1, y + j + 1, c);
                    }
                }
            } else {
                // 独立色块：纯色
                guiGraphics.fill(x, y, x + this.width, y + this.height,
                        0xFF000000 | (this.rgb & 0xFFFFFF));
            }
            // 2px 粗黑边框 + 悬停高亮（醒目，防淹没在花背景里）
            int border = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            for (int k = 0; k < 2; k++) {
                guiGraphics.renderOutline(x - k, y - k, this.width + 2 * k, this.height + 2 * k, border);
            }
        }
    }
}
