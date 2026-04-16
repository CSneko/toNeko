package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.cneko.toneko.common.mod.genetics.api.GeneticsRegistry;

import java.util.ArrayList;
import java.util.List;

public class GeneticsScreen extends Screen {
    private final CompoundTag genomeNbt;
    private final List<String> displayLines = new ArrayList<>();

    public GeneticsScreen(CompoundTag genomeNbt) {
        super(Component.translatable("gui.toneko.genetics_viewer"));
        this.genomeNbt = genomeNbt;
    }

    @Override
    protected void init() {
        displayLines.clear();
        // 解析 NBT 数据为可读文本
        for (String chrKey : genomeNbt.getAllKeys()) {
            displayLines.add("§6[染色体 " + chrKey + "]§r");
            CompoundTag pair = genomeNbt.getCompound(chrKey);
            CompoundTag strandA = pair.getCompound("A");
            CompoundTag strandB = pair.getCompound("B");

            for (String locusId : strandA.getAllKeys()) {
                String alleleA = strandA.getString(locusId);
                String alleleB = strandB.getString(locusId);

                // 格式化输出: 基因座名: 基因A | 基因B
                displayLines.add("  §7" + getLocusName(locusId) + ":§f");
                displayLines.add("    §b" + getAlleleName(alleleA) + " §7| §b" + getAlleleName(alleleB));
            }
        }
    }

    private String getLocusName(String id) {
        return Component.translatable("locus." + id.replace(":", ".")).getString();
    }

    private String getAlleleName(String id) {
        return Component.translatable("allele." + id.replace(":", ".")).getString();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int y = 40;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        for (String line : displayLines) {
            graphics.drawString(this.font, line, 50, y, 0xFFFFFF);
            y += 12;
            if (y > this.height - 20) break; // 防止超出屏幕
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}