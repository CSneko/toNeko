package org.cneko.gal.common.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import org.cneko.gal.common.client.parser.GalParser;
import org.cneko.gal.common.client.screen.DialogueScreen;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.util.TickTaskQueue;

import java.io.File;
import java.nio.file.Path;

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

                    .then(literal("playDialogue")
                            .then(argument("path", StringArgumentType.string())
                                    .executes(ctx -> {
                                        var task = new TickTaskQueue();
                                        task.addTask(1,  ()->{
                                            Minecraft.getInstance().setScreen(new DialogueScreen(
                                                    new GalParser(Path.of(StringArgumentType.getString(ctx, "path")))
                                            ));
                                        });
                                        TickTasks.addClient(task);
                                        return 1;
                                    })
                            )
                    )
            );
        });
    }
}
