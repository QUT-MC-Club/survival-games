package supercoder79.survivalgames.game.map.biome.generator;

import com.mojang.serialization.Codec;

import com.mojang.serialization.MapCodec;
import supercoder79.survivalgames.game.map.biome.AspenForestGen;
import supercoder79.survivalgames.game.map.biome.BadlandsGen;
import supercoder79.survivalgames.game.map.biome.BiomeGen;
import supercoder79.survivalgames.game.map.biome.DesertGen;
import supercoder79.survivalgames.game.map.biome.JungleGen;
import supercoder79.survivalgames.game.map.biome.JungleHillsGen;
import supercoder79.survivalgames.game.map.biome.MesaPlateauGen;
import supercoder79.survivalgames.game.map.biome.SavannaGen;
import supercoder79.survivalgames.game.map.biome.ShatteredSavannaGen;

public class TropicalBiomeGenerator implements BiomeGenerator {
    public static final MapCodec<TropicalBiomeGenerator> CODEC = MapCodec.unit(new TropicalBiomeGenerator());

	@Override
	public BiomeGen getBiome(double temperature, double rainfall) {
		if (temperature < 0.35) {
			if (rainfall > 0.5) {
				return ShatteredSavannaGen.INSTANCE;
			}

			return SavannaGen.INSTANCE;
		}

		if (rainfall < 0.3) {
			if (temperature > 0.7 || rainfall < 0.2) {
				return DesertGen.INSTANCE;
			}

			if (temperature > 0.4) {
				return MesaPlateauGen.INSTANCE;
			}

			return BadlandsGen.INSTANCE;
		}

		if (rainfall > 0.7) {
			if (temperature < 0.5) {
				return AspenForestGen.INSTANCE;
			}

			return JungleHillsGen.INSTANCE;
		}

		return JungleGen.INSTANCE;
	}

	@Override
	public MapCodec<? extends BiomeGenerator> getCodec() {
		return CODEC;
	}
}
