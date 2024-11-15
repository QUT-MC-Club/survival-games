package supercoder79.survivalgames.game.map.noise;

import com.mojang.serialization.Codec;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.Identifier;

public final class NoiseGenerators {
	public static void init() {
		register("default", DefaultNoiseGenerator.CODEC);
		register("island", IslandNoiseGenerator.CODEC);
	}

	public static void register(String name, MapCodec<? extends NoiseGenerator> generator) {
		NoiseGenerator.REGISTRY.register(Identifier.of("survivalgames", name), generator);
	}
}
