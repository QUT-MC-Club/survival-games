package supercoder79.survivalgames.game.config;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import supercoder79.survivalgames.game.map.biome.generator.BiomeGenerator;
import supercoder79.survivalgames.game.map.noise.NoiseGenerator;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import net.minecraft.world.dimension.DimensionType;

import net.minecraft.item.ItemStack;

public class SurvivalGamesConfig {
	public static final Codec<SurvivalGamesConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			WorldBorderConfig.CODEC.fieldOf("border").forGetter(config -> config.borderConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(config -> config.playerConfig),
			Codec.INT.fieldOf("town_depth").forGetter(config -> config.townDepth),
			Codec.INT.fieldOf("outskirts_building_count").forGetter(config -> config.outskirtsBuildingCount),
			BiomeGenerator.CODEC.fieldOf("biome_generator").forGetter(config -> config.biomeGenerator),
			NoiseGenerator.CODEC.fieldOf("noise_generator").forGetter(config -> config.noiseGenerator),
			ItemStack.CODEC.listOf().fieldOf("kit").forGetter(config -> config.kit),
			Identifier.CODEC.optionalFieldOf("dimension", DimensionType.OVERWORLD_ID).forGetter(config -> config.dimension),
			Identifier.CODEC.optionalFieldOf("outskirts_pool", new Identifier("survivalgames", "outskirts_buildings")).forGetter(config -> config.outskirtsPool),
			BlockState.CODEC.optionalFieldOf("default_state", Blocks.STONE.getDefaultState()).forGetter(config -> config.defaultState),
			BlockState.CODEC.optionalFieldOf("default_fluid", Blocks.WATER.getDefaultState()).forGetter(config -> config.defaultFluid)
	).apply(instance, SurvivalGamesConfig::new));
	public final WorldBorderConfig borderConfig;
	public final PlayerConfig playerConfig;
	public final int townDepth;
	public final int outskirtsBuildingCount;
	public final BiomeGenerator biomeGenerator;
	public final NoiseGenerator noiseGenerator;
	public final List<ItemStack> kit;
	public final Identifier dimension;
	public final Identifier outskirtsPool;
	public final BlockState defaultState;
	public final BlockState defaultFluid;

	public SurvivalGamesConfig(WorldBorderConfig borderConfig, PlayerConfig playerConfig, int townDepth, int outskirtsBuildingCount, BiomeGenerator biomeGenerator, NoiseGenerator noiseGenerator, List<ItemStack> kit, Identifier dimension, Identifier outskirtsPool, BlockState defaultState, BlockState defaultFluid) {
		this.borderConfig = borderConfig;
		this.playerConfig = playerConfig;
		this.townDepth = townDepth;
		this.outskirtsBuildingCount = outskirtsBuildingCount;
		this.biomeGenerator = biomeGenerator;
		this.noiseGenerator = noiseGenerator;
		this.kit = kit;
		this.dimension = dimension;
		this.outskirtsPool = outskirtsPool;
		this.defaultState = defaultState;
		this.defaultFluid = defaultFluid;
	}
}
