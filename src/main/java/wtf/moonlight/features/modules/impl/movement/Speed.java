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
package wtf.moonlight.features.modules.impl.movement;

import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import org.lwjglx.input.Keyboard;
import wtf.moonlight.events.annotations.EventTarget;
import wtf.moonlight.events.impl.packet.PacketEvent;
import wtf.moonlight.events.impl.player.*;
import wtf.moonlight.features.modules.Module;
import wtf.moonlight.features.modules.ModuleCategory;
import wtf.moonlight.features.modules.ModuleInfo;
import wtf.moonlight.features.modules.impl.exploit.Disabler;
import wtf.moonlight.features.values.impl.BoolValue;
import wtf.moonlight.features.values.impl.ModeValue;
import wtf.moonlight.features.values.impl.SliderValue;
import wtf.moonlight.utils.math.MathUtils;
import wtf.moonlight.utils.misc.DebugUtils;
import wtf.moonlight.utils.player.FallDistanceComponent;
import wtf.moonlight.utils.player.MovementUtils;
import wtf.moonlight.utils.player.PlayerUtils;
import wtf.moonlight.utils.player.RotationUtils;

import java.util.Objects;

@ModuleInfo(name = "Speed", category = ModuleCategory.Movement, key = Keyboard.KEY_V)
public class Speed extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"Watchdog", "EntityCollide", "BlocksMC", "Intave", "NCP", "Miniblox", "Legit"}, "Watchdog", this);
    private final ModeValue wdMode = new ModeValue("Watchdog Mode", new String[]{"Basic", "Glide","Full Strafe", "Test"}, "Basic", this, () -> mode.is("Watchdog"));
    private final BoolValue fallStrafe = new BoolValue("Fall Strafe", true, this, () -> mode.is("Watchdog") && wdMode.is("Full Strafe"));
    private final BoolValue frictionOverride = new BoolValue("Friction Override", true, this, () -> mode.is("Watchdog") && wdMode.is("Full Strafe"));
    private final BoolValue extraStrafe = new BoolValue("Extra Strafe", true, this, () -> mode.is("Watchdog") && wdMode.is("Full Strafe"));
    private final BoolValue boost = new BoolValue("Boost", true, this, () -> mode.is("Watchdog") && wdMode.is("Basic"));
    private final BoolValue fastFall = new BoolValue("Fast Fall", true, this, () -> mode.is("Watchdog") && wdMode.is("Basic"));
    private final BoolValue hurtTimeCheck = new BoolValue("Hurt Time Check", false, this, () -> mode.is("Watchdog") && (wdMode.is("Full Strafe") || fastFall.canDisplay() && fastFall.get()));
    private final ModeValue wdFastFallMode = new ModeValue("Fast Fall Mode", new String[]{"Normal","Test 1","Test 2","Test 3","Test 4","Test 5","Predict", "Predict 2","8 Tick","7 Tick"}, "8 Tick", this, () -> mode.is("Watchdog") && fastFall.canDisplay() && fastFall.get());
    private final SliderValue predictTicks = new SliderValue("Predict Ticks",5,4,6,1,this,() -> fastFall.canDisplay() && fastFall.get() && wdFastFallMode.is("Predict"));
    private final BoolValue expand = new BoolValue("More Expand", false, this, () -> Objects.equals(mode.get(), "EntityCollide"));
    private final BoolValue ignoreDamage = new BoolValue("Ignore Damage", true, this, () -> Objects.equals(mode.get(), "EntityCollide"));
    private final BoolValue pullDown = new BoolValue("Pull Down", true, this, () -> Objects.equals(mode.get(), "NCP"));
    private final SliderValue onTick = new SliderValue("On Tick", 5, 1, 10, 1, this, () -> Objects.equals(mode.get(), "NCP") && pullDown.get());
    private final BoolValue onHurt = new BoolValue("On Hurt", true, this, () -> Objects.equals(mode.get(), "NCP") && pullDown.get());
    private final BoolValue airBoost = new BoolValue("Air Boost", true, this, () -> Objects.equals(mode.get(), "NCP"));
    private final BoolValue damageBoost = new BoolValue("Damage Boost", false, this, () -> Objects.equals(mode.get(), "NCP"));
    private final SliderValue mTicks = new SliderValue("Ticks", 5, 1, 6, 1, this, () -> Objects.equals(mode.get(), "Miniblox"));
    public final BoolValue noBob = new BoolValue("No Bob", true, this);
    private final BoolValue forceStop = new BoolValue("Force Stop", true, this);
    private final BoolValue lagBackCheck = new BoolValue("Lag Back Check", true, this);
    private final BoolValue liquidCheck = new BoolValue("Liquid Check", true, this);
    private final BoolValue guiCheck = new BoolValue("Gui Check", true, this);
    private final BoolValue debug = new BoolValue("Debug", true, this);
    private final BoolValue printOffGroundTicks = new BoolValue("Print Off Ground Ticks", true, this);
    private boolean disable;
    private boolean disable3;
    private int boostTicks;
    private boolean recentlyCollided;
    private boolean ice;
    private boolean slab;
    private boolean stopVelocity;
    public boolean couldStrafe;
    private double speed;
    private int ticksSinceTeleport;

    @Override
    public void onEnable() {
        if (mode.is("Watchdog")) {
            if (wdMode.is("Glide")) {
                speed = 0.28;
            }
            slab = false;

            disable3 = false;
            if(mc.thePlayer.offGroundTicks > 2){
                disable = true;
            }
        }
    }

    @Override
    public void onDisable() {
        ice = false;
        disable = false;
        couldStrafe = false;
        if(forceStop.get()){
            MovementUtils.strafe(0);
        }
        mc.timer.timerSpeed = 1.0f;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        setTag(mode.get());
        ticksSinceTeleport++;
        if(liquidCheck.get() && (mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) || guiCheck.get() && mc.currentScreen instanceof GuiContainer)
            return;

        if (printOffGroundTicks.get())
            DebugUtils.sendMessage(mc.thePlayer.offGroundTicks + "Tick");

        switch (mode.get()) {
            case "Miniblox": {
                if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                    mc.thePlayer.jump();
                }

                switch (mc.thePlayer.offGroundTicks) {
                    case 1: {
                        switch ((int) mTicks.get()) {
                            case 1:
                                mc.thePlayer.motionY -= 0.76;
                                break;
                            case 2:
                                mc.thePlayer.motionY -= 0.52;
                                break;
                            case 3:
                                mc.thePlayer.motionY -= 0.452335182447;
                                break;
                            case 4:
                                mc.thePlayer.motionY -= 0.322335182447;
                                break;
                            case 5:
                                mc.thePlayer.motionY -= 0.232335182447;
                                break;
                            case 6:
                                mc.thePlayer.motionY -= 0.162335182447;
                                break;
                        }
                    }
                    break;

                    case 3: {
                        mc.thePlayer.motionY -= 0.1523351824467155;
                    }
                    break;
                }
            }
            break;

            case "NCP": {
                if (mc.thePlayer.offGroundTicks == onTick.get() && pullDown.get()) {
                    MovementUtils.strafe();
                    mc.thePlayer.motionY -= 0.1523351824467155;
                }

                if (onHurt.get() && mc.thePlayer.hurtTime >= 5 && mc.thePlayer.motionY >= 0) {
                    mc.thePlayer.motionY -= 0.1;
                }

                if (airBoost.get() && MovementUtils.isMoving()) {
                    mc.thePlayer.motionX *= 1f + 0.00718;
                    mc.thePlayer.motionZ *= 1f + 0.00718;
                }
            }
            break;

            case "Intave": {
                if (mc.thePlayer.motionY > 0.03 && mc.thePlayer.isSprinting()) {
                    mc.thePlayer.motionX *= 1f + 0.003;
                    mc.thePlayer.motionZ *= 1f + 0.003;
                }
            }
            break;

            case "EntityCollide": {
                couldStrafe = false;
                if (mc.thePlayer.hurtTime <= 1) {
                    stopVelocity = false;
                }

                if (stopVelocity && !ignoreDamage.get()) {
                    return;
                }

                if (!MovementUtils.isMoving())
                    return;

                int collisions = 0;
                AxisAlignedBB box = expand.get() ? mc.thePlayer.getEntityBoundingBox().expand(1.0, 1.0, 1.0)
                        : mc.thePlayer.getEntityBoundingBox().expand(0.8, 0.8, 0.8);
                for (Entity entity : mc.theWorld.getLoadedEntityList()) {
                    AxisAlignedBB entityBox = entity.getEntityBoundingBox();
                    if (canCauseSpeed(entity) && box.intersectsWith(entityBox)) {
                        collisions++;
                    }
                }

                double yaw = Math.toRadians(RotationUtils.shouldRotate() ? RotationUtils.currentRotation[0] : mc.thePlayer.rotationYaw);

                double boost = 0.078 * collisions;
                mc.thePlayer.addVelocity(-Math.sin(yaw) * boost, 0.0, Math.cos(yaw) * boost);
            }
            break;

            case "Watchdog":

                if (isEnabled(Disabler.class) && getModule(Disabler.class).options.isEnabled("Watchdog Motion") && !getModule(Disabler.class).disabled) {

                    if (fastFall.canDisplay() && fastFall.get()) {
                        if (!disable) {
                            switch (wdFastFallMode.get()) {
                                case "Normal":
                                    switch (mc.thePlayer.offGroundTicks) {
                                        case 5:
                                            mc.thePlayer.motionY = -0.1523351824467155;
                                            break;
                                        case 8:
                                            mc.thePlayer.motionY = -0.3;
                                            break;
                                    }
                                    break;

                                case "Test 1":
                                    switch (mc.thePlayer.offGroundTicks) {
                                        case 1:
                                            MovementUtils.strafe();
                                            couldStrafe = true;
                                            break;
                                        case 4:
                                            mc.thePlayer.motionY -= 0.03;
                                            break;
                                        case 5:
                                            mc.thePlayer.motionY -= 0.1905189780583944;
                                            break;
                                        case 6:
                                            mc.thePlayer.motionY *= 1.01;
                                            break;
                                    }
                                    break;

                                case "Test 2":
                                    switch (mc.thePlayer.offGroundTicks) {
                                        case 4:
                                            mc.thePlayer.motionY -= 0.045;
                                            break;
                                        case 5:
                                            mc.thePlayer.motionY = -0.19;
                                            break;
                                        case 6:
                                            mc.thePlayer.motionY = -0.269;
                                            break;
                                        case 7:
                                            mc.thePlayer.motionY = -0.347;
                                            break;
                                    }
                                    break;

                                case "Test 3":
                                    switch (mc.thePlayer.offGroundTicks) {
                                        case 3:
                                            mc.thePlayer.motionY -= -0.0045;
                                            break;
                                        case 4:
                                            mc.thePlayer.motionY -= 0.186;
                                            break;
                                        case 5:
                                            mc.thePlayer.motionY -= 0.042;
                                            break;
                                        case 7:
                                            mc.thePlayer.motionY += 0.012f;
                                            break;
                                        case 8:
                                            mc.thePlayer.motionY += 0.015f;
                                            break;
                                    }
                                    break;

                                case "Test 4":
                                    switch (mc.thePlayer.offGroundTicks) {
                                        case 3:
                                            mc.thePlayer.motionY -= -0.0025;
                                            break;
                                        case 4:
                                            mc.thePlayer.motionY -= 0.04;
                                            break;
                                        case 5:
                                            mc.thePlayer.motionY -= 0.1905189780583944;
                                            break;
                                        case 6:
                                            mc.thePlayer.motionX *= 1.001;
                                            mc.thePlayer.motionZ *= 1.001;
                                        case 7:
                                            mc.thePlayer.motionY -= 0.004;
                                            break;
                                        case 8:
                                            mc.thePlayer.motionY -= 0.01;
                                            break;
                                    }
                                    break;
                                case "7 Tick":
                                    switch (mc.thePlayer.offGroundTicks) {
                                        case 1:
                                            mc.thePlayer.motionY += 0.05700000002980232;
                                            break;
                                        case 3:
                                            mc.thePlayer.motionY -= 0.1309;
                                            break;
                                        case 4:
                                            mc.thePlayer.motionY -= 0.2;
                                            break;
                                    }
                                    break;
                            }
                        }
                    }
                }
                break;
            case "Legit":
                if (mc.thePlayer.isUsingItem()) {
                    MovementUtils.handleUsingItem();
                } else {
                    MovementUtils.handleMovement();
                }
                break;
        }
    }

    @EventTarget
    public void onMotion(MotionEvent event) {

        if(liquidCheck.get() && (mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) || guiCheck.get() && mc.currentScreen instanceof GuiContainer)
            return;

        if (isEnabled(Scaffold.class) && (getModule(Scaffold.class).towering() || getModule(Scaffold.class).towerMoving()))
            return;

        switch (mode.get()) {
            case "Miniblox": {
                if (MovementUtils.isMoving()) {
                    if (mc.thePlayer.onGround) {
                        switch ((int) mTicks.get()) {
                            case 1:
                                MovementUtils.strafe(0.07);
                                break;
                            case 2:
                                MovementUtils.strafe(0.08);
                                break;
                            case 3:
                                MovementUtils.strafe(0.09);
                                break;
                            case 4:
                                MovementUtils.strafe(0.1);
                                break;
                            case 5:
                                MovementUtils.strafe(0.115);
                                break;
                            case 6:
                                MovementUtils.strafe(0.13);
                                break;
                        }
                    } else {
                        MovementUtils.strafe(0.35f);
                    }
                }
            }
            break;

            case "NCP": {
                if (MovementUtils.isMoving()) {
                    couldStrafe = true;
                    MovementUtils.strafe();
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                        MovementUtils.strafe(0.48 + MovementUtils.getSpeedEffect() * 0.07);
                    }
                }

                if (damageBoost.get() && mc.thePlayer.hurtTime > 0) {
                    MovementUtils.strafe(Math.max(MovementUtils.getSpeed(),0.5));
                }
            }
            break;

            case "Watchdog":
                if (event.isPre()) {
                    switch (wdMode.get()) {
                        case "Basic":
                            if (MovementUtils.isMoving()) {
                                if (mc.thePlayer.onGround) {
                                    float baseSpeed = 0.482f;
                                    mc.thePlayer.jump();
                                    MovementUtils.strafe(baseSpeed + ((MovementUtils.getSpeedEffect() * 0.057f)));
                                }

                                if (boost.get()) {
                                    if (mc.thePlayer.offGroundTicks == 1)
                                        MovementUtils.strafe(MovementUtils.getBaseMoveSpeed());
                                }
                            }
                            break;
                        case "Full Strafe":
                            if(mc.thePlayer.isCollidedHorizontally || ticksSinceTeleport < 2){
                                recentlyCollided = true;
                                boostTicks = mc.thePlayer.ticksExisted+9;
                            }
                            if (!mc.thePlayer.isCollidedHorizontally && (mc.thePlayer.ticksExisted > boostTicks)){

                                recentlyCollided = false;

                            }

                            if (PlayerUtils.blockRelativeToPlayer(0, -1.0, 0) == (Blocks.packed_ice) || PlayerUtils.blockRelativeToPlayer(0, -1.0, 0) == (Blocks.ice)) {

                                ice = true;
                            } else if(mc.thePlayer.offGroundTicks>1){
                                ice = false;
                            }


                            if(mc.thePlayer.onGround){
                                disable3 = false;
                            }
                            if (PlayerUtils.blockRelativeToPlayer(0, mc.thePlayer.motionY, 0) != Blocks.air) {
                                disable = false;
                            }

                            if(mc.thePlayer.isCollidedVertically && !mc.thePlayer.onGround && PlayerUtils.isBlockOver(2.0)){
                                disable = true;
                            }

                            double posY = event.getY();
                            if (Math.abs(posY - Math.round(posY)) > 0.03 && mc.thePlayer.onGround) {
                                slab = true;
                            } else if(mc.thePlayer.onGround){
                                slab = false;
                            }

                            break;
                    }

                    if (fastFall.canDisplay() && fastFall.get()) {

                        if(mc.thePlayer.isInWater() ||
                                mc.thePlayer.isInWeb ||
                                mc.thePlayer.isInLava() ||
                                hurtTimeCheck.get() && mc.thePlayer.hurtTime > 0
                        ) {
                            disable = true;
                            return;
                        }
                        if (PlayerUtils.blockRelativeToPlayer(0, mc.thePlayer.motionY, 0) != Blocks.air) {
                            disable = false;
                        }

                        if(mc.thePlayer.isCollidedVertically && !mc.thePlayer.onGround && PlayerUtils.isBlockOver(2.0)){
                            disable = true;
                        }
                    }


                    if (fastFall.canDisplay() && fastFall.get() && isEnabled(Disabler.class) && getModule(Disabler.class).options.isEnabled("Watchdog Motion") && !getModule(Disabler.class).disabled) {

                        if (!disable &&
                                wdFastFallMode.is("8 Tick")) {
                            boolean down = false;
                            int simpleY = (int) Math.round((event.y % 1) * 10000);

                            if(debug.get())
                                DebugUtils.sendMessage(simpleY + "Value, " + event.y);

                            //0
                            //4200
                            //7532
                            //13
                            //1413
                            //2000
                            //1792
                            //804
                            //9051
                            //6550
                            //3315


                            if (simpleY == 13) {
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.02483;
                            }

                            if (simpleY == 2000) {
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.1913;
                            }

                            if (simpleY == 13) down = true;
                            if (down) event.y -= 1E-5;
                        }
                    }
                }

                break;
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {

        if(liquidCheck.get() && (mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) || guiCheck.get() && mc.currentScreen instanceof GuiContainer)
            return;

        if (mode.get().equals("Watchdog") && wdMode.get().equals("Full Strafe") && (mc.thePlayer.isInWater() || mc.thePlayer.isInWeb || mc.thePlayer.isInLava())) {
            disable = true;
            return;
        }
        if (mode.get().equals("Watchdog")) {

            switch (wdMode.get()) {
                case "Glide":
                    if (MovementUtils.isMoving() && mc.thePlayer.onGround) {
                        MovementUtils.strafe(MovementUtils.getAllowedHorizontalDistance());
                        mc.thePlayer.jump();
                    }

                    if (mc.thePlayer.onGround) {
                        speed = 1.0F;
                    }

                    final int[] allowedAirTicks = new int[]{10, 11, 13, 14, 16, 17, 19, 20, 22, 23, 25, 26, 28, 29};

                    if (!(mc.theWorld.getBlockState(mc.thePlayer.getPosition().add(0, -0.25, 0)).getBlock() instanceof BlockAir)) {
                        for (final int allowedAirTick : allowedAirTicks) {
                            if (mc.thePlayer.offGroundTicks == allowedAirTick && allowedAirTick <= 11) {
                                mc.thePlayer.motionY = 0;
                                MovementUtils.strafe(MovementUtils.getAllowedHorizontalDistance() * speed);
                                couldStrafe = true;

                                speed *= 0.98F;

                            }
                        }
                    }
                    break;
                case "Full Strafe":

                    MovementUtils.useDiagonalSpeed();

                    if (mc.thePlayer.isPotionActive(Potion.moveSpeed) && mc.thePlayer.onGround) {
                        if (!(getModule(Scaffold.class).isEnabled()) && !recentlyCollided) {
                            MovementUtils.strafe(fallStrafe.get() ? MovementUtils.getAllowedHorizontalDistance() : MovementUtils.getAllowedHorizontalDistance() * 0.994);
                            couldStrafe = true;
                            mc.thePlayer.jump();
                        } else if ((getModule(Scaffold.class).isEnabled()) && !recentlyCollided) {
                            MovementUtils.strafe(0.29);
                            mc.thePlayer.jump();
                        } else {
                            MovementUtils.strafe(0.29);
                            couldStrafe = true;
                            mc.thePlayer.jump();
                        }


                    } else if (mc.thePlayer.onGround) {

                        if (!recentlyCollided) {
                            MovementUtils.strafe(MovementUtils.getAllowedHorizontalDistance());

                        } else if ((getModule(Scaffold.class).isEnabled())) {
                            MovementUtils.strafe(.23);
                        } else {
                            //  ChatUtil.display("s");
                            MovementUtils.strafe(MovementUtils.getBaseMoveSpeed());
                        }
                        mc.thePlayer.jump();
                    }

                    if (mc.thePlayer.offGroundTicks == 1 && !disable) {
                        mc.thePlayer.motionY += 0.057f;

                        if (mc.thePlayer.isPotionActive(Potion.moveSpeed) && !(getModule(Scaffold.class).isEnabled() && mc.gameSettings.keyBindJump.isKeyDown()) && mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1 >= 2 && !disable && !recentlyCollided) {
                            MovementUtils.strafe(0.48);
                            couldStrafe = true;

                        } else if (mc.thePlayer.isPotionActive(Potion.moveSpeed) && mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1 >= 2) {
                            MovementUtils.strafe(0.4);
                            couldStrafe = true;

                        } else if (mc.thePlayer.isPotionActive(Potion.moveSpeed) && mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1 == 1) {
                            MovementUtils.strafe(0.405);
                            couldStrafe = true;
                        } else {
                            MovementUtils.strafe(0.33);
                            couldStrafe = true;
                        }
                    }


                    if (mc.thePlayer.offGroundTicks == 2 && !disable && extraStrafe.get()) {
                        double motionX3 = mc.thePlayer.motionX;
                        double motionZ3 = mc.thePlayer.motionZ;
                        mc.thePlayer.motionZ = (mc.thePlayer.motionZ * 1 + motionZ3 * 2) / 3;
                        mc.thePlayer.motionX = (mc.thePlayer.motionX * 1 + motionX3 * 2) / 3;
                    }


                    if (mc.thePlayer.offGroundTicks == 3 && !disable) {
                        mc.thePlayer.motionY -= 0.1309f;
                    }

                    if (mc.thePlayer.offGroundTicks == 4 && !disable) {
                        mc.thePlayer.motionY -= 0.2;
                    }

                    if (mc.thePlayer.offGroundTicks == 6 && !disable && (PlayerUtils.blockRelativeToPlayer(0, mc.thePlayer.motionY * 3, 0) != Blocks.air && fallStrafe.get())) {
                        mc.thePlayer.motionY += 0.075;
                        MovementUtils.strafe();
                        double hypotenuse = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
                        if ((hypotenuse < MovementUtils.getAllowedHorizontalDistance() || mc.thePlayer.motionX == 0 || mc.thePlayer.motionZ == 0) && !disable && (!recentlyCollided && mc.thePlayer.isPotionActive(Potion.moveSpeed)) && !getModule(Scaffold.class).isEnabled()) {
                            MovementUtils.strafe(MovementUtils.getAllowedHorizontalDistance() - 0.01);
                            couldStrafe = true;

                        } else if (!disable && !getModule(Scaffold.class).isEnabled() && (hypotenuse < MovementUtils.getAllowedHorizontalDistance() || mc.thePlayer.motionX == 0 || mc.thePlayer.motionZ == 0)) {
                            MovementUtils.strafe(MovementUtils.getAllowedHorizontalDistance() - 0.05);
                            couldStrafe = true;
                        }
                    }

                    if (mc.thePlayer.offGroundTicks < 7 && (PlayerUtils.blockRelativeToPlayer(0, mc.thePlayer.motionY, 0) != Blocks.air) && mc.thePlayer.isPotionActive(Potion.moveSpeed) && !slab) {
                        boostTicks = mc.thePlayer.ticksExisted + 9;
                        recentlyCollided = true;
                    }

                    /*if (mc.thePlayer.offGroundTicks == 7 && !disable && (PlayerUtils.blockRelativeToPlayer(0, mc.thePlayer.motionY * 2, 0) != Blocks.air) && !getModule(Scaffold.class).isEnabled()) {
                        MovementUtils.strafe(fallStrafe.get() ? MovementUtils.getSpeed() : MovementUtils.getAllowedHorizontalDistance() * 1.1);
                        couldStrafe = true;
                    }*/


                    if (PlayerUtils.blockRelativeToPlayer(0, mc.thePlayer.motionY, 0) != Blocks.air && !disable && fallStrafe.get() && (mc.thePlayer.offGroundTicks > 7) && !disable3) {
                        MovementUtils.strafe(MovementUtils.getAllowedHorizontalDistance() * 1.079);
                        couldStrafe = true;
                        disable3 = true;
                    } else if (PlayerUtils.blockRelativeToPlayer(0, mc.thePlayer.motionY, 0) != Blocks.air && !disable && mc.thePlayer.offGroundTicks > 6 && !disable3) {
                        MovementUtils.strafe(MovementUtils.getAllowedHorizontalDistance());
                        couldStrafe = true;
                        disable3 = true;
                    } else if (PlayerUtils.blockRelativeToPlayer(0, mc.thePlayer.motionY, 0) != Blocks.air && mc.thePlayer.offGroundTicks > 5 && !disable3) {
                        MovementUtils.strafe(MovementUtils.getAllowedHorizontalDistance());
                        couldStrafe = true;
                        disable3 = true;
                    }


                    double speed2 = Math.hypot((mc.thePlayer.motionX - (mc.thePlayer.lastTickPosX - mc.thePlayer.lastLastTickPosX)), (mc.thePlayer.motionZ - (mc.thePlayer.lastTickPosZ - mc.thePlayer.lastLastTickPosZ)));
                    if (speed2 < .0125 && frictionOverride.get()) {
                        MovementUtils.strafe();
                        couldStrafe = true;
                    }

                    break;
            }

            if (fastFall.canDisplay() && fastFall.get() && isEnabled(Disabler.class) && getModule(Disabler.class).options.isEnabled("Watchdog Motion") && !getModule(Disabler.class).disabled) {
                switch (wdFastFallMode.get()) {
                    case "Test 5":
                        if (!disable) {
                            switch (mc.thePlayer.offGroundTicks) {
                                case 3:
                                    mc.thePlayer.motionY -= -0.0025;
                                    break;
                                case 4:
                                    mc.thePlayer.motionY -= 0.04;
                                    break;
                                case 5:
                                    mc.thePlayer.motionY -= 0.1905189780583944;
                                    break;
                                case 6:
                                    mc.thePlayer.motionX *= 1.001;
                                    mc.thePlayer.motionZ *= 1.001;
                                case 7:
                                    mc.thePlayer.motionY -= 0.004;
                                    break;
                                case 8:
                                    mc.thePlayer.motionY -= 0.01;
                            }
                        }
                        break;
                    case "Predict":
                        if (!disable) {
                            if (mc.thePlayer.offGroundTicks == predictTicks.get()) {
                                mc.thePlayer.motionY = MovementUtils.predictedMotionY(mc.thePlayer.motionY, 2);
                            }
                        }
                        break;
                    case "Predict 2":
                        if (!disable) {
                            if (mc.thePlayer.offGroundTicks == 5 || mc.thePlayer.offGroundTicks == 6) {
                                mc.thePlayer.motionY = MovementUtils.predictedMotionY(mc.thePlayer.motionY, 1);
                            }
                        }
                        break;
                }
            }
        }
    }

    @EventTarget
    public void onPostStrafe(PostStrafeEvent event) {
        if (mode.is("Watchdog") && wdMode.is("Full Strafe")) {
            if (extraStrafe.get()) {
                double attempt_angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(MovementUtils.getDirection()));
                double movement_angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90);
                if (MathUtils.wrappedDifference(attempt_angle, movement_angle) > 90) {
                    MovementUtils.strafe(MovementUtils.getSpeed(), (float) movement_angle - 180);
                }
            }
        }
    }

    @EventTarget
    public void onMove(MoveEvent event){

        if (!liquidCheck.get() || (!mc.thePlayer.isInWater() && !mc.thePlayer.isInLava())) {
            guiCheck.get();
        }

    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            ticksSinceTeleport = 0;
            if (lagBackCheck.get()) {
                toggle();
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event){
        if(!mode.is("EntityCollide"))
            if(mc.thePlayer.onGround)
                event.setJumping(false);
    }

    private boolean canCauseSpeed(Entity entity) {
        return entity != mc.thePlayer && entity instanceof EntityLivingBase && !(entity instanceof EntityArmorStand);
    }
}
