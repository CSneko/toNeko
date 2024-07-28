package org.cneko.toneko.fabric.impl;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.cneko.ctlib.common.file.YamlConfiguration;
import org.cneko.toneko.fabric.ModMeta;
import org.cneko.toneko.common.util.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

import static org.cneko.toneko.common.Bootstrap.LOGGER;
import static org.cneko.toneko.common.Bootstrap.MODID;
import static org.cneko.toneko.common.util.ConfigUtil.*;

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
