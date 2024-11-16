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
        CustomInput eventInput = new CustomInput();

        eventInput.forward = !previousInput.isForward() && newInput.isForward();
        eventInput.backward = !previousInput.isBackward() && newInput.isBackward();
        eventInput.left = !previousInput.isLeft() && newInput.isLeft();
        eventInput.right = !previousInput.isRight() && newInput.isRight();
        eventInput.jump = !previousInput.isJump() && newInput.isJump();
        eventInput.sneak = !previousInput.isSneak() && newInput.isSneak();
        eventInput.sprint = !previousInput.isSneak() && newInput.isSprint();

        if (eventInput.isValid()) {
            return new PlayerPressInputEvent(player, eventInput);
        }
        else
            return null;
    }

    public PlayerHoldInputEvent createHoldEvent(Player player, Input previousInput, Input newInput) {
        CustomInput eventInput = new CustomInput();

        eventInput.forward = previousInput.isForward() && newInput.isForward();
        eventInput.backward = previousInput.isBackward() && newInput.isBackward();
        eventInput.left = previousInput.isLeft() && newInput.isLeft();
        eventInput.right = previousInput.isRight() && newInput.isRight();
        eventInput.jump = previousInput.isJump() && newInput.isJump();
        eventInput.sneak = previousInput.isSneak() && newInput.isSneak();
        eventInput.sprint = previousInput.isSneak() && newInput.isSprint();

        if (eventInput.isValid()) {
            return new PlayerHoldInputEvent(player, eventInput);
        }
        else
            return null;
    }

    public PlayerReleaseInputEvent createReleaseEvent(Player player, Input previousInput, Input newInput) {
        CustomInput eventInput = new CustomInput();

        eventInput.forward = previousInput.isForward() && !newInput.isForward();
        eventInput.backward = previousInput.isBackward() && !newInput.isBackward();
        eventInput.left = previousInput.isLeft() && !newInput.isLeft();
        eventInput.right = previousInput.isRight() && !newInput.isRight();
        eventInput.jump = previousInput.isJump() && !newInput.isJump();
        eventInput.sneak = previousInput.isSneak() && !newInput.isSneak();
        eventInput.sprint = previousInput.isSneak() && !newInput.isSprint();

        if (eventInput.isValid()) {
            return new PlayerReleaseInputEvent(player, eventInput);
        }
        else
            return null;
    }

    @EventHandler
    public void onInput(PlayerInputEvent event) {
        Input previousInput = event.getPlayer().getCurrentInput();

        if (previousInput == null) {
            PlayerPressInputEvent pressEvent = new PlayerPressInputEvent(event.getPlayer(), event.getInput());
            Bukkit.getPluginManager().callEvent(pressEvent);
            return;
        }

        PlayerPressInputEvent pressEvent = createPressEvent(event.getPlayer(), previousInput, event.getInput());
        if (pressEvent != null)
            Bukkit.getPluginManager().callEvent(pressEvent);

        PlayerHoldInputEvent holdEvent = createHoldEvent(event.getPlayer(), previousInput, event.getInput());
        if (holdEvent != null)
            Bukkit.getPluginManager().callEvent(holdEvent);

        PlayerReleaseInputEvent releaseEvent = createReleaseEvent(event.getPlayer(), previousInput, event.getInput());
        if (releaseEvent != null)
            Bukkit.getPluginManager().callEvent(releaseEvent);
    }
}
