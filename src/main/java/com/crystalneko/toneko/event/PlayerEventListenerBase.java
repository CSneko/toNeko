package com.crystalneko.toneko.event;

import com.crystalneko.ctlib.chat.chatPrefix;
import com.crystalneko.ctlibPublic.network.httpGet;
import com.crystalneko.ctlibPublic.sql.sqlite;
import com.crystalneko.toneko.ToNeko;
import com.crystalneko.toneko.utils.ConfigFileUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEventListenerBase implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        //创建数据文件实例
        File dataFile = new File( "plugins/toNeko/nekos.yml");
        // 加载数据文件
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        Player player = event.getPlayer();
        //判断是否有主人
        if(data.getString(player.getName() + ".owner") != null) {
            //添加前缀
            chatPrefix.addPrivatePrefix(player, ToNeko.getMessage("other.neko"));
        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        //创建数据文件实例
        File dataFile = new File( "plugins/toNeko/nekos.yml");
        // 加载数据文件
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        Player player = event.getPlayer();
        if(data.getString(player.getName() + ".owner") != null){
            //删除前缀
            chatPrefix.subPrivatePrefix(player, ToNeko.getMessage("other.neko"));
        }
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
                NamespacedKey key = new NamespacedKey(ToNeko.pluginInstance, "neko");
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
                                ConfigFileUtils.createNewKey(player.getName() + "." + "xp", 0, data, dataFile);
                                //减去值
                                int xpValue = data.getInt(player.getName() + ".xp") - randomNumber;
                                ConfigFileUtils.setValue(player.getName() + ".xp",xpValue,dataFile);
                                player.sendMessage(ToNeko.getMessage("death.sub-xp",new String[]{killer.getName(), String.valueOf(randomNumber)}));
                                killer.sendMessage(ToNeko.getMessage("death.sub-xp",new String[]{player.getName(), String.valueOf(randomNumber)}));
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        // 检查是否是玩家被攻击
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (event.getDamager() instanceof Player) {
                //创建数据文件实例
                File dataFile = new File("plugins/toNeko/nekos.yml");
                // 加载数据文件
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
                // 获取击杀者
                Player killer = (Player) event.getDamager();
                // 检查击杀者手中的物品是否为空或非武器
                ItemStack itemStack = killer.getInventory().getItemInMainHand();
                if (itemStack == null || !itemStack.getType().isItem()) {
                    return;
                }
                // 获取物品的ItemMeta对象
                ItemMeta itemMeta = itemStack.getItemMeta();
                //定义NBT标签
                NamespacedKey key = new NamespacedKey(ToNeko.pluginInstance, "neko");
                NamespacedKey key2 = new NamespacedKey(ToNeko.pluginInstance, "nekolevel");
                // 检查物品是否含有该自定义NBT标签
                if (itemMeta != null && itemMeta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                    // 读取NBT标签的值
                    String nbtValue = itemMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                    //判断NBT是否正确
                    if (nbtValue.equalsIgnoreCase("true")) {
                        if (data.getString(player.getName() + ".owner") != null) {
                            PotionEffectType effectType = PotionEffectType.WEAKNESS; // 虚弱效果的类型
                            int duration = 200; // 持续时间
                            int amplifier = 0; // 效果强度
                            givePlayerPotionEffect(player, effectType, duration, amplifier);
                            //判断是否为主人
                            if (killer.getName().equals(data.getString(player.getName() + ".owner"))) {
                                //生成随机数
                                Random random = new Random();
                                int randomNumber = random.nextInt(6) - 2;
                                //检查配置是否存在
                                ConfigFileUtils.createNewKey(player.getName() + "." + "xp", 0, data, dataFile);
                                //加上值
                                int xpValue = data.getInt(player.getName() + ".xp") + randomNumber;
                                ConfigFileUtils.setValue(player.getName() + ".xp", xpValue, dataFile);
                                player.sendMessage(ToNeko.getMessage("attack.add-xp",new String[]{killer.getName(), String.valueOf(randomNumber)}));
                                killer.sendMessage(ToNeko.getMessage("attack.add-xp",new String[]{player.getName(), String.valueOf(randomNumber)}));
                            }

                            //发送撅人音效
                            player.getWorld().playSound(player.getLocation(), "toneko.neko.stick", 1, 1);
                            killer.getWorld().playSound(player.getLocation(), "toneko.neko.stick", 1, 1);
                            // 添加统计
                            addStatistic(player.getName(), killer.getName());

                        }
                    }
                } else if(itemMeta != null && itemMeta.getPersistentDataContainer().has(key2, PersistentDataType.INTEGER)){
                    int nbtValue2 = itemMeta.getPersistentDataContainer().get(key2, PersistentDataType.INTEGER);
                    if(nbtValue2  == 2) {
                        PotionEffectType effectType = PotionEffectType.WEAKNESS; // 虚弱效果的类型
                        PotionEffectType effectType2 = PotionEffectType.BLINDNESS; //失明
                        PotionEffectType effectType3 = PotionEffectType.SLOW; //缓慢
                        PotionEffectType effectType4 = PotionEffectType.DARKNESS; //黑暗
                        int duration = 1000; // 持续时间
                        int amplifier = 3; // 效果强度
                        givePlayerPotionEffect(player, effectType, duration, amplifier);
                        givePlayerPotionEffect(player, effectType2, duration, amplifier);
                        givePlayerPotionEffect(player, effectType3, duration, amplifier);
                        givePlayerPotionEffect(player, effectType4, duration, amplifier);
                        //判断是否为主人
                        if (killer.getName().equals(data.getString(player.getName() + ".owner"))) {
                            //生成随机数
                            Random random = new Random();
                            int randomNumber = random.nextInt(14) + 2;
                            //检查配置是否存在
                            ConfigFileUtils.createNewKey(player.getName() + "." + "xp", 0, data, dataFile);
                            //加上值
                            int xpValue = data.getInt(player.getName() + ".xp") + randomNumber +10;
                            ConfigFileUtils.setValue(player.getName() + ".xp", xpValue, dataFile);
                            player.sendMessage(ToNeko.getMessage("attack.add-xp",new String[]{killer.getName(), String.valueOf(randomNumber)}));
                            killer.sendMessage(ToNeko.getMessage("attack.add-xp",new String[]{player.getName(), String.valueOf(randomNumber)}));
                            // 添加统计
                            addStatistic(player.getName(), killer.getName());
                        }
                        //发送撅人音效
                        player.getWorld().playSound(player.getLocation(),"toneko.neko.stick.level2",1,1);
                        killer.getWorld().playSound(player.getLocation(),"toneko.neko.stick.level2",1,1);
                    }
                }
            }
        }
    }

    // 给予玩家药水效果的方法
    private void givePlayerPotionEffect(Player player, PotionEffectType type, int duration, int amplifier) {
        PotionEffect effect = new PotionEffect(type, duration, amplifier);
        player.addPotionEffect(effect);
    }

    //判断是否满足统计条件
    // 这段代码目前暂无用处
    /*public static Boolean isStatistic(String name, int threshold){
        if (!statistics.containsKey(name)) {
            statistics.put(name, 1); // 如果名称不存在，则将其赋值为1
        } else {
            int value = statistics.get(name); // 获取名称对应的值
            value++; // 假设每次更新值加1
            statistics.put(name, value); // 更新值

            // 判断值是否达到阈值
            if (value >= threshold) {
                statistics.remove(name); // 清除值
                return true;
            }
        }
        return false;
    }*/
    public void addStatistic(String neko,String player ){
        try {
            httpGet.get("toneko.cneko.org/stick?neko="+neko+"player="+player,null);
        } catch (IOException ignored) {
        }
    }
}
