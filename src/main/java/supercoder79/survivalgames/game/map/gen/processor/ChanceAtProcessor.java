package supercoder79.survivalgames.game.map.gen.processor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldView;

public class ChanceAtProcessor extends StructureProcessor {
	public static MapCodec<ChanceAtProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(p -> p.pos),
			BlockState.CODEC.fieldOf("state").forGetter(p -> p.state),
			Codec.BOOL.fieldOf("match_terrain").orElse(false).forGetter(p -> p.matchTerrain),
			Codec.DOUBLE.fieldOf("chance").forGetter(p -> p.chance)
	).apply(instance, ChanceAtProcessor::new));

	private final BlockPos pos;
	private final boolean matchTerrain;
	private final BlockState state;
	private final double chance;

	public ChanceAtProcessor(BlockPos pos, BlockState state, boolean matchTerrain, double chance) {
		this.pos = pos;
		this.matchTerrain = matchTerrain;
		this.state = state;
		this.chance = chance;
	}

	@Override
	public StructureTemplate.StructureBlockInfo process(WorldView world, BlockPos worldPos, BlockPos localPos, StructureTemplate.StructureBlockInfo localInfo, StructureTemplate.StructureBlockInfo worldInfo, StructurePlacementData structurePlacementData) {
		if (structurePlacementData.getRandom(worldInfo.pos()).nextDouble() < this.chance) {
			if (localInfo.pos().asLong() == this.pos.asLong()) {

				BlockState state = this.state;
				if (state.contains(Properties.FACING)) {
					state = state.with(Properties.FACING, structurePlacementData.getRotation().rotate(state.get(Properties.FACING)));
				} else if (state.contains(Properties.HORIZONTAL_FACING)) {
					state = state.with(Properties.HORIZONTAL_FACING, structurePlacementData.getRotation().rotate(state.get(Properties.HORIZONTAL_FACING)));
				}

				if (this.matchTerrain) {
					int y = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, worldInfo.pos().getX(), worldInfo.pos().getZ());

					BlockPos topPos = new BlockPos(worldInfo.pos().getX(), y, worldInfo.pos().getZ());
					return new StructureTemplate.StructureBlockInfo(topPos, state, null);
				}

				return new StructureTemplate.StructureBlockInfo(worldInfo.pos(), state, null);
			}
		}

		return worldInfo;
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return SurvivalGamesProcessorTypes.CHANCE_AT;
	}
}
