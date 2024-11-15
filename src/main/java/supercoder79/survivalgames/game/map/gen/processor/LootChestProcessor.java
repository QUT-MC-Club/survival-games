package supercoder79.survivalgames.game.map.gen.processor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import supercoder79.survivalgames.game.map.loot.LootHelper;
import supercoder79.survivalgames.game.map.loot.LootProvider;
import supercoder79.survivalgames.game.map.loot.LootProviders;

public class LootChestProcessor extends StructureProcessor {
	public static MapCodec<LootChestProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(p -> p.chestPos),
			Codec.BOOL.fieldOf("match_terrain").orElse(false).forGetter(p -> p.matchTerrain),
			Codec.STRING.fieldOf("type").forGetter(p -> p.lootType)
	).apply(instance, LootChestProcessor::new));

	private final BlockPos chestPos;
	private final boolean matchTerrain;
	private final String lootType;

	public LootChestProcessor(BlockPos chestPos, boolean matchTerrain, String lootType) {
		this.chestPos = chestPos;
		this.matchTerrain = matchTerrain;
		this.lootType = lootType;
	}

	@Override
	public StructureTemplate.StructureBlockInfo process(WorldView world, BlockPos worldPos, BlockPos localPos, StructureTemplate.StructureBlockInfo localInfo, StructureTemplate.StructureBlockInfo worldInfo, StructurePlacementData structurePlacementData) {
		BlockPos pos = localInfo.pos();
		if (pos.asLong() == this.chestPos.asLong()) {
			// TODO: rotation

			if (this.matchTerrain) {
				int y = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, worldInfo.pos().getX(), worldInfo.pos().getZ());

				BlockPos topPos = new BlockPos(worldInfo.pos().getX(), y, worldInfo.pos().getZ());
				LootHelper.placeProviderChest((WorldAccess) world, topPos, getLootProvider(this.lootType));
			} else {
				LootHelper.placeProviderChest((WorldAccess) world, worldInfo.pos(), getLootProvider(this.lootType));
			}

			// Place air here
			return null;
		}


		return worldInfo;
	}

	// TODO: loot provider registry
	private static LootProvider getLootProvider(String name) {
		return switch (name) {
			case "house" -> LootProviders.HOUSE;
			case "tower" -> LootProviders.TOWER;
			case "enchanting_table" -> LootProviders.ENCHANTING_TABLE;
			case "ore_pile" -> LootProviders.ORE_PILE;
			default -> LootProviders.GENERIC;
		};
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return SurvivalGamesProcessorTypes.LOOT;
	}
}
