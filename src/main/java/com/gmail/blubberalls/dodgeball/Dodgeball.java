package com.gmail.blubberalls.dodgeball;

import com.gmail.blubberalls.inputhandler.InputListener;
import com.gmail.blubberalls.inputhandler.PlayerPressInputEvent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import javax.lang.model.element.VariableElement;

public final class Dodgeball extends JavaPlugin implements Listener {
    public static double PLAYER_MAX_RANGE = 10;
    public static Dodgeball INSTANCE;
    public static InputListener INPUT_HANDLER = new InputListener();

    public InputListener getInputHandler() {
        return INPUT_HANDLER;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(INPUT_HANDLER, this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::tickBallEntities, 0L, 0L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Bukkit.getLogger().info("Please");

        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        Projectile projectile = event.getPlayer().launchProjectile(Snowball.class);
        Bounceable.setBounceable(projectile, .2);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Bounceable.BounceableData data = Bounceable.getBounceableData(event.getEntity());
        if (data == null)
            return;

        EntitySnapshot snapshot = event.getEntity().createSnapshot();
        if (snapshot == null)
            return;

        event.setCancelled(true);
        Projectile projectile = (Projectile) snapshot.createEntity(event.getEntity().getLocation());
        event.getEntity().remove();

        if (event.getHitBlock() != null) {
            Bounceable.doBlockHit(projectile, data, event.getHitBlock(), event.getHitBlockFace());
        }
        else {
            Bounceable.doEntityHit(projectile, data, event.getHitEntity());
        }

//        if (Math.abs(projectile.getVelocity().getY()) <= .2) {
//            Vector newVelocity = new Vector(projectile.getVelocity().getX(), 0, projectile.getVelocity().getZ());
//
//            projectile.setVelocity(newVelocity);
//            projectile.setGravity(false);
//        }
    }


    public void doubleJump(Player player, Input currentInput) {
        Vector playerDirection = player.getEyeLocation().getDirection().normalize();
        playerDirection.setY(0);
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
                angle = -1;
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

        if (angle >= 0) {
            player.setVelocity(playerDirection.rotateAroundY(Math.toRadians(angle)).multiply(.5).setY(.5));
        }
        else {
            player.setVelocity(player.getVelocity().setY(.5));
        }
    }

    @EventHandler
    public void onPlayerPress(PlayerPressInputEvent event) {
        if (event.getChangedInput().isJump() && !event.getPlayer().isOnGround() && event.getPlayer().getVelocity().getY() < 0) {
            doubleJump(event.getPlayer(), event.getInput());
        }
    }

    public void spawnWindCharge(Player player) {
        Location targetLocation = rayCastPlayer(player).toLocation(player.getWorld());
        WindCharge windCharge = Ball.spawn(targetLocation);
        Ball.setTargetEntity(windCharge, player);
    }

    public Vector rayCastPlayer(Player player) {
        Vector targetLocation;
        RayTraceResult rayTrace = player.rayTraceBlocks(PLAYER_MAX_RANGE);
        if (rayTrace == null)
            targetLocation = player.getEyeLocation().toVector().add(player.getEyeLocation().getDirection().normalize().multiply(PLAYER_MAX_RANGE));
        else
            targetLocation = rayTrace.getHitPosition();

        return targetLocation;
    }

    public void tickBallEntities() {
        for (World world : Bukkit.getWorlds()) {
            world.getEntitiesByClass(WindCharge.class).forEach(Ball::tick);
        }
    }
}
