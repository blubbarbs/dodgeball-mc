package com.gmail.blubberalls.tf2;

import com.gmail.blubberalls.simpledata.SD;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class DoubleJump implements Listener {
    public static class Keys {
        public static SD.Key<Long> IN_AIR_TICKS = new SD.Key<>("tf2", "in_air_ticks", PersistentDataType.LONG);
        public static SD.Key<Integer> JUMPS_LEFT = new SD.Key<>("tf2", "jumps_left", PersistentDataType.INTEGER);
        public static SD.Key<Integer> NUM_JUMPS = new SD.Key<>("tf2", "num_jumps", PersistentDataType.INTEGER);
    }

    public static long getTicksInAir(Player player) {
        return SD.getOrDefault(player, Keys.IN_AIR_TICKS, 0L);
    }

    public static int getJumpsLeft(Player player) {
        return SD.getOrDefault(player, Keys.JUMPS_LEFT, 0);
    }

    public static int getNumJumps(Player player) {
        return SD.getOrDefault(player, Keys.NUM_JUMPS, 0);
    }

    public static Vector getDoubleJumpVector(Player player, Input currentInput) {
        Vector doubleJump = player.getEyeLocation().getDirection().normalize();
        doubleJump.setY(0);
        int x = 0;
        int z = 0;

        x -= currentInput.isRight() ? 1 : 0;
        x += currentInput.isLeft() ? 1 : 0;
        z += currentInput.isForward() ? 1 : 0;
        z -= currentInput.isBackward() ? 1 : 0;
        double angle = 0;

        if (x == -1) {
            if (z == -1) {
                angle = 225;
            }
            else if (z == 0) {
                angle = 270;
            }
            else if (z == 1) {
                angle = 315;
            }
        }
        else if (x == 0) {
            if (z == -1) {
                angle = 180;
            }
            else if (z == 0) {
                return player.getVelocity().setY(.42);
            }
            else if (z == 1) {
                angle = 0;
            }
        }
        else if (x == 1) {
            if (z == -1) {
                angle = 135;
            }
            else if (z == 0) {
                angle = 90;
            }
            else if (z == 1) {
                angle = 45;
            }
        }

        return doubleJump.rotateAroundY(Math.toRadians(angle)).multiply(.42).setY(.42);
    }

    public static void tick(Player player) {
        if (!player.isOnGround()) {
            long inAirTicks = getTicksInAir(player);
            SD.set(player, Keys.IN_AIR_TICKS, inAirTicks + 1);
            return;
        }

        int jumpsLeft = getJumpsLeft(player);
        int numJumps = getNumJumps(player);
        if (jumpsLeft < numJumps) {
            SD.set(player, Keys.JUMPS_LEFT, numJumps);
        }
        SD.set(player, Keys.IN_AIR_TICKS, 0L);
    }

    public static void tickAll() {
        Bukkit.getOnlinePlayers().forEach(DoubleJump::tick);
    }

    @EventHandler
    public void onPlayerInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        Input previousInput = player.getCurrentInput();
        boolean isJumpPress = !previousInput.isJump() && event.getInput().isJump();

        if (!isJumpPress
                || player.isOnGround()
                || player.isFlying()
                || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR)
            return;

        long inAirTicks = getTicksInAir(player);

        if (inAirTicks <= 1)
            return;

        int jumpsLeft = getJumpsLeft(player);

        if (jumpsLeft > 0) {
            Vector doubleJump = getDoubleJumpVector(event.getPlayer(), event.getInput());
            player.setVelocity(doubleJump);
            SD.set(player, Keys.JUMPS_LEFT, jumpsLeft - 1);
        }
    }
}
