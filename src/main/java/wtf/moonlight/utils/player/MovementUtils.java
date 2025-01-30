/*
 * MoonLight Hacked Client
 *
 * A free and open-source hacked client for Minecraft.
 * Developed using Minecraft's resources.
 *
 * Repository: https://github.com/randomguy3725/MoonLight
 *
 * Author(s): [Randumbguy & opZywl & lucas]
 */
package wtf.moonlight.utils.player;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.jetbrains.annotations.NotNull;
import wtf.moonlight.events.impl.player.MoveEvent;
import wtf.moonlight.events.impl.player.MoveInputEvent;
import wtf.moonlight.features.modules.impl.combat.TargetStrafe;
import wtf.moonlight.utils.InstanceAccess;

import java.util.Arrays;

import static java.lang.Math.toRadians;

public class MovementUtils implements InstanceAccess {
    public static final double WALK_SPEED = 0.221;
    public static final double BUNNY_SLOPE = 0.66;
    public static final double MOD_SPRINTING = 1.3F;
    public static final double MOD_SNEAK = 0.3F;
    public static final double MOD_ICE = 2.5F;
    public static final double MOD_WEB = 0.105 / WALK_SPEED;
    public static final double JUMP_HEIGHT = 0.42F;
    public static final double BUNNY_FRICTION = 159.9F;
    public static final double Y_ON_GROUND_MIN = 0.00001;
    public static final double Y_ON_GROUND_MAX = 0.0626;
    public static final double MOD_SWIM = 0.115F / WALK_SPEED;
    public static final double[] MOD_DEPTH_STRIDER = {
            1.0F,
            0.1645F / MOD_SWIM / WALK_SPEED,
            0.1995F / MOD_SWIM / WALK_SPEED,
            1.0F / MOD_SWIM,
    };

    public static final double BASE_JUMP_HEIGHT = 0.41999998688698;

    public static final double UNLOADED_CHUNK_MOTION = -0.09800000190735147;
    public static final double HEAD_HITTER_MOTION = -0.0784000015258789;

    public static boolean isMoving() {
        return isMoving(mc.thePlayer);
    }
    public static boolean isMoving(EntityLivingBase player) {
        return player != null && (player.moveForward != 0F || player.moveStrafing != 0F);
    }

