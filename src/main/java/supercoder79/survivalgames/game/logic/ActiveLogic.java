package supercoder79.survivalgames.game.logic;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.ActionResult;
import xyz.nucleoid.stimuli.event.EventResult;

public interface ActiveLogic {
    void tick(long time);

    default EventResult onEntityDeath(Entity entity, DamageSource source) {
        return EventResult.PASS;
    }
}
