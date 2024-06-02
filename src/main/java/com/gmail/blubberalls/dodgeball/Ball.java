package com.gmail.blubberalls.dodgeball;

import com.gmail.blubberalls.simpledata.SimpleData;
import com.jeff_media.morepersistentdatatypes.DataType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WindCharge;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.UUID;

public class Ball {
    public static final double MINECRAFT_TPS = 20;
    public static final double MINECRAFT_TICK_DURATION = 1.0 / MINECRAFT_TPS;
    public static final double SIMULATED_TPS = 66;
    public static final double SIMULATED_TICK_DURATION = 1.0 / SIMULATED_TPS;
    public static final double TURN_RATE_MODIFIER = .1 / SIMULATED_TICK_DURATION;

    public static class Defaults {
        public static final double MAX_SPEED = 12.5;
        public static final double TURN_FACTOR = .260;
    }

    public static class Keys {
        public static SimpleData.Key<Vector> DIRECTION_VEC = new SimpleData.Key<>("db", "__direction", DataType.VECTOR);
        public static SimpleData.Key<Double> TURN_FACTOR = new SimpleData.Key<>("db", "turn_factor", PersistentDataType.DOUBLE);
        public static SimpleData.Key<Double> MAX_SPEED = new SimpleData.Key<>("db", "max_speed", PersistentDataType.DOUBLE);
        public static SimpleData.Key<UUID> TARGET_ENTITY_UUID_DEBUG = new SimpleData.Key<>("db", "target_uuid_debug", DataType.UUID);
        public static SimpleData.Key<UUID> TARGET_ENTITY_UUID = new SimpleData.Key<>("db", "target_uuid", DataType.UUID);
        public static SimpleData.Key<Vector> TARGET_LOCATION = new SimpleData.Key<>("db", "target_location", DataType.VECTOR);

    }

    public static boolean isBall(Entity e) {
        return SimpleData.has(e, Keys.TURN_FACTOR);
    }

    public static WindCharge spawn(Location location) {
        WindCharge ballEntity = location.getWorld().spawn(location, WindCharge.class);
        ballEntity.setNoPhysics(true);
        ballEntity.setPower(new Vector(0, 0, 0));
        SimpleData.set(ballEntity, Keys.DIRECTION_VEC, new Vector(1, 0, 0));
        SimpleData.set(ballEntity, Keys.MAX_SPEED, Defaults.MAX_SPEED);
        SimpleData.set(ballEntity, Keys.TURN_FACTOR, Defaults.TURN_FACTOR);

        return ballEntity;
    }

    public static void setTargetEntity(WindCharge ballEntity, LivingEntity entity) {
        SimpleData.set(ballEntity, Keys.TARGET_ENTITY_UUID, entity.getUniqueId());
        SimpleData.remove(ballEntity, Keys.TARGET_ENTITY_UUID_DEBUG);
        SimpleData.remove(ballEntity, Keys.TARGET_LOCATION);
    }

    public static void setTargetEntityDebug(WindCharge ballEntity, LivingEntity entity) {
        SimpleData.remove(ballEntity, Keys.TARGET_ENTITY_UUID);
        SimpleData.set(ballEntity, Keys.TARGET_ENTITY_UUID_DEBUG, entity.getUniqueId());
        SimpleData.remove(ballEntity, Keys.TARGET_LOCATION);
    }

    public static void setTargetLocation(WindCharge ballEntity, Vector location) {
        SimpleData.remove(ballEntity, Keys.TARGET_ENTITY_UUID);
        SimpleData.remove(ballEntity, Keys.TARGET_ENTITY_UUID_DEBUG);
        SimpleData.set(ballEntity, Keys.TARGET_LOCATION, location);
    }

    public static void setTargetLocation(WindCharge ballEntity, Location location) {
        setTargetLocation(ballEntity, location.toVector());
    }

    public static void setTurnFactor(WindCharge ballEntity, double turnFactor) {
        SimpleData.set(ballEntity, Keys.TURN_FACTOR, turnFactor);
    }

    public static void setMaxSpeed(WindCharge ballEntity, double speed) {
        SimpleData.set(ballEntity, Keys.MAX_SPEED, speed);
    }

