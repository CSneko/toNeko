package org.cneko.toneko.neoforge;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.NeoForge;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.mod.ModBootstrap;
import org.cneko.toneko.common.mod.ModMeta;
import org.cneko.toneko.common.mod.commands.*;
import org.cneko.toneko.common.mod.events.ToNekoEvents;
import org.cneko.toneko.common.mod.events.ToNekoNetworkEvents;
import org.cneko.toneko.common.mod.genetics.api.GeneticsDataLoader;
import org.cneko.toneko.common.mod.impl.FabricLanguageImpl;
import org.cneko.toneko.common.mod.packets.ToNekoPackets;
import org.cneko.toneko.common.mod.quirks.ToNekoQuirks;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.neoforge.entities.ToNekoEntities;
import org.cneko.toneko.neoforge.items.NekoArmorCurios;
import org.cneko.toneko.neoforge.items.ToNekoArmorMaterials;
import org.cneko.toneko.neoforge.items.ToNekoBlockEntities;
import org.cneko.toneko.neoforge.items.ToNekoBlocks;
import org.cneko.toneko.neoforge.items.ToNekoItems;
import org.cneko.toneko.neoforge.msic.*;

import static org.cneko.toneko.common.Bootstrap.MODID;


@Mod(MODID)
public final class ToNekoNeoForge {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE,MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<CriterionTrigger<?>> CRITERION_TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, "minecraft");
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public ToNekoNeoForge(IEventBus bus, ModContainer container) {
        // 初始化语言和配置
        LanguageUtil.INSTANCE = new FabricLanguageImpl();
        // 通用的启动
        Bootstrap.bootstrap();
        ModBootstrap.bootstrap();

        ITEMS.register(bus);
        MOB_EFFECTS.register(bus);
        BLOCKS.register(bus);
        DATA_COMPONENTS.register(bus);
        ATTRIBUTES.register(bus);
        ENTITY_TYPES.register(bus);
        CREATIVE_MODE_TABS.register(bus);
        CRITERION_TRIGGERS.register(bus);
        MENU_TYPES.register(bus);
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        // 注册装备
        ToNekoArmorMaterials.init();
        ToNekoItems.init();
        ToNekoEffectNeoForge.init();
        ToNekoBlocks.init();
        ToNekoBlockEntities.init();
        ToNekoAttributes.init();
        ToNekoCriteriaNeoForge.init();
        ToNekoRecipesNeo.init(bus);
        ToNekoMenuTypesNeo.init(bus);
        // 注册Quirks
        ToNekoQuirks.init();
        bus.addListener(ToNekoAttributes::onRegisterAttributes);
        bus.addListener(ToNekoAttributes::registerAttributes);
        bus.addListener(ToNekoEntities::onCreatureSpawn);
        ToNekoEvents.init();
        ToNekoEntities.init();

        // 注册遗传学数据包加载器（支持 /reload 热重载）
        // 26.1.2：AddReloadListenerEvent 改名 AddServerReloadListenersEvent，注册需带 id
        NeoForge.EVENT_BUS.addListener(AddServerReloadListenersEvent.class, event -> {
            event.addListener(net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, "genetics"), new GeneticsDataLoader());
        });

        // 注册网络数据包
        ToNekoPackets.init();

        ToNekoNetworkEvents.init();
        ToNekoCommand.init();
        NekoCommand.init();
        QuirkCommand.init();
        ToNekoAdminCommand.init();
        GeneticsCommand.init();

        // 注册箱子战利品注入
        ChestLootInjection.init();

        // Curios 集成（可选依赖）：把猫耳/猫尾/猫爪注册为饰品行为
        // （属性 +0.05 猫娘度等），客户端渲染在 ToNekoNeoForgeClient 中注册
        if (ModList.get().isLoaded("curios")) {
            bus.addListener(FMLCommonSetupEvent.class,
                    event -> event.enqueueWork(NekoArmorCurios::init));
        }

        // 26.1.2：主动绑定模组物品的默认组件（配方解析需要，见 ComponentBinding 注释）
        bus.addListener(FMLCommonSetupEvent.class,
                event -> event.enqueueWork(org.cneko.toneko.common.mod.util.ComponentBinding::bindAll));

        // 26.1.2：服务器启动完成后重载资源，使配方管理器在组件绑定完成后重新解析
        bus.addListener(net.neoforged.neoforge.event.server.ServerStartedEvent.class,
                event -> org.cneko.toneko.common.mod.util.ComponentBinding.reloadRecipesAfterStart(event.getServer()));

        // 注册权限
        PermissionUtil.init();

        // 在所有注册表完成后：解析 DeferredRegister 值并注册群系生成
        bus.addListener(FMLLoadCompleteEvent.class, event -> {
            ToNekoItems.reg();
            ToNekoBlockEntities.reg();
            // 调用 common 中的群系生成注册（使用 FFAPI 桥接的 BiomeModifications）
            org.cneko.toneko.common.mod.entities.ToNekoEntities.registerBiomeSpawns(
                    ToNekoEntities.ADVENTURER_NEKO_HOLDER.get(),
                    ToNekoEntities.GHOST_NEKO_HOLDER.get(),
                    ToNekoEntities.CRYSTAL_NEKO_HOLDER.get(),
                    ToNekoEntities.FIGHTING_NEKO_HOLDER.get()
            );
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModMeta.INSTANCE.setServer(server);
        });
    }

}
