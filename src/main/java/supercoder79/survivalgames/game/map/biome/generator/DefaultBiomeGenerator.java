package supercoder79.survivalgames.game.map.biome.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import supercoder79.survivalgames.game.map.biome.*;

public class DefaultBiomeGenerator implements BiomeGenerator {
	public static final MapCodec<DefaultBiomeGenerator> CODEC = MapCodec.unit(new DefaultBiomeGenerator());

	@Override
	public BiomeGen getBiome(double temperature, double rainfall) {
		if (temperature < 0.35) {
			if (rainfall > 0.4) {
				return TaigaGen.INSTANCE;
			}

			return MountainGen.INSTANCE;
		}

		if (rainfall < 0.35) {
			if (temperature > 0.7 || rainfall < 0.1) {
				return DesertGen.INSTANCE;
			}

			if (temperature > 0.55) {
				return DeadlandGen.INSTANCE;
			}

			return PlainsGen.INSTANCE;
		}

		if (rainfall > 0.7) {
			if (temperature > 0.5) {
				return RoofedForestGen.INSTANCE;
			}

			return AspenForestGen.INSTANCE;
		}

		return PoplarForestGen.INSTANCE;
	}

	@Override
	public MapCodec<? extends BiomeGenerator> getCodec() {
		return CODEC;
	}
}
