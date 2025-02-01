package org.cneko.toneko.common.mod.ai;

import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;

import static org.cneko.toneko.common.mod.ai.PromptRegistry.register;
import static org.cneko.toneko.common.mod.ai.PromptRegistry.PromptFactory;
public class Prompts {
    public static final PromptFactory NEKO_NAME = (neko,other)-> neko.getName().getString();
    public static final PromptFactory NEKO_TYPE = (neko,other)-> neko.getTypeName().getString();
    public static final PromptFactory NEKO_DES = (neko,other)-> neko.getDescription();
    public static final PromptFactory NEKO_HEIGHT = (neko,other)-> new DecimalFormat("0.00").format(neko.getBbHeight());
    public static final PromptFactory NEKO_MOE_TAGS = (neko,other)-> neko.getMoeTagsString();
    public static final PromptFactory PLAYER_NAME = (neko,other)-> other.getEntity().getName().getString();
    public static final PromptFactory PLAYER_IS_OWNER = (neko,other)-> neko.getNeko().hasOwner(other.getEntity().getUUID())? Component.translatable("misc.toneko.is_or_not.is").getString():Component.translatable("misc.toneko.is_or_not.not").getString();
    public static final PromptFactory PLAYER_IS_NEKO = (neko,other)-> other.isNeko()?Component.translatable("misc.toneko.is_or_not.is").getString():Component.translatable("misc.toneko.is_or_not.not").getString();
    public static final PromptFactory WORLD_TIME = (neko,other)-> neko.level().isDay()? Component.translatable("misc.toneko.time.day").getString():Component.translatable("misc.toneko.time.night").getString();
    public static final PromptFactory WORLD_WEATHER = (neko,other)-> (neko.level().isRainingAt(neko.blockPosition())||neko.level().isThundering())? Component.translatable("misc.toneko.weather.rain").getString():Component.translatable("misc.toneko.weather.sunny").getString();
    public static void init() {
        register("neko_name",NEKO_NAME);
        register("neko_type",NEKO_TYPE);
        register("neko_des",NEKO_DES);
        register("neko_height",NEKO_HEIGHT);
        register("neko_moe_tags",NEKO_MOE_TAGS);
        register("player_name",PLAYER_NAME);
        register("player_is_owner",PLAYER_IS_OWNER);
        register("player_is_neko",PLAYER_IS_NEKO);
        register("world_time",WORLD_TIME);
        register("world_weather",WORLD_WEATHER);
    }
}
