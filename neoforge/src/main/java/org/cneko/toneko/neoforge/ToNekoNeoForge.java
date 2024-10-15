package org.cneko.toneko.neoforge;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.cneko.toneko.common.mod.commands.NekoCommand;
import org.cneko.toneko.common.mod.commands.QuirkCommand;
import org.cneko.toneko.common.mod.commands.ToNekoAdminCommand;
import org.cneko.toneko.common.mod.commands.ToNekoCommand;
import org.cneko.toneko.common.mod.events.ToNekoEvents;
import org.cneko.toneko.common.mod.events.ToNekoNetworkEvents;
import org.cneko.toneko.common.mod.packets.EntityPosePayload;
import org.cneko.toneko.common.mod.packets.QuirkQueryPayload;
import org.cneko.toneko.common.mod.packets.ToNekoPackets;
import org.cneko.toneko.common.mod.packets.VehicleStopRidePayload;
import org.cneko.toneko.common.mod.packets.interactives.*;
import org.cneko.toneko.neoforge.msic.ToNekoAttributes;
import org.cneko.toneko.neoforge.items.ToNekoArmorMaterials;
import org.cneko.toneko.neoforge.items.ToNekoItems;
import net.minecraft.world.entity.ai.attributes.Attribute;

import static org.cneko.toneko.common.Bootstrap.MODID;


@Mod(MODID)
public final class ToNekoNeoForge {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, MODID);
    public ToNekoNeoForge(IEventBus bus, ModContainer container) {
        ITEMS.register(bus);
        DATA_COMPONENTS.register(bus);
        ARMOR_MATERIALS.register(bus);
        ATTRIBUTES.register(bus);
        // 注册装备
        ToNekoArmorMaterials.init();
        ToNekoItems.init();
        bus.addListener(ToNekoItems::buildContents);
        ToNekoAttributes.init();
        bus.addListener(ToNekoAttributes::onRegisterAttributes);
        ToNekoEvents.init();

        // 注册网络数据包
        ToNekoPackets.init();

        ToNekoNetworkEvents.init();
        ToNekoCommand.init();
        NekoCommand.init();
        QuirkCommand.init();
        ToNekoAdminCommand.init();

        //new ToNeko().onInitialize();

    }
}
