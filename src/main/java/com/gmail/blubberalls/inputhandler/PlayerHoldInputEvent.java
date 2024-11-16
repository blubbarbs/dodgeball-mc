package com.gmail.blubberalls.inputhandler;

import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInputEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerHoldInputEvent extends PlayerInputEvent {
    private static final HandlerList handlers = new HandlerList();

    private Input changedInput;

    public PlayerHoldInputEvent(@NotNull Player player, @NotNull Input input, @NotNull Input changedInput) {
        super(player, input);
        this.changedInput = changedInput;
    }

    public Input getChangedInput() {
        return changedInput;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
