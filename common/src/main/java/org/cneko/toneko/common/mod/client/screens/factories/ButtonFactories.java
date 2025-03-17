package org.cneko.toneko.common.mod.client.screens.factories;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.client.api.ClientEntityPoseManager;
import org.cneko.toneko.common.mod.client.screens.*;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder.ButtonFactory;
import org.cneko.toneko.common.mod.entities.CrystalNekoEntity;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.items.ToNekoItems;
import org.cneko.toneko.common.mod.packets.MateWithCrystalNekoPayload;
import org.cneko.toneko.common.mod.packets.interactives.FollowOwnerPayload;
import org.cneko.toneko.common.mod.packets.interactives.GiftItemPayload;
import org.cneko.toneko.common.mod.packets.interactives.NekoPosePayload;
import org.cneko.toneko.common.mod.packets.interactives.RideEntityPayload;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.mod.util.TextUtil;
import org.cneko.toneko.common.mod.util.TickTaskQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ButtonFactories {
    public static ButtonFactory BACK_BUTTON = screen -> Button.builder(Component.translatable("gui.back"), button -> Minecraft.getInstance().setScreen(screen.lastScreen));
    public static ButtonFactory CHAT_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.chat"),(btn)-> Minecraft.getInstance().setScreen(new ChatWithNekoScreen(screen.getNeko())));
    public static ButtonFactory GIFT_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.gift"),(btn)->{
        ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
        int slot = Minecraft.getInstance().player.getInventory().findSlotMatchingItem(stack);
        if(!stack.isEmpty()){
            ClientPlayNetworking.send(new GiftItemPayload(screen.getNeko().getUUID().toString(), slot));
        }
    });
    public static ButtonFactory ACTION_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.action"),(btn)->{
        Minecraft.getInstance().setScreen(new InteractionScreen(screen.getTitle(), screen.getNeko(), screen, ScreenBuilders.COMMON_ACTION_SCREEN));
    });
    public static ButtonFactory BREED_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.breed"),(btn)->{
        Player player = Minecraft.getInstance().player;
        NekoEntity neko = screen.getNeko();
        if (neko.isBaby()){
            int i = new Random().nextInt(13);
            //player.sendSystemMessage(Component.translatable("message.toneko.neko.breed_fail_baby."+i));
            return;
        }
        if (neko.getMoeTags().contains("mesugaki")){
            if (!player.getMainHandItem().is(ToNekoItems.CATNIP)) {
                // 杂鱼，你还不配和我交配~
                btn.setPosition(new Random().nextInt(screen.width - btn.getWidth()), new Random().nextInt(screen.height - btn.getHeight()));
                //player.sendSystemMessage(TextUtil.randomTranslatabledComponent("message.toneko.neko.breed_fail_zako", 10, neko.getName().getString()));
            }else {
                // 哪只猫猫会拒绝猫薄荷呢
                NekoMateScreen.open(neko, List.of(player),null);
                //player.sendSystemMessage(TextUtil.randomTranslatabledComponent("message.toneko.neko.breed_success_zako", 3, neko.getName().getString()));
            }
            return;
        }
        // 获取附近16格内的所有猫娘
        List<INeko> entities = new ArrayList<>();
        for (LivingEntity entity : EntityUtil.getLivingEntitiesInRange(neko, Minecraft.getInstance().player.level(),(float) NekoEntity.DEFAULT_FIND_RANGE)){
            if(entity instanceof INeko o){
                if (o != neko)
                    entities.add(o);
            }
        }
        NekoMateScreen.open(neko,entities,null);
    });
    
    // ---------------------------------------------------- 动作 -----------------------------------------------------------------
    public static ButtonFactory ACTION_FOLLOW_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.follow"),(btn)->{
        NekoEntity neko = screen.getNeko();
        ClientPlayNetworking.send(new FollowOwnerPayload(neko.getUUID().toString()));
        neko.followOwner(Minecraft.getInstance().player);
    });
    public static ButtonFactory ACTION_RIDE_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.ride"),(btn)->{
        NekoEntity neko = screen.getNeko();
        // 让猫娘骑在最近的实体身上
        LivingEntity entity = EntityUtil.findNearestEntityInRange(neko, Minecraft.getInstance().player.level(),NekoEntity.DEFAULT_RIDE_RANGE);
        if (entity != null && entity != neko){
            if (neko.isSitting()){
                neko.stopRiding();
            }else {
                neko.startRiding(entity, true);
            }
            // 向服务器发包
            ClientPlayNetworking.send(new RideEntityPayload(neko.getUUID().toString(),entity.getUUID().toString()));
        }
    });
    public static ButtonFactory ACTION_LIE_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.lie"),(btn)->{
        NekoEntity neko = screen.getNeko();
        // 把猫娘设置为躺
        if (ClientEntityPoseManager.contains(neko)){
            ClientEntityPoseManager.remove(neko);
            neko.setPose(Pose.STANDING);
        }else {
            ClientEntityPoseManager.setPose(neko, Pose.SLEEPING);
            neko.setPose(Pose.SLEEPING);
        }
        ClientPlayNetworking.send(new NekoPosePayload(Pose.SLEEPING,neko.getUUID().toString(),true));
    });
    public static ButtonFactory ACTION_GET_DOWN_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.get_down"),(btn)->{
        NekoEntity neko = screen.getNeko();
        // 把猫娘设置为趴
        if (ClientEntityPoseManager.contains(neko)){
            ClientEntityPoseManager.remove(neko);
            neko.setPose(Pose.STANDING);
        }else {
            ClientEntityPoseManager.setPose(neko, Pose.SWIMMING);
            neko.setPose(Pose.SWIMMING);
        }
        ClientPlayNetworking.send(new NekoPosePayload(Pose.SWIMMING,neko.getUUID().toString(),true));
    });

    // ------------------------------------------------------ CrystalNeko ------------------------------------------------------------
    public static ButtonFactory CRYSTAL_NEKO_WHO_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.crystal_neko_interactive.button.who"),(btn)->{
        Player player = Minecraft.getInstance().player;
        TickTaskQueue messageQueue = new TickTaskQueue();
