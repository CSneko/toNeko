package com.crystalneko.tonekofabric;

import com.crystalneko.ctlibPublic.File.YamlConfiguration;
import com.crystalneko.tonekofabric.command.command;
import com.crystalneko.tonekofabric.entity.nekoEntity;
import com.crystalneko.tonekofabric.event.*;
import com.crystalneko.tonekofabric.items.stick;
import com.crystalneko.tonekofabric.libs.base;
import com.crystalneko.tonekofabric.libs.lp;
import com.crystalneko.tonekofabric.test.testCommand;
import com.crystalneko.tonekofabric.test.testItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;

import java.io.IOException;
import java.nio.file.Path;

public class ToNekoFabric implements ModInitializer {
    public static Boolean started = false;
    private command command;
    public static YamlConfiguration config;


    public static EntityType<nekoEntity> NEKO = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier("toneko", "neko"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, nekoEntity::new).dimensions(EntityDimensions.fixed(0.6f, 2.0f)).build()
    );
    public static final Item NEKO_SPAWN_EGG = new SpawnEggItem(NEKO, 0xc4c4c4, 0xadadad, new Item.Settings());

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
        Registry.register(Registries.ITEM, new Identifier("toneko", "neko_spawn_egg"), NEKO_SPAWN_EGG);

        //注册实体
        FabricDefaultAttributeRegistry.register(NEKO, nekoEntity.createMobAttributes());

        //设置实体刷新规则
        SpawnRestriction.register(NEKO, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, nekoEntity::canMobSpawn);
        BiomeModifications.addSpawn(BiomeSelectors.all(), SpawnGroup.CREATURE, NEKO, 5, 1,3);



        //注册权限组
        new lp();

        try {
            Class.forName("com.crystalneko.tonekofabric.test.testItem");
            new testCommand();
            testItem testItemT = new testItem(new Item.Settings());
            Registry.register(Registries.ITEM,new Identifier("toneko","empty"),testItemT);
        }catch (ClassNotFoundException ignored) {}
        //加载配置文件
        try {
            config = new YamlConfiguration(Path.of("ctlib/toneko/config.yml"));
        } catch (IOException e) {
            System.out.println("无法加载配置文件" + e.getMessage());
        }
    }
    //监听事件
    private void event(){
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(content -> {
            content.add(NEKO_SPAWN_EGG);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            new playerAttack();
            new playerJoin();
            new playerLeave();
            if (config.getBoolean("chat.enable")) {
                playerChat.init();
            }

            try {
                Class.forName("com.crystalneko.tonekofabric.test.event");
                System.out.println("成功启动测试版本");
                new com.crystalneko.tonekofabric.test.event();
            } catch (ClassNotFoundException ignored) {}
        });
    }
}
