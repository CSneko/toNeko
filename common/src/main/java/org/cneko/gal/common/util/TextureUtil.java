package org.cneko.gal.common.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;

public class TextureUtil {
    public static Identifier registerTexture(String name, InputStream stream) throws IOException {
        // 26.x：TextureManager.register 返回 void，标识符由调用方自行持有
        Identifier id = Identifier.parse(name);
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture((java.util.function.Supplier<String>) () -> name, NativeImage.read(stream)));
        return id;
    }
}
