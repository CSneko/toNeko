package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.cneko.toneko.common.mod.api.NekoLevelRegistry;
import org.cneko.toneko.common.mod.entities.INeko;
import org.jetbrains.annotations.NotNull;

import static org.cneko.toneko.common.mod.util.TextUtil.translatable;

public class NekoInfoScreen extends Screen {
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int LABEL_COLOR = 0xAAAAAA;
    private static final int VALUE_COLOR = 0xFFFF55;
    private static final int SECTION_COLOR = 0x55FFFF;
    private static final int FACTOR_COLOR = 0x55FF55;
    private static final int FACTOR_RAW_COLOR = 0x888888;

    protected NekoInfoScreen() {
        super(translatable("screen.toneko.neko_info.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new NekoInfoScreen());
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.back"),
                button -> this.onClose()
        ).bounds(cx - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int left = 20;
        int y = 20;
        int lineHeight = 12;

        // Title
        guiGraphics.drawString(this.font, this.title, left, y, TITLE_COLOR, true);
        y += 18;

        // Status
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.status",
                translatable(player.isNeko() ? "screen.toneko.neko_info.yes" : "screen.toneko.neko_info.no")),
                left, y, LABEL_COLOR, false);
        y += lineHeight;

        if (!player.isNeko()) {
            y += lineHeight;
            guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.not_neko"), left, y, 0xFF5555, false);
            return;
        }

        // Nickname
        String nick = player.getNickName();
        if (!nick.isEmpty()) {
            guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.nickname", nick),
                    left, y, LABEL_COLOR, false);
            y += lineHeight;
        }

        y += 4;

        // === Age Section ===
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.age_section"),
                left, y, SECTION_COLOR, true);
        y += lineHeight;

        int age = player.getNekoAge();
        int maxAge = player.getMaxAge();
        boolean isBaby = player.isNekoBaby();
        double ageScale = player.getNekoAgeScale();
        int growthPercent = (int) Math.round((ageScale - 0.3) / 0.7 * 100);

        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.is_baby",
                        translatable(isBaby ? "screen.toneko.neko_info.yes" : "screen.toneko.neko_info.no")),
                left + 10, y, VALUE_COLOR, false);
        y += lineHeight;

        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.age",
                        age, maxAge),
                left + 10, y, VALUE_COLOR, false);
        y += lineHeight;

        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.age_scale",
                        String.format("%.2f", ageScale), growthPercent),
                left + 10, y, VALUE_COLOR, false);
        y += lineHeight;

        y += 4;

        // === Level Section ===
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.level_section"),
                left, y, SECTION_COLOR, true);
        y += lineHeight;

        float totalLevel = player.getNekoLevel();
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.total_level", String.format("%.1f", totalLevel)),
                left + 10, y, VALUE_COLOR, false);
        y += lineHeight;

        // Base factor
        double baseRaw = player.getNekoLevelFactorRaw("base");
        double baseLevel = NekoLevelRegistry.base().getLevel(baseRaw);
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.base_factor",
                        String.format("%.1f", baseLevel), String.format("%.0f", baseRaw)),
                left + 20, y, FACTOR_COLOR, false);
        y += lineHeight;

        // Interaction factor
        double interactionRaw = player.getNekoLevelFactorRaw("interaction");
        double interactionLevel = NekoLevelRegistry.interaction().getLevel(interactionRaw);
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.interaction_factor",
                        String.format("%.1f", interactionLevel), String.format("%.0f", interactionRaw)),
                left + 20, y, FACTOR_COLOR, false);
        y += lineHeight;

        // Combat factor
        double combatRaw = player.getNekoLevelFactorRaw("combat");
        double combatLevel = NekoLevelRegistry.combat().getLevel(combatRaw);
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.combat_factor",
                        String.format("%.1f", combatLevel), String.format("%.0f", combatRaw)),
                left + 20, y, FACTOR_COLOR, false);
        y += lineHeight;

        y += 4;

        // === Energy Section ===
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.energy_section"),
                left, y, SECTION_COLOR, true);
        y += lineHeight;

        float energy = player.getNekoEnergy();
        float maxEnergy = player.getMaxNekoEnergy();
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.energy",
                        String.format("%.0f", energy), String.format("%.0f", maxEnergy)),
                left + 10, y, VALUE_COLOR, false);
        y += lineHeight;

        y += 4;

        // === Ability Section ===
        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.ability_section"),
                left, y, SECTION_COLOR, true);
        y += lineHeight;

        guiGraphics.drawString(this.font, translatable("screen.toneko.neko_info.ability", player.getNekoAbility()),
                left + 10, y, VALUE_COLOR, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
