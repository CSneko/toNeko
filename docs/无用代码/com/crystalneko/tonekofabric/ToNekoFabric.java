package com.crystalneko.tonekofabric;

import com.crystalneko.tonekofabric.api.NekoEntityEvents;
import com.crystalneko.tonekofabric.command.command;
import com.crystalneko.tonekofabric.entity.EntityRegister;
import com.crystalneko.tonekofabric.entity.nekoEntity;
import com.crystalneko.tonekofabric.event.PlayerAttack;
import com.crystalneko.tonekofabric.event.PlayerChat;
import com.crystalneko.tonekofabric.event.PlayerJoin;
import com.crystalneko.tonekofabric.event.PlayerLeave;
import com.crystalneko.tonekofabric.event.PlayerDamage;
import com.crystalneko.tonekofabric.items.stick;
import com.crystalneko.tonekofabric.libs.base;
import com.crystalneko.tonekofabric.libs.lp;
import org.cneko.ctlib.common.file.Resources;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import org.cneko.ctlib.common.file.YamlConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ToNekoFabric implements ModInitializer {
    public static Boolean started = false;
    private command command;
    public static YamlConfiguration config;
    public static Logger logger = Logger.getLogger("ToNeko");
    public static EntityType<nekoEntity> NEKO;
    public static Item NEKO_SPAWN_EGG;
    public static boolean isNewVersion = false;
    /**
     * 运行模组 initializer.
     */
    @Override
    public void onInitialize() {
        new base();
        // 加载配置
    //    try {
            //ConfigUtils.updateConfig();
      //  } catch (IOException e) {
        //    System.out.println("加载配置文件失败:"+e.getMessage());
       // }
        Path configPath = Path.of("ctlib/toneko/config.yml");
        if(Files.exists(configPath)){
            try {
                config = new YamlConfiguration(configPath);
            } catch (IOException e) {
                logger.log(Level.CONFIG, "Failed to load config file:"+e.getMessage());
            }
        }else {
            try {
                Resources resource = new Resources(ToNekoFabric.class);
                String conf = resource.readFileFromJar("assets/toneko/config.yml");
                config = new YamlConfiguration(conf);
            } catch (Exception e){
                logger.log(Level.CONFIG, "Failed to load config file from jar:"+e.getMessage());
            }
        }
        //注册命令
        this.command = new command();

        //注册监听事件
        event();

        //注册物品
        new stick();

        //判断是否是新版本
        try{
            Class.forName("com.crystalneko.tonekofabric.entity.nekoEntity");
            isNewVersion = true;
            EntityRegister.register();
            NekoEntityEvents.register();
        }catch (ClassNotFoundException ignored){}
        //注册权限组
        new lp();

    }
    //监听事件
    private void event(){
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            new PlayerAttack();
            new PlayerJoin();
            new PlayerLeave();
            PlayerDamage.init();
            if (config.getBoolean("chat.enable")) {
                PlayerChat.init();
            }

            /*try {
                Class.forName("com.crystalneko.tonekofabric.test.event");
                System.out.println("成功启动测试版本");
                new com.crystalneko.tonekofabric.test.event();
            } catch (ClassNotFoundException ignored) {}*/
        });
    }
}
