package com.gmail.blubberalls.dodgeball;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class Dodgeball extends JavaPlugin implements Listener {
    public static double PLAYER_MAX_RANGE = 10;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::tickBallEntities, 0L, 0L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        Player p = event.getPlayer();
        Location targetLocation = rayCastPlayer(p).toLocation(p.getWorld());
        WindCharge windCharge = Ball.spawn(targetLocation);

        Ball.setTargetEntity(windCharge, p);
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
