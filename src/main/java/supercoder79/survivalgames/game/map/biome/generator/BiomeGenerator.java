package supercoder79.survivalgames.game.map.biome.generator;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import supercoder79.survivalgames.game.map.biome.BiomeGen;
import xyz.nucleoid.plasmid.api.util.TinyRegistry;

public interface BiomeGenerator {
	TinyRegistry<MapCodec<? extends BiomeGenerator>> REGISTRY = TinyRegistry.create();
	MapCodec<BiomeGenerator> CODEC = REGISTRY.dispatchMap(BiomeGenerator::getCodec, Function.identity());

	BiomeGen getBiome(double temperature, double rainfall);

	default boolean generateSnow() {
		return false;
	}

	MapCodec<? extends BiomeGenerator> getCodec();
}
