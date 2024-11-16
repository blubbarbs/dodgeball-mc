package com.gmail.blubberalls.inputhandler;

import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;

public class InputListener implements Listener {
    private class CustomInput implements Input {
        public boolean forward = false;
        public boolean backward = false;
        public boolean left = false;
        public boolean right = false;
        public boolean jump = false;
        public boolean sneak = false;
        public boolean sprint = false;

        @Override
        public boolean isForward() {
            return forward;
        }

        @Override
        public boolean isBackward() {
            return backward;
        }

        @Override
        public boolean isLeft() {
            return left;
        }

        @Override
        public boolean isRight() {
            return right;
        }

        @Override
        public boolean isJump() {
            return jump;
        }

        @Override
        public boolean isSneak() {
            return sneak;
        }

        @Override
        public boolean isSprint() {
            return sprint;
        }

        public boolean isValid() {
            return forward || backward || left || right || jump || sneak || sprint;
        }
    }

    public PlayerPressInputEvent createPressEvent(Player player, Input previousInput, Input newInput) {
        CustomInput changedInput = new CustomInput();

        changedInput.forward = !previousInput.isForward() && newInput.isForward();
        changedInput.backward = !previousInput.isBackward() && newInput.isBackward();
        changedInput.left = !previousInput.isLeft() && newInput.isLeft();
        changedInput.right = !previousInput.isRight() && newInput.isRight();
        changedInput.jump = !previousInput.isJump() && newInput.isJump();
        changedInput.sneak = !previousInput.isSneak() && newInput.isSneak();
        changedInput.sprint = !previousInput.isSneak() && newInput.isSprint();

        if (changedInput.isValid()) {
            return new PlayerPressInputEvent(player, newInput, changedInput);
        }
        else
            return null;
    }

    public PlayerReleaseInputEvent createReleaseEvent(Player player, Input previousInput, Input newInput) {
        CustomInput changedInput = new CustomInput();

        changedInput.forward = previousInput.isForward() && !newInput.isForward();
        changedInput.backward = previousInput.isBackward() && !newInput.isBackward();
        changedInput.left = previousInput.isLeft() && !newInput.isLeft();
        changedInput.right = previousInput.isRight() && !newInput.isRight();
        changedInput.jump = previousInput.isJump() && !newInput.isJump();
        changedInput.sneak = previousInput.isSneak() && !newInput.isSneak();
        changedInput.sprint = previousInput.isSneak() && !newInput.isSprint();

        if (changedInput.isValid()) {
            return new PlayerReleaseInputEvent(player, newInput, changedInput);
        }
        else
            return null;
    }

    @EventHandler
    public void onInput(PlayerInputEvent event) {
        Input previousInput = event.getPlayer().getCurrentInput();

        if (previousInput == null) {
            PlayerPressInputEvent pressEvent = new PlayerPressInputEvent(event.getPlayer(), event.getInput(), event.getInput());
            Bukkit.getPluginManager().callEvent(pressEvent);
            return;
        }

        PlayerPressInputEvent pressEvent = createPressEvent(event.getPlayer(), previousInput, event.getInput());
        if (pressEvent != null)
            Bukkit.getPluginManager().callEvent(pressEvent);

        PlayerReleaseInputEvent releaseEvent = createReleaseEvent(event.getPlayer(), previousInput, event.getInput());
        if (releaseEvent != null)
            Bukkit.getPluginManager().callEvent(releaseEvent);
    }
}
