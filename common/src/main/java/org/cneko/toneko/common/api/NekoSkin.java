package org.cneko.toneko.common.api;

import org.cneko.ctlib.common.file.YamlConfiguration;
import org.cneko.toneko.common.Bootstrap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.cneko.toneko.common.Bootstrap.LOGGER;

public class NekoSkin {
    public String skin;
    public SkinType type = SkinType.NEKO;
    public String url;
    public String texture;
    /**
     * 猫娘皮肤
     * @param skin 皮肤在nameMC的名称
     */
    public NekoSkin(String skin){
        this.skin = skin;
    }

    /**
     * 获取猫娘皮肤
     * @return 皮肤在nameMC的名称
     */
    public String getSkin(){
        return skin;
    }

    /**
     * 设置猫娘皮肤
     * @param skin 皮肤在nameMC的名称
     */
    public void setSkin(String skin){
        this.skin = skin;
    }

    /**
     * 设置皮肤类型
     */
    public void setType(SkinType type){
        this.type = type;
    }

    /**
     * 获取皮肤类型
     * @return 皮肤类型
     */
    public SkinType getType(){
        return type;
    }

    public String getUrl(){
        return url;
    }
    public void setUrl(String url){
        this.url = url;
    }
    public String getTexture(){
        return texture;
    }
    public void setTexture(String texture){
        this.texture = texture;
    }

    @Override
    public String toString(){
        return skin;
    }

    public static NekoSkin of(String skin){
        return new NekoSkin(skin);
    }

    public static List<String> getSkins() {
        try {
            YamlConfiguration skins = YamlConfiguration.fromFile(Path.of(Bootstrap.SKIN_FILE));
            return skins.getStringList("neko");
        } catch (IOException e) {
            LOGGER.error("Unable to read skinList", e);
        }
        return new ArrayList<>();
    }

    public static void addSkin(String skin){
        try {
            YamlConfiguration skins = YamlConfiguration.fromFile(Path.of(Bootstrap.SKIN_FILE));
            List<String> neko = skins.getStringList("neko");
            neko.add(skin);
            skins.set("neko",neko);
            skins.save();
        } catch (IOException e) {
            LOGGER.error("Unable to read skinList", e);
        }
    }

    public static void removeSkin(String skin){
        try {
            YamlConfiguration skins = YamlConfiguration.fromFile(Path.of(Bootstrap.SKIN_FILE));
            List<String> neko = skins.getStringList("neko");
            neko.remove(skin);
            skins.set("neko",neko);
            skins.save();
        } catch (IOException e) {
            LOGGER.error("Unable to read skinList", e);
        }
    }

    public static NekoSkin getRandomSkin(){
        return NekoSkin.of(getSkins().get((int)(Math.random()*getSkins().size())));
    }

    public static enum SkinType{
        NEKO
    }

}
