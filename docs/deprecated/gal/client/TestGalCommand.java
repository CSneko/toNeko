package org.cneko.gal.common.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.cneko.gal.common.util.pack.DynamicSoundManager;
import org.cneko.gal.common.util.pack.ExternalPack;

import java.nio.file.Path;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class TestGalCommand {
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext) -> {
            dispatcher.register(literal("testgal")
                    .then(literal("add")
                            .then(argument("loc", StringArgumentType.string())
                                    .then(argument("file", StringArgumentType.string())
                                            .executes(context->{
                                                ExternalPack.addResource(ResourceLocation.parse(StringArgumentType.getString(context, "loc")), Path.of(StringArgumentType.getString(context, "file")));
                                                return 1;
                                            })
                                    )
                            )
                    )
                    .then(literal("refresh")
                            .executes(context->{
                                ExternalPack.applyPendingTextureRefreshes();
                                ExternalPack.applyPendingSoundRefreshes(Minecraft.getInstance().getSoundManager());
                                return 1;
                            })
                    )
                    .then(literal("regSound")
                            .then(argument("namespace", StringArgumentType.string())
                                    .then(argument("event", StringArgumentType.string())
                                            .then(argument("path", StringArgumentType.string())
                                                    .executes(context->{
                                                        var config = new DynamicSoundManager.SoundEventConfig("record", List.of(
                                                                new DynamicSoundManager.SoundVariant(StringArgumentType.getString(context, "path"))
                                                        ));
                                                        DynamicSoundManager.registerSoundEvent(
                                                                StringArgumentType.getString(context, "namespace"),
                                                                StringArgumentType.getString(context, "event"),
                                                                config
                                                        );
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                    )
                    .then(literal("playSound")
                            .then(argument("namespace", StringArgumentType.string())
                                    .then(argument("event", StringArgumentType.string())
                                            .executes(context->{
                                                DynamicSoundManager.playSoundEvent(
                                                        ResourceLocation.fromNamespaceAndPath(
                                                                StringArgumentType.getString(context, "namespace"),
                                                                StringArgumentType.getString(context, "event")
                                                        ),
                                                        1.0f
                                                );
                                                return 1;
                                            })
                                    )
                            )
                    )
            );
        });
    }
}
