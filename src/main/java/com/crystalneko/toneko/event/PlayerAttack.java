package com.crystalneko.toneko.event;

import com.crystalneko.toneko.ToNeko;
import com.crystalneko.toneko.files.create;
import com.crystalneko.toneko.other.APIs;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.bukkit.Bukkit.getServer;

public class PlayerAttack implements Listener {
    private ToNeko plugin;
    public static Map<String, Integer> statistics = new HashMap<>(); // 存储多个名称和对应的值

    public PlayerAttack(ToNeko plugin) {
        this.plugin = plugin;
        getServer().getPluginManager().registerEvents(this,plugin );
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
                    NamespacedKey key = new NamespacedKey(plugin, "neko");
                    NamespacedKey key2 = new NamespacedKey(plugin, "nekolevel");
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
                                    create.createNewKey(player.getName() + "." + "xp", 0, data, dataFile);
                                    //加上值
                                    int xpValue = data.getInt(player.getName() + ".xp") + randomNumber;
                                    create.setValue(player.getName() + ".xp", xpValue, dataFile);
                                    player.sendMessage(ToNeko.getMessage("attack.add-xp",new String[]{killer.getName(), String.valueOf(randomNumber)}));
                                    killer.sendMessage(ToNeko.getMessage("attack.add-xp",new String[]{player.getName(), String.valueOf(randomNumber)}));
                                }
                                //添加统计信息
                               /* if(isStatistic(player.getName(),5)){
                                    addStatistics(player.getName(),killer.getName(),"neko");
                                }
                                if(isStatistic(killer.getName(),5)){
                                    addStatistics(player.getName(),killer.getName(),"killer");
                                }*/
                                //发送撅人音效
                                player.getWorld().playSound(player.getLocation(), "toneko.neko.stick", 1, 1);
                                killer.getWorld().playSound(player.getLocation(), "toneko.neko.stick", 1, 1);


                            }
                        }
                    } else if(itemMeta.getPersistentDataContainer().has(key2, PersistentDataType.INTEGER)){
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
                                create.createNewKey(player.getName() + "." + "xp", 0, data, dataFile);
                                //加上值
                                int xpValue = data.getInt(player.getName() + ".xp") + randomNumber +10;
                                create.setValue(player.getName() + ".xp", xpValue, dataFile);
                                player.sendMessage(ToNeko.getMessage("attack.add-xp",new String[]{killer.getName(), String.valueOf(randomNumber)}));
                                killer.sendMessage(ToNeko.getMessage("attack.add-xp",new String[]{player.getName(), String.valueOf(randomNumber)}));
                            }
                            //添加统计信息
                            /*if(isStatistic(player.getName(),5)){
                                addStatistics(player.getName(),killer.getName(),"neko");
                            }
                            if(isStatistic(killer.getName(),5)){
                                addStatistics(player.getName(),killer.getName(),"killer");
                            }*/
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
    public static Boolean isStatistic(String name, int threshold){
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
    }
    //添加统计信息
    public static void addStatistics(String neko,String player,String type) {
        try {
            // 创建URL对象，指定要发送GET请求的URL地址
            URL url = new URL(APIs.onlineService + APIs.addStick + "?neko="+neko+"&&player="+player+"&&type=" + type);

            // 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置请求方法为GET
            connection.setRequestMethod("GET");

            // 发送GET请求
            connection.getResponseCode();
            // 关闭连接
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
