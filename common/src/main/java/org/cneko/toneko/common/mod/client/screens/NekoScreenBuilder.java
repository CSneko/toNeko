package org.cneko.toneko.common.mod.client.screens;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NekoScreenBuilder implements Cloneable {
    private List<WidgetFactory> widgets = new ArrayList<>();
    private int startY = 15;

    public NekoScreenBuilder setStartY(int startY) {
        this.startY = startY+15;
        return this;
    }

    public NekoScreenBuilder addButton(ButtonFactory button) {
        return addWidget(button);
    }

    public NekoScreenBuilder addTooltip(TooltipFactory tooltip) {
        return addWidget(tooltip);
    }

    public NekoScreenBuilder addWidget(WidgetFactory widget) {
        widgets.add(widget);
        return this;
    }
    public enum InsertPosition {
        BEFORE,
        AFTER
    }

    public NekoScreenBuilder addWidget(WidgetFactory widget, WidgetFactory targetWidget, InsertPosition position) {
        return addWidget(widget, targetWidget, position, 1);
    }

    public NekoScreenBuilder addWidget(WidgetFactory widget, WidgetFactory targetWidget, InsertPosition position, int count) {
        // 如果 count 为负数，则设置为 1
        count = Math.max(count, 1);

        // 找到目标 widget 的位置
        int targetIndex = -1;
        for (int i = 0; i < widgets.size(); i++) {
            if (widgets.get(i).equals(targetWidget)) {
                targetIndex = i;
                break;
            }
        }

        // 如果找到了目标 widget
        if (targetIndex != -1) {
            if (position == InsertPosition.AFTER) {
                widgets.add(targetIndex + count, widget);  // 在目标 widget 后面插入
            } else if (position == InsertPosition.BEFORE) {
                widgets.add(targetIndex - count, widget);  // 在目标 widget 前面插入
            }
        } else {
            // 如果没有找到目标 widget，就在末尾添加
            widgets.add(widget);
        }

        return this;
    }



    public List<WidgetFactory> getWidgets() {
        return widgets;
    }
    public List<TooltipFactory> getTooltips() {
        List<TooltipFactory> tooltips = new ArrayList<>();
        for (WidgetFactory widget : widgets) {
            if (widget instanceof TooltipFactory) {
                tooltips.add((TooltipFactory) widget);
            }
        }
        return tooltips;
    }
    public List<ButtonFactory> getButtons() {
        List<ButtonFactory> buttons = new ArrayList<>();
        for (WidgetFactory widget : widgets) {
            if (widget instanceof ButtonFactory) {
                buttons.add((ButtonFactory) widget);
            }
        }
        return buttons;
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
