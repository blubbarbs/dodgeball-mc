package com.gmail.blubberalls.tf2;

import com.gmail.blubberalls.simpledata.SD;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class Bounceable implements Listener {
    public static class Keys {
        public static SD.Key<BounceableData> BOUNCEABLE = new SD.Key<>("tf2", "bounceable", BounceableData.TYPE);
    }

    public static class BounceableData implements SD.Serializable {
        public static class Keys {
            public static SD.Key<Double> ENERGY_LOSS = new SD.Key<Double>("energy_loss", PersistentDataType.DOUBLE);
        }
        public static PersistentDataType<PersistentDataContainer, BounceableData> TYPE = SD.Serializable.createDataType(BounceableData.class, BounceableData::new);

        public final double energyLoss;

        public BounceableData(double energyLoss) {
            this.energyLoss = energyLoss;
        }

        public BounceableData(PersistentDataContainer pdc) {
            energyLoss = SD.get(pdc, Keys.ENERGY_LOSS);
        }

        @Override
        public void serialize(PersistentDataContainer pdc) {
            SD.set(pdc, Keys.ENERGY_LOSS, energyLoss);
        }
    }

    public static boolean isBounceable(Projectile projectile) {
        return SD.has(projectile, Keys.BOUNCEABLE);
    }

    public static void setBounceable(Projectile projectile, double energyLoss) {
        BounceableData bounceableData = new BounceableData(energyLoss);
        SD.set(projectile, Keys.BOUNCEABLE, bounceableData);
    }

    public static BounceableData getBounceableData(Projectile projectile) {
        if (!isBounceable(projectile))
            return null;

        return SD.get(projectile, Keys.BOUNCEABLE);
    }

    public static void doEntityHit(Projectile projectile, BounceableData data, Entity hitEntity) {
        Vector reflected = projectile.getVelocity().multiply(-(1 - data.energyLoss));

        projectile.setVelocity(projectile.getVelocity().multiply(-(1 - data.energyLoss)));
        projectile.teleport(projectile.getLocation().setDirection(reflected));
    }

    public static void doBlockHit(Projectile projectile, BounceableData data, Block block, BlockFace hitFace) {
        Vector normal = getNormal(hitFace);

        if (normal == null) {
            Bukkit.getLogger().info("HIT BLOCK FACE: " + hitFace);
            return;
        }

        Vector reflected = reflectVector(projectile.getVelocity(), normal).multiply(1 - data.energyLoss);

        projectile.setVelocity(reflected);
        projectile.teleport(projectile.getLocation().setDirection(reflected));
    }

    private static Vector getNormal(BlockFace face) {
        return switch (face) {
            case UP -> new Vector(0, 1, 0);
            case DOWN -> new Vector(0, -1, 0);
            case EAST -> new Vector(1, 0, 0);
            case WEST -> new Vector(-1, 0, 0);
            case NORTH -> new Vector(0, 0, -1);
            case SOUTH -> new Vector(0, 0, 1);
            default -> null;
        };
    }

    private static Vector reflectVector(Vector a, Vector normal) {
        return a.subtract(normal.multiply(a.dot(normal) * 2));
    }

    private static Vector rotateVectors(Vector a, Vector b, double percentage) {
        if (percentage == 1)
            return b.clone();

        Vector axis = a.getCrossProduct(b).normalize();
        double angle = a.angle(b) * percentage;

        return a.clone().rotateAroundAxis(axis, angle);
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
}
