package org.cneko.gal.common.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import java.io.File;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class TestGalCommand {
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext) -> {
            dispatcher.register(literal("testgal")
                    .then(literal("playMusic")
                            .then(argument("path", StringArgumentType.string())
                                    .then(argument("loop", BoolArgumentType.bool())
                                            .executes(ctx -> {
                                                GalSoundInstance.getInstance().playMusic(StringArgumentType.getString(ctx, "path"), BoolArgumentType.getBool(ctx, "loop"));
                                                return 1;
                                            })
                                    )
                            )
                    )
                    .then(literal("playVoice")
                            .then(argument("path", StringArgumentType.string())
                                    .executes(ctx -> {
                                        GalSoundInstance.getInstance().playVoice(StringArgumentType.getString(ctx, "path"));
                                        return 1;
                                    })
                            )
                    )
                    .then(literal("stopMusic")
                            .executes(ctx -> {
                                GalSoundInstance.getInstance().stopMusic();
                                return 1;
                            })
                    )
                    .then(literal("stopVoice")
                            .executes(ctx -> {
                                GalSoundInstance.getInstance().stopVoice();
                                return 1;
                            })
                    )
            );
        });
    }
}
