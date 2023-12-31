package com.crystalneko.toneko.event;

import com.crystalneko.toneko.ToNeko;
import com.crystalneko.toneko.files.create;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.Random;

import static org.bukkit.Bukkit.getServer;

public class PlayerDeath implements Listener {
    private ToNeko plugin;
    public PlayerDeath(ToNeko plugin){
        this.plugin = plugin;
        //注册玩家死亡监听器
        getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        File dataFile = new File( "plugins/toNeko/nekos.yml");
        // 加载数据文件
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        // 处理玩家死亡事件的逻辑
        Player player = event.getEntity();
        if (player.getKiller() instanceof Player) {
            // 获取击杀者
            Player killer = player.getKiller();
            // 获取击杀者使用的物品
            ItemStack itemStack = killer.getInventory().getItemInMainHand();
            if(itemStack != null){
                // 获取物品的ItemMeta对象
                ItemMeta itemMeta = itemStack.getItemMeta();
                //定义NBT标签
                NamespacedKey key = new NamespacedKey(plugin, "neko");
                // 检查物品是否含有该自定义NBT标签
                if (itemMeta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                    // 读取NBT标签的值
                    String nbtValue = itemMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                    //判断NBT是否正确
                    if (nbtValue.equals("true")){
                        //判断玩家是否为猫娘
                        if(data.getString(player.getName() + ".owner") != null) {
                            //发送死亡提示
                            String deathMessage = "猫娘 " + player.getName() + " 被 " + killer.getName() + " §f撅死了！";
                            event.setDeathMessage(deathMessage);
                            //判断是否为主人
                            if(killer.getName().equals(data.getString(player.getName() + ".owner"))){
                                //生成随机数
                                Random random = new Random();
                                int randomNumber = random.nextInt(7) + 3;
                                //检查配置是否存在
                                create.createNewKey(player.getName() + "." + "xp", 0, data, dataFile);
                                //减去值
                                int xpValue = data.getInt(player.getName() + ".xp") - randomNumber;
                                create.setValue(player.getName() + ".xp",xpValue,dataFile);
                                player.sendMessage(ToNeko.getMessage("death.sub-xp",new String[]{killer.getName(), String.valueOf(randomNumber)}));
                                killer.sendMessage(ToNeko.getMessage("death.sub-xp",new String[]{player.getName(), String.valueOf(randomNumber)}));
                            }
                        }
                    }
                }
            }else {
                //发送正常死亡提示
                String deathMessage = player.getName() + " 被 " + killer.getName() + " 用 " + itemStack.getItemMeta().getDisplayName() + "§f击杀了";
                event.setDeathMessage(deathMessage);
            }
        }
    }
}
