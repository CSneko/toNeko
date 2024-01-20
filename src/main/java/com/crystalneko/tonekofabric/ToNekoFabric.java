package com.crystalneko.tonekofabric;

import com.crystalneko.tonekofabric.command.command;
import com.crystalneko.tonekofabric.entity.neko.nekoEntity;
import com.crystalneko.tonekofabric.event.*;
import com.crystalneko.tonekofabric.items.stick;
import com.crystalneko.tonekofabric.libs.base;
import com.crystalneko.tonekofabric.libs.lp;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ToNekoFabric implements ModInitializer {
    public static Boolean started = false;
    private command command;
    public static EntityType<nekoEntity> NEKO = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier("toneko", "neko"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, nekoEntity::new).dimensions(EntityDimensions.fixed(0.75f, 0.75f)).build()
    );

    /**
     * 运行模组 initializer.
     */
    @Override
    public void onInitialize() {
        new base();
        //注册命令
        this.command = new command();

        //注册监听事件
        event();

        //注册物品
        new stick();

        //注册实体
        FabricDefaultAttributeRegistry.register(NEKO, nekoEntity.createMobAttributes());

        //注册权限组
        new lp();
    }
    //简易的监听事件
    private void event(){
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            new playerAttack();
            new playerJoin();
            new playerLeave();
            playerChat.init();
        });
    }
}