    public static Vector getDestination(WindCharge ballEntity) {
        if (SimpleData.has(ballEntity, Keys.TARGET_LOCATION)) {
            return SimpleData.get(ballEntity, Keys.TARGET_LOCATION);
        } else if (SimpleData.has(ballEntity, Keys.TARGET_ENTITY_UUID_DEBUG)) {
            Entity e = Bukkit.getEntity(SimpleData.get(ballEntity, Keys.TARGET_ENTITY_UUID_DEBUG));

            if (e == null || e.isDead() || !(e instanceof LivingEntity livingE))
                return null;

            Vector targetLocation;
            RayTraceResult rayTrace = livingE.rayTraceBlocks(15);
            if (rayTrace == null)
                targetLocation = livingE.getEyeLocation().toVector().add(livingE.getEyeLocation().getDirection().normalize().multiply(15));
            else
                targetLocation = rayTrace.getHitPosition();

            return targetLocation;
        }
        else if (SimpleData.has(ballEntity, Keys.TARGET_ENTITY_UUID)) {
            Entity e = Bukkit.getEntity(SimpleData.get(ballEntity, Keys.TARGET_ENTITY_UUID));

            if (e == null || e.isDead() || !(e instanceof LivingEntity livingE))
                return null;

            return livingE.getEyeLocation().toVector();
        }

        return null;
    }

    // How the homing physics works:
    // 1. Identify 2 vectors: the current direction of the rocket (d) and the vector pointing from the rocket pointing
    // toward the target location (dTarget). dTarget is found by subtracting the target location from the
    // rocket's location.
    // 2. Normalize dTarget, but NOT d.
    // 3. Linearly interpolate d and dTarget based on turn factor (tf). The algorithm for doing this is as follows:
    //    a. Subtract d from dTarget and multiply by tf. We will call this vector dd.
    //    b. Add dd to d.
    // 4. Set the vector found in #3 to be the new d.
    // 5. Set the velocity of the rocket to be d multiplied by the max speed of the rocket.
    //
    // This is simulated multiple times according to the time delta and target tick speed. TF2 runs at 66 tps
    // and Minecraft runs at 20 tps. This means that the algorithm is run 3 times and interpolated by .1 on the
    // 4th iteration. The actual in game velocity is set according to the simulated position subtracted by the current
    // position.
    public static void performHoming(WindCharge ballEntity, double timeDelta) {
        double maxSpeedScaled = SimpleData.get(ballEntity, Keys.MAX_SPEED) * SIMULATED_TICK_DURATION;
        double turnFactor = SimpleData.get(ballEntity, Keys.TURN_FACTOR) / TURN_RATE_MODIFIER;
        Vector currentDirection = SimpleData.get(ballEntity, Keys.DIRECTION_VEC);
        Vector currentPosition = ballEntity.getLocation().toVector();
        Vector destination = getDestination(ballEntity);
        int i = 0;

        if (destination == null)
            return;

        while(timeDelta > 0) {
            double deltaScale = timeDelta < SIMULATED_TICK_DURATION ? timeDelta / SIMULATED_TICK_DURATION : 1.0;
            Vector toTargetDirection = destination.clone().subtract(currentPosition).normalize();
            Vector newDirection = lerpVectors(currentDirection, toTargetDirection, turnFactor);
            currentDirection = lerpVectors(currentDirection, newDirection, deltaScale);
            currentPosition = currentPosition.add(currentDirection.clone().multiply(maxSpeedScaled).multiply(deltaScale));
            timeDelta -= SIMULATED_TICK_DURATION;
            i++;
        }

        ballEntity.setVelocity(currentPosition.subtract(ballEntity.getLocation().toVector()));
        SimpleData.set(ballEntity, Keys.DIRECTION_VEC, currentDirection);
    }

    public static void tick(WindCharge ballEntity) {
        if (!isBall(ballEntity))
            return;

        performHoming(ballEntity, MINECRAFT_TICK_DURATION);

        Color particleColor = Color.fromRGB(255, 0, 0);
        Particle.DustOptions dustOptions = new Particle.DustOptions(particleColor, 1.5F);
        ballEntity.getWorld().spawnParticle(Particle.DUST, ballEntity.getLocation(), 1, 0, 0, 0, 0, dustOptions);
    }

    private static Vector rotateVectors(Vector a, Vector b, double percentage) {
        if (percentage == 1)
            return b.clone();

        Vector axis = a.getCrossProduct(b).normalize();
        double angle = a.angle(b) * percentage;

        return a.clone().rotateAroundAxis(axis, angle);
    }

    private static Vector lerpVectors(Vector a, Vector b, double percentage) {
        Vector difference = b.clone().subtract(a).multiply(percentage);
        return a.clone().add(difference);
    }
}