//        messageQueue.addTask(20, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.0")));
//        messageQueue.addTask(50, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.1")));  // 相对于上一个任务
//        messageQueue.addTask(80, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.2")));
//        messageQueue.addTask(120, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.3")));
//        messageQueue.addTask(170, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.4")));
//        messageQueue.addTask(210, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.5")));
//        messageQueue.addTask(270, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.who.6")));
//        TickTasks.addClient(messageQueue);
    });
    public static ButtonFactory CRYSTAL_NEKO_ABOUT_MOD_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.crystal_neko_interactive.button.about_mod"),(btn)->{
        Player player = Minecraft.getInstance().player;
        TickTaskQueue messageQueue = new TickTaskQueue();
//        messageQueue.addTask(20, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.0")));
//        messageQueue.addTask(90, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.1")));
//        messageQueue.addTask(140, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.2")));
//        messageQueue.addTask(190, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.3")));
//        messageQueue.addTask(230, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.4")));
//        messageQueue.addTask(260, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.5")));
//        messageQueue.addTask(300, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.6")));
//        messageQueue.addTask(345, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.7")));
//        messageQueue.addTask(380, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.8")));
//        messageQueue.addTask(425, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.9")));
//        messageQueue.addTask(455, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.10")));
//        messageQueue.addTask(490, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.11")));
//        messageQueue.addTask(535, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.12")));
//        messageQueue.addTask(585, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.13")));
//        messageQueue.addTask(625, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.14")));
//        messageQueue.addTask(660, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.15")));
//        messageQueue.addTask(705, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.about_mod.16")));
        TickTasks.addClient(messageQueue);
    });
    public static ButtonFactory CRYSTAL_NEKO_PLANS_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.crystal_neko_interactive.button.plans"),(btn)->{
        Player player = Minecraft.getInstance().player;
        TickTaskQueue messageQueue = new TickTaskQueue();
