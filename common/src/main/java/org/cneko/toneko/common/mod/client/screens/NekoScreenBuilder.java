package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class NekoScreenBuilder implements Cloneable {
    private List<WidgetFactory> widgets = new ArrayList<>();
    private int startY = 15;

    public NekoScreenBuilder setStartY(int startY) {
        this.startY = startY+15;
        return this;
    }

    public NekoScreenBuilder addButton(ButtonFactory button) {
        widgets.add(button);
        return this;
    }

    public NekoScreenBuilder addTooltip(TooltipFactory tooltip) {
        widgets.add(tooltip);
        return this;
    }

    public List<WidgetFactory> getWidgets() {
        return widgets;
    }

    public int getStartY() {
        return startY;
    }

    @Override
    public NekoScreenBuilder clone() {
        try {
            NekoScreenBuilder clone = (NekoScreenBuilder) super.clone();
            clone.widgets = new ArrayList<>(widgets);
            clone.startY = startY;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @FunctionalInterface
    public interface TooltipFactory extends WidgetFactory {
        Component build(InteractionScreen screen);
    }

    @FunctionalInterface
    public interface ButtonFactory extends WidgetFactory {
        Button.Builder build(InteractionScreen screen);
    }

    public interface WidgetFactory {
    }
}
