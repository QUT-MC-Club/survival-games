package supercoder79.survivalgames.game.map.gen.feature;

import java.util.Random;

import supercoder79.survivalgames.game.map.gen.MapGen;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.collection.WeightedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;

public class DiskGen implements MapGen {
	public static final DiskGen INSTANCE = new DiskGen();

	private static final WeightedList<BlockState> STATES = new WeightedList<BlockState>()
			.add(Blocks.SAND.getDefaultState(), 1)
			.add(Blocks.GRAVEL.getDefaultState(), 1);

	@Override
	public void generate(ServerWorldAccess world, BlockPos pos, Random random) {

		int radius = random.nextInt(5) + 2;
		int radiusSquared = radius * radius;

		BlockPos.Mutable mutable = new BlockPos.Mutable();
		BlockState state = STATES.pickRandom(random);

		for(int x = pos.getX() - radius; x <= pos.getX() + radius; ++x) {
			for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; ++z) {
				int localX = x - pos.getX();
				int localZ = z - pos.getZ();
				if (localX * localX + localZ * localZ <= radiusSquared) {
					for(int y = pos.getY() - 2; y <= pos.getY() + 2; ++y) {
						mutable.set(x, y, z);

						if (world.getBlockState(mutable).isOf(Blocks.DIRT) || world.getBlockState(mutable).isOf(Blocks.GRASS_BLOCK)) {
							world.setBlockState(mutable, state, 3);
						}
					}
				}
			}
		}
	}
}