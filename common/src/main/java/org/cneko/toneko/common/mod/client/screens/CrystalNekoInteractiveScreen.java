package org.cneko.toneko.common.mod.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.packets.interactives.GiftItemPayload;
import org.cneko.toneko.common.mod.util.TickTaskQueue;
import org.cneko.toneko.common.mod.entities.CrystalNekoEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CrystalNekoInteractiveScreen extends InteractionScreen implements INekoScreen{
    public CrystalNekoEntity neko;
    public CrystalNekoInteractiveScreen(@NotNull CrystalNekoEntity neko, @Nullable Screen lastScreen) {
        super(Component.empty(), lastScreen, (screen)-> {
            // 在父类构造函数调用后执行
            return getButtonBuilders(screen,neko);
        });
        this.neko = neko;
    }

    @Override
    public CrystalNekoEntity getNeko() {
        return neko;
    }

    public static Map<String, Button.Builder> getButtonBuilders(Screen screen,CrystalNekoEntity neko) {
        Map<String,Button.Builder> builders = new LinkedHashMap<>();
        builders.put("screen.toneko.crystal_neko_interactive.button.who",Button.builder(Component.translatable("screen.toneko.crystal_neko_interactive.button.who"),(btn)->{
            Player player = Minecraft.getInstance().player;
            TickTaskQueue messageQueue = new TickTaskQueue();
            messageQueue.addTask(20, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.0")));
            messageQueue.addTask(50, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.1")));  // 相对于上一个任务
            messageQueue.addTask(80, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.2")));
            messageQueue.addTask(120, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.3")));
            messageQueue.addTask(170, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.4")));
            messageQueue.addTask(210, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.5")));
            messageQueue.addTask(270, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.6")));
            TickTasks.addClient(messageQueue);
        }));
        builders.put("screen.toneko.crystal_neko_interactive.button.about_mod",Button.builder(Component.translatable("screen.toneko.crystal_neko_interactive.button.about_mod"),(btn)->{
            Player player = Minecraft.getInstance().player;
            TickTaskQueue messageQueue = new TickTaskQueue();
            messageQueue.addTask(20, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.0")));
            messageQueue.addTask(90, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.1")));
            messageQueue.addTask(140, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.2")));
            messageQueue.addTask(190, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.3")));
            messageQueue.addTask(230, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.4")));
            messageQueue.addTask(260, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.5")));
            messageQueue.addTask(300, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.6")));
            messageQueue.addTask(345, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.7")));
            messageQueue.addTask(380, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.8")));
            messageQueue.addTask(425, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.9")));
            messageQueue.addTask(455, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.10")));
            messageQueue.addTask(490, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.11")));
            messageQueue.addTask(535, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.12")));
            messageQueue.addTask(585, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.13")));
            messageQueue.addTask(625, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.14")));
            messageQueue.addTask(660, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.15")));
            messageQueue.addTask(705, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.16")));
            TickTasks.addClient(messageQueue);
        }));
        builders.put("screen.toneko.crystal_neko_interactive.button.plans",Button.builder(Component.translatable("screen.toneko.crystal_neko_interactive.button.plans"),(btn)->{
            Player player = Minecraft.getInstance().player;
            TickTaskQueue messageQueue = new TickTaskQueue();
            messageQueue.addTask(20, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.0")));
            messageQueue.addTask(90, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.1")));
            messageQueue.addTask(140, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.2")));
            messageQueue.addTask(190, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.3")));
            messageQueue.addTask(230, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.4")));
            messageQueue.addTask(260, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.5")));
            messageQueue.addTask(300, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.6")));
            messageQueue.addTask(345, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.7")));
            messageQueue.addTask(380, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.8")));
            messageQueue.addTask(425, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.9")));
            messageQueue.addTask(455, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.10")));
            messageQueue.addTask(490, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.11")));
            messageQueue.addTask(535, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.12")));
            messageQueue.addTask(585, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.13")));
            messageQueue.addTask(625, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.14")));
            messageQueue.addTask(660, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.15")));
            messageQueue.addTask(705, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.16")));
            TickTasks.addClient(messageQueue);
        }));
        builders.put("screen.toneko.crystal_neko_interactive.button.links",Button.builder(Component.translatable("screen.toneko.crystal_neko_interactive.button.links"),(btn)->{
            Minecraft.getInstance().setScreen(new LinksScreen(screen,neko));
        }));
        builders.put("screen.toneko.crystal_neko_interactive.button.interactive",Button.builder(Component.translatable("screen.toneko.crystal_neko_interactive.button.interactive"),(btn)->{
            Minecraft.getInstance().setScreen(new NekoEntityInteractiveScreen(neko,screen,(screen1)-> getNekoButtonBuilders(screen1,neko)));
        }));
        builders.put("screen.toneko.crystal_neko_interactive.button.more",Button.builder(Component.translatable("screen.toneko.crystal_neko_interactive.button.more"),(btn)->{
            Minecraft.getInstance().setScreen(new MoreInteractiveScreen(neko,screen));
        }));
        return builders;
    }

    public static Map<String,Button.Builder> getNekoButtonBuilders(InteractionScreen screen,CrystalNekoEntity neko) {
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
            Player player = Minecraft.getInstance().player;
            TickTaskQueue messageQueue = new TickTaskQueue();
            messageQueue.addTask(20, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.interactive.breed.0")));
            messageQueue.addTask(50, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.interactive.breed.1")));
            messageQueue.addTask(90, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.interactive.breed.2")));
            messageQueue.addTask(120, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.interactive.breed.3")));
            TickTasks.addClient(messageQueue);
        }));

        return builders;
    }

    public static class MoreInteractiveScreen extends InteractionScreen implements INekoScreen {
        private final CrystalNekoEntity neko;

        public MoreInteractiveScreen(CrystalNekoEntity neko,@Nullable Screen lastScreen) {
            super(Component.empty(), lastScreen, (screen -> getButtonBuilders(screen, neko)));
            this.neko = neko;
        }

        @Override
        public CrystalNekoEntity getNeko() {
            return neko;
        }

        public static Map<String, Button.Builder> getButtonBuilders(Screen screen, CrystalNekoEntity neko) {
            Map<String, Button.Builder> builders = new LinkedHashMap<>();
            builders.put("screen.toneko.crystal_neko_more_interactive.button.nya",Button.builder(Component.translatable("screen.toneko.crystal_neko_more_interactive.button.nya"),(btn)->{
                neko.nya(Minecraft.getInstance().player);
            }));


            return builders;
        }
    }

}
