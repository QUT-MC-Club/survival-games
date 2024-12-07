package supercoder79.survivalgames.game;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.logging.log4j.core.jmx.Server;
import supercoder79.survivalgames.game.config.SurvivalGamesConfig;
import xyz.nucleoid.plasmid.api.game.GameSpace;

import java.util.Set;

public final class SurvivalGamesSpawnLogic {
    private final GameSpace world;
    private final SurvivalGamesConfig config;

    public SurvivalGamesSpawnLogic(GameSpace world, SurvivalGamesConfig config) {
        this.world = world;
        this.config = config;
    }

    public void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.getInventory().clear();
        player.getEnderChestInventory().clear();
        player.clearStatusEffects();
        player.setHealth(20.0F);
        player.getHungerManager().setFoodLevel(20);
        player.fallDistance = 0.0F;
        player.changeGameMode(gameMode);
        player.setExperienceLevel(0);
        player.setExperiencePoints(0);
    }

    public void spawnPlayerAtCenter(ServerPlayerEntity player, ServerWorld world) {
        this.spawnPlayerAt(player, 0, 0, world);
    }

    public void spawnPlayerAt(ServerPlayerEntity player, int x, int z, ServerWorld world) {

        ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);
        WorldChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
        BlockPos pos = new BlockPos(x, chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) + 1, z);

        if (!chunk.getBlockState(pos.down()).getFluidState().isEmpty()) {
            boolean found = false;

            // Try 20 times to spiral outwards, hopefully not hitting fluid
            for (int i = 0; i < 20; i++) {
                if (found) {
                    break;
                }

                int dist = i * 8 + 16;

                int count = 4 + i;
                for (int j = 0; j < count; j++) {
                    double theta = ((double)j / count) * Math.PI * 2;
                    int ax = (int) (Math.cos(theta) * dist) + x;
                    int az = (int) (Math.sin(theta) * dist) + z;

                    ChunkPos circlePos = new ChunkPos(ax >> 4, az >> 4);
                    WorldChunk circleChunk = world.getChunk(circlePos.x, circlePos.z);
                    pos = new BlockPos(ax, circleChunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ax, az) + 1, az);

                    // Check the position at the circle
                    if (chunk.getBlockState(pos.down()).getFluidState().isEmpty()) {
                        found = true;
                        break;
                    }
                }
            }
        }

        player.teleport(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Set.of(), 0.0F, 0.0F, true);
    }
}
