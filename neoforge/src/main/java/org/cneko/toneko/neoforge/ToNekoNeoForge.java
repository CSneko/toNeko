package org.cneko.toneko.neoforge;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.cneko.toneko.common.Bootstrap;
import org.cneko.toneko.common.mod.ModBootstrap;
import org.cneko.toneko.common.mod.ModMeta;
import org.cneko.toneko.common.mod.commands.NekoCommand;
import org.cneko.toneko.common.mod.commands.QuirkCommand;
import org.cneko.toneko.common.mod.commands.ToNekoAdminCommand;
import org.cneko.toneko.common.mod.commands.ToNekoCommand;
import org.cneko.toneko.common.mod.events.ToNekoEvents;
import org.cneko.toneko.common.mod.events.ToNekoNetworkEvents;
import org.cneko.toneko.common.mod.impl.FabricLanguageImpl;
import org.cneko.toneko.common.mod.packets.ToNekoPackets;
import org.cneko.toneko.common.mod.util.PermissionUtil;
import org.cneko.toneko.common.util.ConfigUtil;
import org.cneko.toneko.common.util.LanguageUtil;
import org.cneko.toneko.neoforge.entities.ToNekoEntities;
import org.cneko.toneko.neoforge.items.ToNekoBlocks;
import org.cneko.toneko.neoforge.msic.ToNekoAttributes;
import org.cneko.toneko.neoforge.items.ToNekoArmorMaterials;
import org.cneko.toneko.neoforge.items.ToNekoItems;
import net.minecraft.world.entity.ai.attributes.Attribute;

import static org.cneko.toneko.common.Bootstrap.MODID;


@Mod(MODID)
public final class ToNekoNeoForge {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public ToNekoNeoForge(IEventBus bus, ModContainer container) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
        DATA_COMPONENTS.register(bus);
        ARMOR_MATERIALS.register(bus);
        ATTRIBUTES.register(bus);
        ENTITY_TYPES.register(bus);
        CREATIVE_MODE_TABS.register(bus);
        // 注册装备
        ToNekoArmorMaterials.init();
        ToNekoItems.init();
        ToNekoBlocks.init();
        ToNekoAttributes.init();
        bus.addListener(ToNekoAttributes::onRegisterAttributes);
        bus.addListener(ToNekoAttributes::registerAttributes);
        ToNekoEvents.init();
        ToNekoEntities.init();
        //bus.addListener(ToNekoEntities::onCreatureSpawn);

        // 注册网络数据包
        ToNekoPackets.init();

        ToNekoNetworkEvents.init();
        ToNekoCommand.init();
        NekoCommand.init();
        QuirkCommand.init();
        ToNekoAdminCommand.init();

        // 注册权限
        PermissionUtil.init();
        // 初始化语言和配置
        LanguageUtil.INSTANCE = new FabricLanguageImpl();
        // 通用的启动
        Bootstrap.bootstrap();
        ModBootstrap.bootstrap();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModMeta.INSTANCE.setServer(server);
            ToNekoItems.reg();
        });
    }

}
