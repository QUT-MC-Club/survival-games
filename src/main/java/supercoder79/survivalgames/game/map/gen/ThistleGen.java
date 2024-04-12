package supercoder79.survivalgames.game.map.gen;

import kdotjpg.opensimplex.OpenSimplexNoise;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import xyz.nucleoid.substrate.gen.MapGen;

import net.minecraft.util.math.random.Random;

public class ThistleGen implements MapGen {
    public static final MapGen INSTANCE = new ThistleGen();
    public static final OpenSimplexNoise ALLIUM_NOISE = new OpenSimplexNoise();
    public static final OpenSimplexNoise LILY_NOISE = new OpenSimplexNoise();

    @Override
    public void generate(ServerWorldAccess world, BlockPos pos, Random random) {
        BlockState state = random.nextDouble() < 0.1 ? getFlower(pos) : Blocks.SHORT_GRASS.getDefaultState();
        boolean grassBelow = world.getBlockState(pos.mutableCopy().down()).getBlock().equals(Blocks.GRASS_BLOCK);
        if (!world.getBlockState(pos).getBlock().equals(Blocks.AIR)) return;
        if (grassBelow) {
            world.setBlockState(pos, state, 0);
        } else {
            if (random.nextDouble() < 0.5 && state.getBlock().equals(Blocks.SHORT_GRASS)) world.setBlockState(pos, state, 0);
        }
    }

    public BlockState getFlower(BlockPos pos) {
        if (ALLIUM_NOISE.eval(pos.getX() / 64.0, pos.getY() / 64.0) < 0.4) {
            return Blocks.ALLIUM.getDefaultState();
        } else if (LILY_NOISE.eval(pos.getX() / 64.0, pos.getY() / 64.0) < 0.3) {
            return Blocks.LILY_OF_THE_VALLEY.getDefaultState();
        }
        return Blocks.ORANGE_TULIP.getDefaultState();
    }
}
