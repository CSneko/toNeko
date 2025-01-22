package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.cneko.toneko.common.mod.client.screens.factories.ScreenBuilders;

import java.util.*;

import static org.cneko.toneko.common.mod.entities.ToNekoEntities.*;

public class NekoScreenRegistry {
    private static Map<ResourceLocation, NekoScreenBuilder> screens = new HashMap<>();

    public static void register(ResourceLocation id, NekoScreenBuilder builder){
        screens.put(id, builder);
    }
    public static NekoScreenBuilder get(ResourceLocation id){
        return screens.get(id);
    }
    public static NekoScreenBuilder get(EntityType<?> entityType){
        return get(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
    }
    public static void init(){
        register(ADVENTURER_NEKO_ID, ScreenBuilders.COMMON_INTERACTION_SCREEN);
        register(CRYSTAL_NEKO_ID, ScreenBuilders.CRYSTAL_NEKO_INTERACTION_SCREEN);
        register(GHOST_NEKO_ID, ScreenBuilders.CRYSTAL_NEKO_BASE_INTERACTION_SCREEN);
    }

    public static class NekoScreenBuilder {
        private List<WidgetFactory> widgets;
        private int startY = 0;
        public NekoScreenBuilder setStartY(int startY) {
            this.startY = startY;
            return this;
        }
        public NekoScreenBuilder addButton(ButtonFactory button){
            widgets.add(button);
            return this;
        }
        public NekoScreenBuilder addTooltip(TooltipFactory tooltip){
            widgets.add(tooltip);
            return this;
        }
        public List<WidgetFactory> getWidgets() {
            return widgets;
        }
        public int getStartY() {
            return startY;
        }

        @FunctionalInterface
        public interface TooltipFactory extends WidgetFactory{
            Component build(InteractionScreen screen);
        }
        @FunctionalInterface
        public interface ButtonFactory extends WidgetFactory {
            Button.Builder build(InteractionScreen screen);
        }
        public interface WidgetFactory{
        }
    }
}
