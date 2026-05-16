package org.cneko.toneko.common.mod.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.cneko.toneko.common.mod.genetics.api.GeneticsRegistry;
import org.cneko.toneko.common.mod.genetics.api.SpeciesKaryotype;
import org.cneko.toneko.common.mod.packets.GenomeDataPayload;

import java.util.ArrayList;
import java.util.List;

public class GeneticsScreen extends Screen {
    private final int entityId;
    private final LivingEntity targetEntity; // 保存目标实体引用
    private final SpeciesKaryotype karyotype; // 当前物种核型
    private final CompoundTag genomeNbt;
    private final boolean canEdit;

    private final List<String> chromosomes = new ArrayList<>();
    private int currentChrIndex = 0;

    private double leftScrollY = 0;
    private int leftMaxScroll = 0;

    // 右侧显示的列表内容
    private final List<ResourceLocation> displayAlleles = new ArrayList<>();
    private double rightScrollY = 0;
    private int rightMaxScroll = 0;

    private Button prevBtn, nextBtn, saveBtn, toggleModeBtn; // 模式切换按钮

    private String editingLocus = null;
    private String editingStrand = null;
    private String currentAlleleId = null;

    private boolean showAllAllelesMode = false; // 状态：是否显示非该座位的基因

    public GeneticsScreen(int entityId, CompoundTag genomeNbt, boolean canEdit) {
        super(Component.translatable("gui.toneko.genetics_viewer"));
        this.entityId = entityId;
        this.genomeNbt = genomeNbt;
        this.canEdit = canEdit;

        // 获取实体和核型
        this.targetEntity = (LivingEntity) net.minecraft.client.Minecraft.getInstance().level.getEntity(entityId);
        this.karyotype = GeneticsRegistry.getKaryotype(targetEntity);

        // 初始化染色体列表
        if (karyotype != null) {
            // 优先根据物种核型来显示染色体
            for (int i = 1; i <= karyotype.getChromosomePairs(); i++) {
                this.chromosomes.add(String.valueOf(i));
            }
        } else {
            // 退化逻辑：如果没有核型，则直接读 NBT 的 Key
            this.chromosomes.addAll(genomeNbt.getAllKeys());
            this.chromosomes.sort(GeneticsScreen::compareNaturally);
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        prevBtn = this.addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            if (currentChrIndex > 0) currentChrIndex--;
            updateState();
        }).bounds(20, 20, 30, 20).build());

        nextBtn = this.addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            if (currentChrIndex < chromosomes.size() - 1) currentChrIndex++;
            updateState();
        }).bounds(centerX - 50, 20, 30, 20).build());

        // 新增：全量模式切换按钮（放在右上角）
        toggleModeBtn = this.addRenderableWidget(Button.builder(Component.translatable("gui.toneko.genetics_viewer.filter"), btn -> {
            showAllAllelesMode = !showAllAllelesMode;
            updateDisplayAlleles();
            btn.setMessage(showAllAllelesMode
                    ? Component.translatable("gui.toneko.genetics_viewer.mode.all")
                    : Component.translatable("gui.toneko.genetics_viewer.mode.recommended"));
        }).bounds(this.width - 110, 20, 100, 20).build());
        toggleModeBtn.visible = canEdit;

        if (canEdit) {
            saveBtn = this.addRenderableWidget(Button.builder(Component.translatable("gui.toneko.genetics_viewer.save"), btn -> {
                ClientPlayNetworking.send(new GenomeDataPayload(entityId, genomeNbt, true));
                this.minecraft.setScreen(null);
            }).bounds(this.width - 110, this.height - 30, 100, 20).build());
        }

        updateState();
    }

    private void updateState() {
        prevBtn.active = currentChrIndex > 0;
        nextBtn.active = currentChrIndex < chromosomes.size() - 1;
        leftScrollY = 0;
        rightScrollY = 0;
        editingLocus = null;
        editingStrand = null;
        updateDisplayAlleles();
    }

    /**
     * 【核心改进】：根据当前点击的基因座和过滤模式更新右侧备选列表
     */
    private void updateDisplayAlleles() {
        displayAlleles.clear();
        if (editingLocus == null) return;

        ResourceLocation locusId = ResourceLocation.parse(editingLocus);

        if (showAllAllelesMode) {
            // 全量模式：显示所有已注册基因
            displayAlleles.addAll(GeneticsRegistry.ALLELES.keySet());
        } else {
            // 推荐模式：仅显示该基因座对应的野生库基因
            List<GeneticsRegistry.WeightedAllele> pool = GeneticsRegistry.WILD_POOLS.get(locusId);
            if (pool != null) {
                for (GeneticsRegistry.WeightedAllele wa : pool) {
                    displayAlleles.add(wa.alleleId());
                }
            }
        }

        displayAlleles.sort((a, b) -> compareNaturally(a.toString(), b.toString()));
        rightScrollY = 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int centerX = this.width / 2;
        int listTop = 50;
        int listBottom = this.height - 40;

        if (mouseY < listTop || mouseY > listBottom) return super.mouseClicked(mouseX, mouseY, button);

        // --- 处理左侧点击 (选择基因座) ---
        if (mouseX < centerX) {
            String chrIdStr = chromosomes.get(currentChrIndex);
            CompoundTag pairTag = genomeNbt.getCompound(chrIdStr);
            CompoundTag strandA = pairTag.getCompound("A");
            int y = listTop - (int) leftScrollY;

            // 这里根据实际有的 Locus 列表循环
            for (String locusId : strandA.getAllKeys()) {
                // A 链点击
                if (mouseX >= 15 && mouseX <= centerX / 2 - 5 && mouseY >= y + 16 && mouseY <= y + 26) {
                    setEditingTarget(locusId, "A", strandA.getString(locusId));
                    return true;
                }
                // B 链点击
                CompoundTag strandB = pairTag.getCompound("B");
                if (mouseX >= centerX / 2 && mouseX <= centerX - 15 && mouseY >= y + 16 && mouseY <= y + 26) {
                    setEditingTarget(locusId, "B", strandB.getString(locusId));
                    return true;
                }
                y += 35;
            }
        }
        // --- 处理右侧点击 (替换基因) ---
        else if (canEdit && editingLocus != null) {
            int y = listTop - (int) rightScrollY;
            int startX = centerX + 20;
            for (ResourceLocation alleleIdObj : displayAlleles) {
                if (mouseX >= startX && mouseX <= this.width - 20 && mouseY >= y && mouseY < y + 16) {
                    applyAlleleChange(alleleIdObj.toString());
                    return true;
                }
                y += 16;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (chromosomes.isEmpty()) return;

        int centerX = this.width / 2;
        int listTop = 50;
        int listBottom = this.height - 40;

        String chrId = chromosomes.get(currentChrIndex);
        graphics.drawCenteredString(this.font, Component.translatable("gui.toneko.genetics_viewer.chromosome", chrId), centerX / 2, 26, 0xFFAA00);

        // 渲染左侧面板
        graphics.enableScissor(0, listTop, centerX, listBottom);
        renderLeftPanel(graphics, chrId, mouseX, mouseY, listTop, listBottom);
        graphics.disableScissor();

        // 渲染右侧面板
        if (canEdit && editingLocus != null) {
            graphics.drawCenteredString(this.font, showAllAllelesMode
                            ? Component.translatable("gui.toneko.genetics_viewer.pool.all")
                            : Component.translatable("gui.toneko.genetics_viewer.pool.recommended"),
                    centerX + centerX / 2, 26, showAllAllelesMode ? 0xFFAAAA : 0x55FFFF);

            graphics.enableScissor(centerX, listTop, this.width, listBottom);
            renderRightPanel(graphics, mouseX, mouseY, listTop, listBottom);
            graphics.disableScissor();
        }

        graphics.fill(centerX, listTop, centerX + 1, listBottom, 0x55FFFFFF);
    }

    private void renderLeftPanel(GuiGraphics graphics, String chrId, int mouseX, int mouseY, int top, int bottom) {
        CompoundTag pair = genomeNbt.getCompound(chrId);
        CompoundTag strandA = pair.getCompound("A");
        CompoundTag strandB = pair.getCompound("B");
        int y = top - (int) leftScrollY;
        int centerX = this.width / 2;

        // 这里遍历 A 链的所有 Key
        for (String locusId : strandA.getAllKeys()) {
            graphics.fill(10, y, centerX - 10, y + 30, 0x44000000);

            // 翻译基因座名称（如果有的话）
            String locusName = getLocusTranslatedName(locusId);
            graphics.drawString(this.font, Component.translatable("gui.toneko.genetics_viewer.locus", locusName).getString(), 15, y + 4, 0xAAAAAA);

            boolean isSelA = locusId.equals(editingLocus) && "A".equals(editingStrand);
            boolean hoverA = canEdit && mouseX >= 15 && mouseX <= centerX / 2 - 5 && mouseY >= y + 16 && mouseY <= y + 26 && mouseY > top && mouseY < bottom;
            graphics.drawString(this.font, Component.translatable("gui.toneko.genetics_viewer.paternal", getTranslatedName(strandA.getString(locusId))).getString(), 15, y + 16, isSelA ? 0xFFFF55 : (hoverA ? 0x55FF55 : 0xDDDDDD));

            boolean isSelB = locusId.equals(editingLocus) && "B".equals(editingStrand);
            boolean hoverB = canEdit && mouseX >= centerX / 2 && mouseX <= centerX - 15 && mouseY >= y + 16 && mouseY <= y + 26 && mouseY > top && mouseY < bottom;
            graphics.drawString(this.font, Component.translatable("gui.toneko.genetics_viewer.maternal", getTranslatedName(strandB.getString(locusId))).getString(), centerX / 2, y + 16, isSelB ? 0xFFFF55 : (hoverB ? 0x55FF55 : 0xDDDDDD));

            y += 35;
        }
        leftMaxScroll = Math.max(0, (strandA.size() * 35) - (bottom - top));
    }

    private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY, int top, int bottom) {
        int y = top - (int) rightScrollY;
        int centerX = this.width / 2;
        int startX = centerX + 20;

        for (ResourceLocation alleleIdObj : displayAlleles) {
            String alleleId = alleleIdObj.toString();
            boolean isCurrent = alleleId.equals(currentAlleleId);
            boolean hover = mouseX >= startX && mouseX <= this.width - 20 && mouseY >= y && mouseY < y + 16 && mouseY > top && mouseY < bottom;

            if (isCurrent) graphics.fill(startX - 2, y - 2, this.width - 18, y + 14, 0x66FFD700);
            else if (hover) graphics.fill(startX - 2, y - 2, this.width - 18, y + 14, 0x44FFFFFF);

            graphics.drawString(this.font, getTranslatedName(alleleId), startX, y, isCurrent ? 0xFFFF55 : 0xFFFFFF);
            y += 16;
        }
        rightMaxScroll = Math.max(0, (displayAlleles.size() * 16) - (bottom - top));
    }

    private void setEditingTarget(String locus, String strand, String currentAllele) {
        this.editingLocus = locus;
        this.editingStrand = strand;
        this.currentAlleleId = currentAllele;
        this.updateDisplayAlleles(); // 【关键】：点击不同的 Locus 时，刷新右侧列表
        this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    // ... [applyAlleleChange 方法保持不变] ...
    private void applyAlleleChange(String newAlleleId) {
        if (editingLocus != null && editingStrand != null) {
            CompoundTag pair = genomeNbt.getCompound(chromosomes.get(currentChrIndex));
            pair.getCompound(editingStrand).putString(editingLocus, newAlleleId);
            this.currentAlleleId = newAlleleId;
            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME, 1.5F));
        }
    }

    private String getTranslatedName(String id) {
        String langKey = "allele." + id.replace(":", ".");
        Component comp = Component.translatable(langKey);
        return comp.getString().equals(langKey) ? id : comp.getString();
    }

    private String getLocusTranslatedName(String id) {
        String langKey = "locus." + id.replace(":", ".");
        Component comp = Component.translatable(langKey);
        return comp.getString().equals(langKey) ? id : comp.getString();
    }

    // ... [mouseScrolled, isPauseScreen, compareNaturally 保持不变] ...
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < this.width / 2) {
            this.leftScrollY = Mth.clamp(this.leftScrollY - scrollY * 20, 0, leftMaxScroll);
        } else if (canEdit && editingLocus != null) {
            this.rightScrollY = Mth.clamp(this.rightScrollY - scrollY * 20, 0, rightMaxScroll);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static int compareNaturally(String s1, String s2) {
        int i = 0, j = 0;
        while (i < s1.length() && j < s2.length()) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(j);
            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                StringBuilder n1 = new StringBuilder();
                StringBuilder n2 = new StringBuilder();
                while (i < s1.length() && Character.isDigit(s1.charAt(i))) n1.append(s1.charAt(i++));
                while (j < s2.length() && Character.isDigit(s2.charAt(j))) n2.append(s2.charAt(j++));
                long v1 = Long.parseLong(n1.toString());
                long v2 = Long.parseLong(n2.toString());
                if (v1 != v2) return Long.compare(v1, v2);
            } else {
                if (c1 != c2) return c1 - c2;
                i++; j++;
            }
        }
        return s1.length() - s2.length();
    }
}