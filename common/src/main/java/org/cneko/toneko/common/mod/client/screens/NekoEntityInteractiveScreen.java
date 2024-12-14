package org.cneko.toneko.common.mod.client.screens;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.items.ToNekoItems;
import org.cneko.toneko.common.mod.packets.interactives.*;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.toneko.common.mod.util.TextUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NekoEntityInteractiveScreen extends InteractionScreen implements INekoScreen{
    private final @NotNull NekoEntity neko;
    public NekoEntityInteractiveScreen(@NotNull NekoEntity neko, @Nullable Screen lastScreen,@Nullable ButtonBuilders builders) {
        // 调用父类构造函数
        super(Component.empty(), lastScreen, getBuilders(neko, builders));

        this.neko = neko;
    }
    public NekoEntityInteractiveScreen(@NotNull NekoEntity neko, @Nullable Screen lastScreen){
        this(neko,lastScreen,null);
    }
    private static ButtonBuilders getBuilders(NekoEntity neko, @Nullable ButtonBuilders builders) {
        if (builders == null) {
            return screen -> getButtonBuilders(screen,neko);
        }
        return builders;
    }

    @Override
    public void init() {
        this.startY = (int) (this.height * 0.1)+40;
        super.init();
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        int x = (int) (this.width * 0.7);
        int y = (int) (this.height * 0.1);
        // 显示猫娘的名字
        gui.renderTooltip(this.font,Component.translatable("screen.toneko.neko_entity_interactive.tooltip.name",neko.getCustomName().getString()),x,y);
        y += 20;
        // 显示猫娘的萌属性
        gui.renderTooltip(this.font,Component.translatable("screen.toneko.neko_entity_interactive.tooltip.moe_tags",neko.getMoeTagsString()),x,y);
    }

    @Override
    public @NotNull NekoEntity getNeko() {
        return neko;
    }

    public static Map<String,Button.Builder> getButtonBuilders(Screen screen,NekoEntity neko) {
        Map<String,Button.Builder> builders = new LinkedHashMap<>();

        builders.put("screen.toneko.neko_entity_interactive.button.chat",Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.chat"),(btn)->{
            Minecraft.getInstance().setScreen(new ChatWithNekoScreen(neko));
        }));

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
            if (neko.isBaby()){
                int i = new Random().nextInt(13);
                player.sendSystemMessage(Component.translatable("message.toneko.neko.breed_fail_baby."+i));
                return;
            }
            if (neko.getMoeTags().contains("mesugaki")){
                if (!player.getMainHandItem().is(ToNekoItems.CATNIP)) {
                    // 杂鱼，你还不配和我交配~
                    btn.setPosition(new Random().nextInt(screen.width - btn.getWidth()), new Random().nextInt(screen.height - btn.getHeight()));
                    player.sendSystemMessage(TextUtil.randomTranslatabledComponent("message.toneko.neko.breed_fail_zako", 10, neko.getName().getString()));
                }else {
                    // 哪只猫猫会拒绝猫薄荷呢
                    NekoMateScreen.open(neko,List.of(player),null);
                    player.sendSystemMessage(TextUtil.randomTranslatabledComponent("message.toneko.neko.breed_success_zako", 3, neko.getName().getString()));
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
        }));

        return builders;
    }

}
