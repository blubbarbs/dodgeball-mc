package com.gmail.blubberalls.dodgeball;

import com.gmail.blubberalls.simpledata.SD;
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
        public static SD.Key<Vector> DIRECTION_VEC = new SD.Key<>("db", "__direction", DataType.VECTOR);
        public static SD.Key<Double> TURN_FACTOR = new SD.Key<>("db", "turn_factor", PersistentDataType.DOUBLE);
        public static SD.Key<Double> MAX_SPEED = new SD.Key<>("db", "max_speed", PersistentDataType.DOUBLE);
        public static SD.Key<UUID> TARGET_ENTITY_UUID_DEBUG = new SD.Key<>("db", "target_uuid_debug", DataType.UUID);
        public static SD.Key<UUID> TARGET_ENTITY_UUID = new SD.Key<>("db", "target_uuid", DataType.UUID);
        public static SD.Key<Vector> TARGET_LOCATION = new SD.Key<>("db", "target_location", DataType.VECTOR);
    }

    public static boolean isBall(Entity e) {
        return SD.has(e, Keys.TURN_FACTOR);
    }

    public static WindCharge spawn(Location location) {
        WindCharge ballEntity = location.getWorld().spawn(location, WindCharge.class);
        ballEntity.setNoPhysics(true);
        ballEntity.setPower(new Vector(0, 0, 0));
        SD.set(ballEntity, Keys.DIRECTION_VEC, new Vector(1, 0, 0));
        SD.set(ballEntity, Keys.MAX_SPEED, Defaults.MAX_SPEED);
        SD.set(ballEntity, Keys.TURN_FACTOR, Defaults.TURN_FACTOR);

        return ballEntity;
    }

    public static void setTargetEntity(WindCharge ballEntity, LivingEntity entity) {
        SD.set(ballEntity, Keys.TARGET_ENTITY_UUID, entity.getUniqueId());
        SD.remove(ballEntity, Keys.TARGET_ENTITY_UUID_DEBUG);
        SD.remove(ballEntity, Keys.TARGET_LOCATION);
    }

    public static void setTargetEntityDebug(WindCharge ballEntity, LivingEntity entity) {
        SD.remove(ballEntity, Keys.TARGET_ENTITY_UUID);
        SD.set(ballEntity, Keys.TARGET_ENTITY_UUID_DEBUG, entity.getUniqueId());
        SD.remove(ballEntity, Keys.TARGET_LOCATION);
    }

    public static void setTargetLocation(WindCharge ballEntity, Vector location) {
        SD.remove(ballEntity, Keys.TARGET_ENTITY_UUID);
        SD.remove(ballEntity, Keys.TARGET_ENTITY_UUID_DEBUG);
        SD.set(ballEntity, Keys.TARGET_LOCATION, location);
    }

    public static void setTargetLocation(WindCharge ballEntity, Location location) {
        setTargetLocation(ballEntity, location.toVector());
    }

    public static void setTurnFactor(WindCharge ballEntity, double turnFactor) {
        SD.set(ballEntity, Keys.TURN_FACTOR, turnFactor);
    }

    public static void setMaxSpeed(WindCharge ballEntity, double speed) {
        SD.set(ballEntity, Keys.MAX_SPEED, speed);
    }

    public static Vector getDestination(WindCharge ballEntity) {
        if (SD.has(ballEntity, Keys.TARGET_LOCATION)) {
            return SD.get(ballEntity, Keys.TARGET_LOCATION);
        } else if (SD.has(ballEntity, Keys.TARGET_ENTITY_UUID_DEBUG)) {
            Entity e = Bukkit.getEntity(SD.get(ballEntity, Keys.TARGET_ENTITY_UUID_DEBUG));

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
        else if (SD.has(ballEntity, Keys.TARGET_ENTITY_UUID)) {
            Entity e = Bukkit.getEntity(SD.get(ballEntity, Keys.TARGET_ENTITY_UUID));

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

    // TODO: Calculations for reflections, namely speed/turn increases
    public static void performHoming(WindCharge ballEntity, double timeDelta) {
        double maxSpeedScaled = SD.get(ballEntity, Keys.MAX_SPEED) * SIMULATED_TICK_DURATION;
        double turnFactor = SD.get(ballEntity, Keys.TURN_FACTOR) / TURN_RATE_MODIFIER;
        Vector currentDirection = SD.get(ballEntity, Keys.DIRECTION_VEC);
        Vector currentPosition = ballEntity.getLocation().toVector();
        Vector destination = getDestination(ballEntity);

        if (destination == null)
            return;

        while(timeDelta > 0) {
            double deltaScale = timeDelta < SIMULATED_TICK_DURATION ? timeDelta / SIMULATED_TICK_DURATION : 1.0;
            Vector toTargetDirection = destination.clone().subtract(currentPosition).normalize();
            Vector newDirection = lerpVectors(currentDirection, toTargetDirection, turnFactor);
            currentDirection = lerpVectors(currentDirection, newDirection, deltaScale);
            currentPosition = currentPosition.add(currentDirection.clone().multiply(maxSpeedScaled).multiply(deltaScale));
            timeDelta -= SIMULATED_TICK_DURATION;
        }

        ballEntity.setVelocity(currentPosition.subtract(ballEntity.getLocation().toVector()));
        SD.set(ballEntity, Keys.DIRECTION_VEC, currentDirection);
    }

    public static void tick(WindCharge ballEntity) {
        if (!isBall(ballEntity))
            return;

        performHoming(ballEntity, MINECRAFT_TICK_DURATION);

        // TODO: Send spawn particle packets instead of using World#spawnParticle
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
