package org.cneko.toneko.fabric.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.client.api.ClientEntityPoseManager;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.packets.interactives.*;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.fabric.entities.NekoEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NekoEntityInteractiveScreen extends InteractionScreen implements INekoScreen{
    private final @NotNull NekoEntity neko;
    public NekoEntityInteractiveScreen(@NotNull NekoEntity neko, @Nullable Screen lastScreen) {
        // 调用父类构造函数
        super(Component.empty(), lastScreen, ()-> {
            // 在父类构造函数调用后执行
            return getButtonBuilders(neko);
        });
        this.neko = neko;
    }

    @Override
    public @NotNull NekoEntity getNeko() {
        return neko;
    }

    public static Map<String,Button.Builder> getButtonBuilders(NekoEntity neko) {
        Map<String,Button.Builder> builders = new LinkedHashMap<>();

        builders.put("screen.toneko.neko_entity_interactive.button.gift",Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.gift"),(btn)->{
            ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
            int slot = Minecraft.getInstance().player.getInventory().findSlotMatchingItem(stack);
            if(!stack.isEmpty()){
                ClientPlayNetworking.send(new GiftItemPayload(neko.getUUID().toString(), slot));
            }
        }));

        builders.put("screen.toneko.neko_entity_interactive.button.action",Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.action"),(btn)->{
            NekoActionScreen.open(neko);
        }));

        builders.put("screen.toneko.neko_entity_interactive.button.breed",Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.breed"),(btn)->{
            // 获取附近16格内的所有猫娘
            List<INeko> entities = new ArrayList<>();
            for (LivingEntity entity : EntityUtil.getLivingEntitiesInRange(neko, Minecraft.getInstance().player.level(),(float) NekoEntity.DEFAULT_FIND_RANGE)){
                if(entity instanceof INeko && !entity.isBaby()){
                    entities.add((INeko) entity);
                }
            }
            NekoMateScreen.open(neko,entities,null);
        }));

        return builders;
    }

}
