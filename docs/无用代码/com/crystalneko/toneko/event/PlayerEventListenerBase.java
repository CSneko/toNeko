package com.crystalneko.toneko.event;


import com.crystalneko.toneko.ToNeko;
import com.crystalneko.toneko.api.NekoQuery;
import com.crystalneko.toneko.utils.ConfigFileUtils;
import com.crystalneko.tonekocommon.Stats;
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
import org.cneko.ctlib.common.util.ChatPrefix;
import org.cneko.toneko.common.util.scheduled.SchedulerPoolProvider;

import java.io.File;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

public class PlayerEventListenerBase implements Listener {
    private final File dataFile = new File( "plugins/toNeko/nekos.yml");
    private final YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final String playerName = player.getName();
        final NekoQuery query = new NekoQuery(playerName,this.data);

        if(query.hasOwner()) {
            //添加前缀
            ChatPrefix.addPrivatePrefix(player.getName(), ToNeko.getMessage("other.neko"));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        final Player player = event.getPlayer();
        final String playerName = player.getName();
        final NekoQuery query = new NekoQuery(playerName,this.data);

        if(query.hasOwner()) {
            //删除前缀
            ChatPrefix.removePrivatePrefix(player.getName(), ToNeko.getMessage("other.neko"));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 处理玩家死亡事件的逻辑
        Player player = event.getEntity();
        if (player.getKiller() instanceof Player) {
            // 获取击杀者
            Player killer = player.getKiller();
            // 获取击杀者使用的物品
            ItemStack itemStack = killer.getInventory().getItemInMainHand();
            // 获取物品的ItemMeta对象
            ItemMeta itemMeta = itemStack.getItemMeta();
            //定义NBT标签
            NamespacedKey key = new NamespacedKey(ToNeko.pluginInstance, "neko");
            // 检查物品是否含有该自定义NBT标签
            if (itemMeta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                // 读取NBT标签的值
                String nbtValue = itemMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                //判断NBT是否正确
                if (Objects.equals(nbtValue, "true")){
                    final String playerName = player.getName();
                    final String killerName = killer.getName();

                    // 加载数据文件
                    //判断玩家是否为猫娘
                    if(this.data.getString(playerName + ".owner") != null) {
                        //发送死亡提示
                        String deathMessage = "猫娘 " + playerName + " 被 " + killerName + " §f撅死了！";
                        event.setDeathMessage(deathMessage);
                        //判断是否为主人
                        if(killerName.equals(this.data.getString(playerName + ".owner"))){
                            //生成随机数
                            Random random = new Random();
                            int randomNumber = random.nextInt(7) + 3;
                            player.sendMessage(ToNeko.getMessage("death.sub-xp",new String[]{killerName, String.valueOf(randomNumber)}));
                            killer.sendMessage(ToNeko.getMessage("death.sub-xp",new String[]{playerName, String.valueOf(randomNumber)}));
                            SchedulerPoolProvider.getINSTANCE().executeAsync(() -> {
                                //检查配置是否存在
                                ConfigFileUtils.createNewKey(playerName + "." + "xp", 0, this.data, this.dataFile);
                                //减去值
                                int xpValue = this.data.getInt(playerName + ".xp") - randomNumber;
                                ConfigFileUtils.setValue(playerName + ".xp",xpValue,this.dataFile);
                            });
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        try {
            // 检查是否是玩家被攻击
            if (event.getEntity() instanceof Player player) {
                if (event.getDamager() instanceof Player killer) {
                    // 获取击杀者
                    // 检查击杀者手中的物品是否为空或非武器
                    ItemStack itemStack = killer.getInventory().getItemInMainHand();
                    if (!itemStack.getType().isItem()) {
                        return;
                    }
                    // 获取物品的ItemMeta对象
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    //定义NBT标签
                    NamespacedKey key = new NamespacedKey(ToNeko.pluginInstance, "neko");
                    NamespacedKey key2 = new NamespacedKey(ToNeko.pluginInstance, "nekolevel");
                    Consumer<YamlConfiguration> callback = null;
                    // 检查物品是否含有该自定义NBT标签
                    if (itemMeta != null && itemMeta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                        // 读取NBT标签的值
                        String nbtValue = itemMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                        //判断NBT是否正确
                        if (Objects.requireNonNull(nbtValue).equalsIgnoreCase("true")) {
                            if (this.data.getString(player.getName() + ".owner") != null) {
                                PotionEffectType effectType = PotionEffectType.WEAKNESS; // 虚弱效果的类型
                                int duration = 200; // 持续时间
                                int amplifier = 0; // 效果强度
                                givePlayerPotionEffect(player, effectType, duration, amplifier);
                                //判断是否为主人
                                if (killer.getName().equals(this.data.getString(player.getName() + ".owner"))) {
                                    //生成随机数
                                    Random random = new Random();
                                    int randomNumber = random.nextInt(6) - 2;

                                    player.sendMessage(ToNeko.getMessage("attack.add-xp", new String[]{killer.getName(), String.valueOf(randomNumber)}));
                                    killer.sendMessage(ToNeko.getMessage("attack.add-xp", new String[]{player.getName(), String.valueOf(randomNumber)}));

                                    SchedulerPoolProvider.getINSTANCE().executeAsync(() -> {
                                        //检查配置是否存在
                                        ConfigFileUtils.createNewKey(player.getName() + "." + "xp", 0, this.data, this.dataFile);
                                        //加上值
                                        int xpValue = this.data.getInt(player.getName() + ".xp") + randomNumber;
                                        ConfigFileUtils.setValue(player.getName() + ".xp", xpValue, this.dataFile);
                                    });
                                }

                                //发送撅人音效
                                player.getWorld().playSound(player.getLocation(), "toneko.neko.stick", 1, 1);
                                killer.getWorld().playSound(player.getLocation(), "toneko.neko.stick", 1, 1);
                                // 添加统计
                                addStatistic(player.getName(), killer.getName());
                            }
                        }
                    } else if (itemMeta != null && itemMeta.getPersistentDataContainer().has(key2, PersistentDataType.INTEGER)) {
                        int nbtValue2 = itemMeta.getPersistentDataContainer().get(key2, PersistentDataType.INTEGER);
                        if (nbtValue2 == 2) {
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
                            if (killer.getName().equals(this.data.getString(player.getName() + ".owner"))) {
                                //生成随机数
                                Random random = new Random();
                                int randomNumber = random.nextInt(14) + 2;

                                player.sendMessage(ToNeko.getMessage("attack.add-xp", new String[]{killer.getName(), String.valueOf(randomNumber)}));
                                killer.sendMessage(ToNeko.getMessage("attack.add-xp", new String[]{player.getName(), String.valueOf(randomNumber)}));
                                // 添加统计
                                addStatistic(player.getName(), killer.getName());

                                SchedulerPoolProvider.getINSTANCE().executeAsync(() -> {
                                    //检查配置是否存在
                                    ConfigFileUtils.createNewKey(player.getName() + "." + "xp", 0, this.data, this.dataFile);
                                    //加上值
                                    int xpValue = this.data.getInt(player.getName() + ".xp") + randomNumber + 10;
                                    ConfigFileUtils.setValue(player.getName() + ".xp", xpValue, this.dataFile);
                                });
                            }
                            //发送撅人音效
                            player.getWorld().playSound(player.getLocation(), "toneko.neko.stick.level2", 1, 1);
                            killer.getWorld().playSound(player.getLocation(), "toneko.neko.stick.level2", 1, 1);
                        }
                    }
                }
            }
        }catch(Exception ignored){

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
        Stats.stick(player,neko);
    }
}
