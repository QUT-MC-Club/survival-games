package supercoder79.survivalgames.mixin;

import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import supercoder79.survivalgames.SurvivalGames;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.stimuli.event.EventResult;

@Mixin(MobSpawnerLogic.class)
public class MixinMobSpawnerLogic {
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private void disableInSG(ServerWorld world, BlockPos pos, CallbackInfo ci) {
        // Disable mob spawners as we handle their behavior
        var space = GameSpaceManager.get().byWorld(world);

        if (space != null && space.getBehavior().testRule(SurvivalGames.DISABLE_SPAWNERS) == EventResult.ALLOW) {
            ci.cancel();
        }
    }
}
