package com.crystalneko.tonekofabric;

import com.crystalneko.tonekofabric.command.command;
import com.crystalneko.tonekofabric.event.playerAttack;
import com.crystalneko.tonekofabric.items.stick;
import com.crystalneko.tonekofabric.libs.base;
import com.crystalneko.tonekofabric.libs.lp;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ToNekoFabric implements ModInitializer {
    private command command;
    //----------------------------------------------------------物品----------------------------------------------------
    public static final Item STICK = new stick(new FabricItemSettings()); //撅猫棍

    /**
     * 运行模组 initializer.
     */
    @Override
    public void onInitialize() {
        new base();
        //注册命令
        this.command = new command();
        //--------------------------------------------------------物品-------------------------------------------------
        Registry.register(Registries.ITEM, new Identifier("tonekofabric", "stick"), STICK);

        //注册简易监听事件
        event();

        //注册权限组
        new lp();
    }
    //简易的监听事件
    private void event(){
        new playerAttack();
        //物品组
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.add(STICK);
        });
    }
}
