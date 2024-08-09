package org.cneko.toneko.neoforge.fabric.impl;

import org.cneko.ctlib.common.file.YamlConfiguration;
import org.cneko.toneko.common.util.ConfigUtil.Config;
import org.cneko.toneko.common.util.FileUtil;

import java.io.IOException;
import java.nio.file.Path;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.util.ConfigUtil.CONFIG;
import static org.cneko.toneko.common.util.ConfigUtil.CONFIG_FILE;

public class FabricConfigImpl implements Config{
    @Override
    public void load(){
        if(!FileUtil.FileExists(CONFIG_FILE)){
            FileUtil.CreateFile(CONFIG_FILE);
            FileUtil.copyResource("assets/toneko/config.yml",CONFIG_FILE);
        }
        try {
            CONFIG = YamlConfiguration.fromFile(Path.of(CONFIG_FILE));
        } catch (IOException e) {
            LOGGER.error("Cannot load config: ",e);
        }
    }
}
