package com.gmail.blubberalls.tf2;

import com.gmail.blubberalls.simpledata.SD;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class TF2 extends JavaPlugin implements Listener {

    public static double PLAYER_MAX_RANGE = 10;
    public static TF2 INSTANCE;

    @Override
    public void onEnable() {
        INSTANCE = this;
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new Dodgeball(), this);
        Bukkit.getPluginManager().registerEvents(new Bounceable(), this);
        Bukkit.getPluginManager().registerEvents(new DoubleJump(), this);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, Dodgeball::tickAll, 0L, 0L);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, DoubleJump::tickAll, 0L, 0L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
//        Bukkit.getLogger().info("Please");
//
//        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK)
//            return;
//
//        Projectile projectile = event.getPlayer().launchProjectile(Snowball.class);
//        Bounceable.setBounceable(projectile, .2);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        SD.set(event.getPlayer(), DoubleJump.Keys.NUM_JUMPS, 1);
    }

    public void spawnWindCharge(Player player) {
        Location targetLocation = rayCastPlayer(player).toLocation(player.getWorld());
        WindCharge windCharge = Dodgeball.spawn(targetLocation);
        Dodgeball.setTargetEntity(windCharge, player);
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
}
