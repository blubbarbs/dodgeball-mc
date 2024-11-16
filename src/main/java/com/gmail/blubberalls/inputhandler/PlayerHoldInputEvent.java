package com.gmail.blubberalls.inputhandler;

import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInputEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerHoldInputEvent extends PlayerInputEvent {
    private static final HandlerList handlers = new HandlerList();

    public PlayerHoldInputEvent(@NotNull Player player, @NotNull Input input) {
        super(player, input);
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
