package com.crystalneko.toneko.api.events;

import com.crystalneko.toneko.api.NekoQuery;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

public class ChatEvents {
    public static class OnChat extends Event implements Cancellable {
        private Player player;
        private String message;
        private AsyncPlayerChatEvent bukkitEvent;
        private io.papermc.paper.event.player.AsyncChatEvent event;
        private boolean isCancelled;
        private static final HandlerList handlers = new HandlerList();

        public OnChat(Player player, String message, AsyncPlayerChatEvent event){
            this.player = player;
            this.message = message;
            this.isCancelled = false;
            this.bukkitEvent = event;
        }

        public OnChat(Player player, String message, io.papermc.paper.event.player.AsyncChatEvent event){
            this.player = player;
            this.message = message;
            this.isCancelled = false;
            this.event = event;
        }

        public AsyncPlayerChatEvent getBukkitEvent() {
            return this.bukkitEvent;
        }
        private io.papermc.paper.event.player.AsyncChatEvent getEvent() {
            return this.event;
        }

        @Override
        public boolean isCancelled() {
            return this.isCancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.isCancelled = cancel;
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return handlers;
        }

        public Player getPlayer() {
            return this.player;
        }
        public String getMessage() {
            return this.message;
        }
        public NekoQuery getQuery() {
            return new NekoQuery(this.player.getName());
        }

    }
}
