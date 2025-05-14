package org.cneko.gal.common.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;

public class TextureUtil {
    public static ResourceLocation registerTexture(String name, InputStream stream) throws IOException {
        return Minecraft.getInstance().getTextureManager().register(name, new DynamicTexture(NativeImage.read(stream)));
    }
}