//        messageQueue.addTask(20, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.0")));
//        messageQueue.addTask(90, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.1")));
//        messageQueue.addTask(140, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.2")));
//        messageQueue.addTask(190, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.3")));
//        messageQueue.addTask(230, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.4")));
//        messageQueue.addTask(260, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.5")));
//        messageQueue.addTask(300, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.6")));
//        messageQueue.addTask(345, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.7")));
//        messageQueue.addTask(380, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.8")));
//        messageQueue.addTask(425, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.9")));
//        messageQueue.addTask(455, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.10")));
//        messageQueue.addTask(490, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.11")));
//        messageQueue.addTask(535, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.12")));
//        messageQueue.addTask(585, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.13")));
//        messageQueue.addTask(625, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.14")));
//        messageQueue.addTask(660, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.15")));
//        messageQueue.addTask(705, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.plans.16")));
        TickTasks.addClient(messageQueue);
    });
    public static ButtonFactory CRYSTAL_NEKO_LINKS = screen -> Button.builder(Component.translatable("screen.toneko.crystal_neko_interactive.button.links"),(btn)->{
        Minecraft.getInstance().setScreen(new InteractionScreen(Component.empty(), screen.getNeko(),screen,ScreenBuilders.LINKS_SCREEN));
    });
    public static ButtonFactory CRYSTAL_NEKO_INTERACTIVE = screen -> Button.builder(Component.translatable("screen.toneko.crystal_neko_interactive.button.interactive"),(btn)->{
        InteractionScreen i = new InteractionScreen(Component.empty(), screen.getNeko(), screen,ScreenBuilders.COMMON_INTERACTION_SCREEN);
        Minecraft.getInstance().setScreen(i);
    });
    public static ButtonFactory CRYSTAL_NEKO_MORE_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.crystal_neko_interactive.button.more"),(btn)->{
        Minecraft.getInstance().setScreen(new InteractionScreen(Component.empty(), screen.getNeko(),screen,ScreenBuilders.CRYSTAL_NEKO_MORE_INTERACTION_SCREEN));
    });
    public static ButtonFactory CRYSTAL_NEKO_BREED_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.breed"),(btn)->{
        Player player = Minecraft.getInstance().player;
        CrystalNekoEntity neko = (CrystalNekoEntity) screen.getNeko();
        if (player.getUUID().equals(CrystalNekoEntity.CRYSTAL_NEKO_UUID) || player.getName().getString().equalsIgnoreCase(CrystalNekoEntity.NAME)){
            ClientPlayNetworking.send(new MateWithCrystalNekoPayload(neko.getUUID().toString()));
        }else if(neko.getMoeTags().contains("mesugaki")){
            if (!player.getMainHandItem().is(ToNekoItems.CATNIP)) {
                // 杂鱼，你还不配和我交配~
                btn.setPosition(new Random().nextInt(screen.width - btn.getWidth()), new Random().nextInt(screen.height - btn.getHeight()));
//                player.sendSystemMessage(TextUtil.randomTranslatabledComponent("message.toneko.neko.breed_fail_zako", 10, neko.getName().getString()));
            }else {
                // 哪只猫猫会拒绝猫薄荷呢
                NekoMateScreen.open(neko,List.of(player),null);
                //player.sendSystemMessage(TextUtil.randomTranslatabledComponent("message.toneko.neko.breed_success_zako", 3, neko.getName().getString()));
            }
        }else {
            TickTaskQueue messageQueue = new TickTaskQueue();
//            messageQueue.addTask(20, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.interactive.breed.0")));
//            messageQueue.addTask(50, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.interactive.breed.1")));
//            messageQueue.addTask(90, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.interactive.breed.2")));
//            messageQueue.addTask(120, () -> player.sendSystemMessage(Component.translatable("message.toneko.crystal_neko.interactive.breed.3")));
            TickTasks.addClient(messageQueue);
        }
    });
    public static ButtonFactory CRYSTAL_NEKO_MORE_INTERACTION_NYA_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.crystal_neko_more_interactive.button.nya"),(btn)->{
        ((CrystalNekoEntity)screen.getNeko()).nya(Minecraft.getInstance().player);
    });

    // --------------------------------- 链接 ---------------------------------
    public static ButtonFactory LINKS_GITHUB_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.links.button.github"),(btn)-> Util.getPlatform().openUri("https://github.com/CSneko/toNeko"));
    public static ButtonFactory LINKS_MODRINTH_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.links.button.modrinth"),(btn)-> Util.getPlatform().openUri("https://modrinth.com/mod/tonekomod"));
    public static ButtonFactory LINKS_DISCORD_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.links.button.discord"),(btn)-> Util.getPlatform().openUri("https://discord.gg/hQ6Mm7wtt4"));
    public static ButtonFactory LINKS_BILIBILI_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.links.button.bilibili"),(btn)-> Util.getPlatform().openUri("https://space.bilibili.com/3461580710742160"));
}
