package supercoder79.survivalgames.game.map.biome.generator;

import com.mojang.serialization.Codec;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.Identifier;

public final class BiomeGenerators {
	public static void init() {
		register("default", DefaultBiomeGenerator.CODEC);
		register("winterland", WinterlandBiomeGenerator.CODEC);
		register("tropical", TropicalBiomeGenerator.CODEC);
		register("nether", NetherBiomeGenerator.CODEC);
		register("alps", AlpsBiomeGenerator.CODEC);
		register("highland", HighlandBiomeGenerator.CODEC);
	}

	public static void register(String name, MapCodec<? extends BiomeGenerator> generator) {
		BiomeGenerator.REGISTRY.register(Identifier.of("survivalgames", name), generator);
	}
}
