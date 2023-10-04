package com.crystalneko.toneko.event;

import com.crystalneko.toneko.ToNeko;
import com.crystalneko.toneko.files.create;
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
import java.util.Random;

import static org.bukkit.Bukkit.getServer;

public class PlayerAttack implements Listener {
    private ToNeko plugin;

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
                if (itemStack != null) {
                    // 获取物品的ItemMeta对象
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    //定义NBT标签
                    NamespacedKey key = new NamespacedKey(plugin, "neko");
                    NamespacedKey key2 = new NamespacedKey(plugin, "nekolevel");
                    // 检查物品是否含有该自定义NBT标签
                    if (itemMeta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        // 读取NBT标签的值
                        String nbtValue = itemMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                        //判断NBT是否正确
                        if (nbtValue.equals("true")) {
                            if(data.getString(player.getName() + ".owner") != null) {
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
                                    player.sendMessage("§c你与§e" + killer.getName() + "§c的好感经验增加了" + randomNumber);
                                    killer.sendMessage("§c你与§e" + player.getName() + "§c的好感经验增加了" + randomNumber);
                                }
                            }
                        }
                    } else if(itemMeta.getPersistentDataContainer().has(key2, PersistentDataType.INTEGER)){
                        int nbtValue2 = itemMeta.getPersistentDataContainer().get(key2, PersistentDataType.INTEGER);
                        if(nbtValue2  == 2) {
                            PotionEffectType effectType = PotionEffectType.WEAKNESS; // 虚弱效果的类型
                            PotionEffectType effectType2 = PotionEffectType.BLINDNESS; //失明
                            PotionEffectType effectType3 = PotionEffectType.SLOW; //缓慢
                            PotionEffectType effectType4 = PotionEffectType.DARKNESS; //黑暗
                            int duration = 2000; // 持续时间
                            int amplifier = 5; // 效果强度
                            givePlayerPotionEffect(player, effectType, duration, amplifier);
                            givePlayerPotionEffect(player, effectType2, duration, amplifier);
                            givePlayerPotionEffect(player, effectType3, duration, amplifier);
                            givePlayerPotionEffect(player, effectType4, duration, amplifier);
                            //判断是否为主人
                            if (killer.getName().equals(data.getString(player.getName() + ".owner"))) {
                                //生成随机数
                                Random random = new Random();
                                int randomNumber = random.nextInt(6) + 2;
                                //检查配置是否存在
                                create.createNewKey(player.getName() + "." + "xp", 0, data, dataFile);
                                //加上值
                                int xpValue = data.getInt(player.getName() + ".xp") + randomNumber +10;
                                create.setValue(player.getName() + ".xp", xpValue, dataFile);
                                player.sendMessage("§c你与§e" + killer.getName() + "§c的好感经验增加了" + randomNumber);
                                killer.sendMessage("§c你与§e" + player.getName() + "§c的好感经验增加了" + randomNumber);
                            }
                        }
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


}
