package wtf.moonlight.features.modules.impl.player;

import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.network.play.client.C03PacketPlayer;
import wtf.moonlight.events.annotations.EventTarget;
import wtf.moonlight.events.impl.player.UpdateEvent;
import wtf.moonlight.features.modules.Module;
import wtf.moonlight.features.modules.ModuleCategory;
import wtf.moonlight.features.modules.ModuleInfo;
import wtf.moonlight.features.values.impl.SliderValue;
import wtf.moonlight.utils.math.TimerUtils;

@ModuleInfo(name = "FastEat", category = ModuleCategory.Player)
public class FastEat extends Module {

    private final SliderValue delay = new SliderValue("Delay", 0, 0, 300, 1, this);

    private final TimerUtils timer = new TimerUtils();
    private boolean isTimerModified = false;

    @Override
    public void onDisable() {
        resetTimer();
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        resetTimer();

        if (!mc.thePlayer.isUsingItem()) {
            return;
        }

        Object usingItem = mc.thePlayer.getItemInUse().getItem();

        if (usingItem instanceof ItemFood || usingItem instanceof ItemBucketMilk || usingItem instanceof ItemPotion) {
            if (!timer.hasTimeElapsed((long) delay.get())) {
                return;
            }

            mc.getNetHandler().addToSendQueue(new C03PacketPlayer(mc.thePlayer.onGround));

            timer.reset();
        }
    }

    private void resetTimer() {
        if (isTimerModified) {
            mc.timer.timerSpeed = 1.0F;
            isTimerModified = false;
        }
    }
}