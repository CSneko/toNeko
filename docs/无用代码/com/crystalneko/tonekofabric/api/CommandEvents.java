package com.crystalneko.tonekofabric.api;


import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class CommandEvents {

    public static Event<Toneko_Player> TONEKO_PLAYER = EventFactory.createArrayBacked(Toneko_Player.class,
            (listeners) -> (context) -> {
                for (Toneko_Player listener : listeners) {
                    int result = listener.toneko_player(context);
                    if (result != 1) {
                        return result;
                    }
                }
                return 1;
            });
    public static Event<Toneko_Remove> TONEKO_REMOVE = EventFactory.createArrayBacked(Toneko_Remove.class,
            (listeners) -> (context) -> {
                for (Toneko_Remove listener : listeners) {
                    int result = listener.toneko_remove(context);
                    if (result != 1) {
                        return result;
                    }
                }
                return 1;
            });

    public interface Toneko_Player{
        int toneko_player(CommandContext<ServerCommandSource> context);
    }
    public interface Toneko_Remove{
        int toneko_remove(CommandContext<ServerCommandSource> context);
    }
}