    public static double getSpeed(EntityPlayer player) {
        return Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ);
    }

    public static double getSpeed() {
        return getSpeed(mc.thePlayer);
    }

    public static void strafe() {
        strafe(getSpeed());
    }

    public static void strafe(final double speed) {
        if (!isMoving())
            return;

        final double yaw = getDirection();
        mc.thePlayer.motionX = -Math.sin(yaw) * speed;
        mc.thePlayer.motionZ = Math.cos(yaw) * speed;
    }

    public static void strafe(final double speed, double yaw) {
        if (!isMoving())
            return;

        mc.thePlayer.motionX = -Math.sin(yaw) * speed;
        mc.thePlayer.motionZ = Math.cos(yaw) * speed;
    }


    public static void strafe(MoveEvent event, double speed) {
        float direction = (float) getDirection();

        if (isMoving()) {
            event.setX(mc.thePlayer.motionX = -Math.sin(direction) * speed);
            event.setZ(mc.thePlayer.motionZ = Math.cos(direction) * speed);
        } else {
            event.setX(mc.thePlayer.motionX = 0);
            event.setZ(mc.thePlayer.motionZ = 0);
        }
    }

    public static double getDirection(float moveForward, float moveStrafing, float rotationYaw) {
        if (moveForward < 0) {
            rotationYaw += 180;
        }

        float forward = 1;

        if (moveForward < 0) {
            forward = -0.5F;
        } else if (moveForward > 0) {
            forward = 0.5F;
        }

        if (moveStrafing > 0) {
            rotationYaw -= 70 * forward;
        }

        if (moveStrafing < 0) {
            rotationYaw += 70 * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public static double getDirection() {
        float rotationYaw;

        if(INSTANCE.getModuleManager().getModule(TargetStrafe.class).isEnabled() && INSTANCE.getModuleManager().getModule(TargetStrafe.class).active && INSTANCE.getModuleManager().getModule(TargetStrafe.class).target != null){
            rotationYaw = INSTANCE.getModuleManager().getModule(TargetStrafe.class).yaw;
        } else {
            rotationYaw = mc.thePlayer.rotationYaw;
        }

        if (mc.thePlayer.movementInput.moveForward < 0F)
            rotationYaw += 180F;

        float forward = 1F;

        if (mc.thePlayer.movementInput.moveForward < 0F)
            forward = -0.5F;
        else if (mc.thePlayer.movementInput.moveForward > 0F)
            forward = 0.5F;

        if (mc.thePlayer.movementInput.moveStrafe > 0F)
            rotationYaw -= 90F * forward;

        if (mc.thePlayer.movementInput.moveStrafe < 0F)
            rotationYaw += 90F * forward;

        return toRadians(rotationYaw);
    }

    public static float getRawDirectionRotation(float yaw, float pStrafe, float pForward) {
        float rotationYaw = yaw;

        if (pForward < 0F)
            rotationYaw += 180F;

        float forward = 1F;
        if (pForward < 0F)
            forward = -0.5F;
        else if (pForward > 0F)
            forward = 0.5F;

        if (pStrafe > 0F)
            rotationYaw -= 90F * forward;

        if (pStrafe < 0F)
            rotationYaw += 90F * forward;

        return rotationYaw;
    }

    public static float getRawDirection() {
        return getRawDirectionRotation(mc.thePlayer.rotationYaw, mc.thePlayer.moveStrafing, mc.thePlayer.moveForward);
    }

    public static int getSpeedEffect(EntityPlayer player) {
        return player.isPotionActive(Potion.moveSpeed) ? player.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1 : 0;
    }

    public static int getSpeedEffect() {
        return getSpeedEffect(mc.thePlayer);
    }

    public static void stopXZ() {
        mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
    }

    public static void stop() {
        mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0;
    }

    public static double getBPS() {
        return getBPS(mc.thePlayer);
    }

    public static double getBPS(EntityPlayer player) {
        if (player == null || player.ticksExisted < 1) {
            return 0.0;
        }
        return getDistance(player.lastTickPosX, player.lastTickPosZ) * (20.0f * mc.timer.timerSpeed);
    }

    public static double getDistance(final double x, final double z) {
        final double xSpeed = mc.thePlayer.posX - x;
        final double zSpeed = mc.thePlayer.posZ - z;
        return MathHelper.sqrt_double(xSpeed * xSpeed + zSpeed * zSpeed);
    }

    public static boolean isMovingStraight() {
        float direction = getRawDirection() + 180;
        float movingYaw = Math.round(direction / 45) * 45;
        return movingYaw % 90 == 0f;
    }

    public static boolean canSprint(final boolean legit) {
        return (legit ? mc.thePlayer.moveForward >= 0.8F
                && !mc.thePlayer.isCollidedHorizontally
                && (mc.thePlayer.getFoodStats().getFoodLevel() > 6 || mc.thePlayer.capabilities.allowFlying)
                && !mc.thePlayer.isPotionActive(Potion.blindness)
                //&& !mc.thePlayer.isUsingItem()
                && !mc.thePlayer.isSneaking()
                : enoughMovementForSprinting());
    }

    public static boolean enoughMovementForSprinting() {
        return Math.abs(mc.thePlayer.moveForward) >= 0.8F || Math.abs(mc.thePlayer.moveStrafing) >= 0.8F;
    }

    public static boolean isGoingDiagonally(double amount) {
        return Math.abs(mc.thePlayer.motionX) > amount && Math.abs(mc.thePlayer.motionZ) > amount;
    }

    public static double getBaseMoveSpeed(EntityPlayer player) {
        var baseSpeed = 0.2873;
        if (player.isPotionActive(Potion.moveSpeed)) {
            int amplifier = player.getActivePotionEffect(Potion.moveSpeed).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        return baseSpeed;
    }

    public static double getBaseMoveSpeed() {
        return getBaseMoveSpeed(mc.thePlayer);
    }

    public static double getJumpHeight() {
        double jumpY = BASE_JUMP_HEIGHT;

        if (mc.thePlayer.isPotionActive(Potion.jump)) {
            jumpY += (float) (mc.thePlayer.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F;
        }

        return jumpY;
    }

    public static void jump(MoveEvent event) {
        event.setY(mc.thePlayer.motionY = getJumpHeight());
    }

    public static int depthStriderLevel() {
        if (mc.thePlayer == null)
            return 0;
        return EnchantmentHelper.getDepthStriderModifier(mc.thePlayer);
    }

    public static double getAllowedHorizontalDistance() {
        double horizontalDistance;
        boolean useBaseModifiers = false;

        if (mc.thePlayer.isInWeb) {
            horizontalDistance = MOD_WEB * WALK_SPEED;
        } else if (PlayerUtils.inLiquid()) {
            horizontalDistance = MOD_SWIM * WALK_SPEED;

            final int depthStriderLevel = depthStriderLevel();
            if (depthStriderLevel > 0) {
                horizontalDistance *= MOD_DEPTH_STRIDER[depthStriderLevel];
                useBaseModifiers = true;
            }

        } else if (mc.thePlayer.isSneaking()) {
            horizontalDistance = MOD_SNEAK * WALK_SPEED;
        } else {
            horizontalDistance = WALK_SPEED;
            useBaseModifiers = true;
        }

        if (useBaseModifiers) {
            if (canSprint(false)) {
                horizontalDistance *= MOD_SPRINTING;
            }

            if (mc.thePlayer.isPotionActive(Potion.moveSpeed) && mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getDuration()
                    > 0) {
                horizontalDistance *= 1 + (0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1));
            }

            if (mc.thePlayer.isPotionActive(Potion.moveSlowdown)) {
                horizontalDistance = 0.29;
            }
        }

        return horizontalDistance;
    }

    public static double predictedMotionY(final double motion, final int ticks) {
        if (ticks == 0) return motion;
        double predicted = motion;

        for (int i = 0; i < ticks; i++) {
            predicted = (predicted - 0.08) * 0.98F;
        }

        return predicted;
    }

    /**
     * Gets the players' movement yaw
     */
    public static double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    public static void boost(double increase) {
        if (!isMoving()) return;
        final double yaw = getDirection();
        mc.thePlayer.motionX += -MathHelper.sin((float) yaw) * increase;
        mc.thePlayer.motionZ += MathHelper.cos((float) yaw) * increase;
    }
    /**
     * Fixes the players movement
     */
    public static void fixMovement(final MoveInputEvent event, final float yaw) {
        final float forward = event.getForward();
        final float strafe = event.getStrafe();

        final double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(mc.thePlayer.rotationYaw, forward, strafe)));

        if (forward == 0 && strafe == 0) {
            return;
        }

        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedStrafe == 0 && predictedForward == 0) continue;

                final double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(yaw, predictedForward, predictedStrafe)));
                final double difference = Math.abs(angle - predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setForward(closestForward);
        event.setStrafe(closestStrafe);
    }
    public static void moveFlying(double increase) {
        if (!MovementUtils.isMoving()) return;
        final double yaw = MovementUtils.getDirection();
        mc.thePlayer.motionX += -MathHelper.sin((float) yaw) * increase;
        mc.thePlayer.motionZ += MathHelper.cos((float) yaw) * increase;
    }

    public static void preventDiagonalSpeed() {
        KeyBinding[] gameSettings = new KeyBinding[]{mc.gameSettings.keyBindForward, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindLeft};

        final int[] down = {0};

        Arrays.stream(gameSettings).forEach(keyBinding -> down[0] = down[0] + (keyBinding.isKeyDown() ? 1 : 0));

        boolean active = down[0] == 1;

        if (active) return;

        final double groundIncrease = (0.1299999676734952 - 0.12739998266255503) + 1E-7 - 1E-8;
        final double airIncrease = (0.025999999334873708 - 0.025479999685988748) - 1E-8;
        final double increase = mc.thePlayer.onGround ? groundIncrease : airIncrease;

        moveFlying(-increase);
    }

    public static void useDiagonalSpeed() {
        KeyBinding[] gameSettings = new KeyBinding[]{mc.gameSettings.keyBindForward, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindLeft};

        final int[] down = {0};

        Arrays.stream(gameSettings).forEach(keyBinding -> {
            down[0] = down[0] + (keyBinding.isKeyDown() ? 1 : 0);
        });

        boolean active = down[0] == 1;

        if (!active) return;

        final double groundIncrease = (0.1299999676734952 - 0.12739998266255503) + 1E-7 - 1E-8;
        final double airIncrease = (0.025999999334873708 - 0.025479999685988748) - 1E-8;
        final double increase = mc.thePlayer.onGround ? groundIncrease : airIncrease;

        moveFlying(increase);
    }

    public static double[] moveFlying(float strafe, float forward, final boolean onGround, final float yaw, final boolean sprinting) {
        float friction = 0.02f;
        final float playerWalkSpeed = mc.thePlayer.getAIMoveSpeed();
        if (onGround) {
            final float f4 = 0.6f * 0.91f;
            final float f = 0.16277136F / (f4 * f4 * f4);
            friction = playerWalkSpeed / 2.0f * f;
        }
        if (sprinting) {
            friction = (float) ((double) friction + ((onGround) ? (playerWalkSpeed / 2.0f) : 0.02f) * 0.3D);
        }
        float f = strafe * strafe + forward * forward;
        if (f >= 1.0E-4F) {
            f = MathHelper.sqrt_float(f);
            if (f < 1.0F) {
                f = 1.0F;
            }
            f = friction / f;
            strafe = strafe * f;
            forward = forward * f;
            final float f1 = MathHelper.sin(yaw * (float) (Math.PI * 2) / 180.0F);
            final float f2 = MathHelper.cos(yaw * (float) (Math.PI * 2) / 180.0F);
            final double motionX = (strafe * f2 - forward * f1);
            final double motionZ = (forward * f2 + strafe * f1);
            return new double[]{motionX, motionZ};
        }
        return null;
    }

    public static void handleUsingItem() {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.motionX *= 1.52;
            mc.thePlayer.motionZ *= 1.52;
        }
    }

    public static void handleMovement() {
        if (mc.thePlayer.onGround) {
            handleGroundMovement();
        } else {
            handleAirMovement();
        }
    }

    private static void handleGroundMovement() {
        if (mc.thePlayer.isSprinting()) {
            if (isMovingForwardAndSideways()) {
                mc.thePlayer.motionX *= 1.008;
                mc.thePlayer.motionZ *= 1.008;
            } else if (isMovingForward()) {
                mc.thePlayer.motionX *= 1.023;
                mc.thePlayer.motionZ *= 1.023;
                if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                    mc.timer.timerSpeed = 1.008F;
                    mc.thePlayer.motionX *= 1.008;
                    mc.thePlayer.motionZ *= 1.008;
                }
            }
        } else {
            if (isMovingForwardAndSideways()) {
                mc.thePlayer.motionX *= 1.2;
                mc.thePlayer.motionZ *= 1.2;
            } else if (isMovingForward()) {
                mc.thePlayer.motionX *= 1.21;
                mc.thePlayer.motionZ *= 1.21;
            }
        }
        mc.timer.timerSpeed = 1.0f;
    }

    private static void handleAirMovement() {
        mc.timer.timerSpeed = 1.008f;
        mc.thePlayer.motionX *= 1.013;
        mc.thePlayer.motionZ *= 1.013;
        if (mc.thePlayer.fallDistance < 0.1) {
            mc.thePlayer.motionY *= 1.029;
        }
    }

    private static boolean isMovingForward() {
        return mc.gameSettings.keyBindForward.isKeyDown();
    }

    private static boolean isMovingForwardAndSideways() {
        return (mc.gameSettings.keyBindRight.isKeyDown() || mc.gameSettings.keyBindLeft.isKeyDown()) && isMovingForward();
    }
}
