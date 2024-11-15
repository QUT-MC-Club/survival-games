package supercoder79.survivalgames.game.map.biome.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import supercoder79.survivalgames.game.map.biome.BiomeGen;
import supercoder79.survivalgames.game.map.biome.GlacierGen;
import supercoder79.survivalgames.game.map.biome.IceSpikesGen;
import supercoder79.survivalgames.game.map.biome.TaigaGen;
import supercoder79.survivalgames.game.map.biome.alpine.AlpineCliffsGen;
import supercoder79.survivalgames.game.map.biome.alpine.AlpineSlopedForestGen;
import supercoder79.survivalgames.game.map.biome.alpine.AlpsGen;

public class WinterlandBiomeGenerator implements BiomeGenerator {
	public static final MapCodec<WinterlandBiomeGenerator> CODEC = MapCodec.unit(new WinterlandBiomeGenerator());

	@Override
	public BiomeGen getBiome(double temperature, double rainfall) {
		if (temperature < 0.25) {
			return IceSpikesGen.INSTANCE;
		}
		if (temperature < 0.35) {
			return GlacierGen.INSTANCE;
		}
		if (temperature < 0.5) {
			return AlpsGen.INSTANCE;
		}
		if (temperature < 0.58) {
			return AlpineCliffsGen.INSTANCE;
		}
		if (temperature < 0.7) {
			return AlpineSlopedForestGen.INSTANCE;
		}
		return TaigaGen.INSTANCE;
	}

	@Override
	public boolean generateSnow() {
		return true;
	}

	@Override
	public MapCodec<? extends BiomeGenerator> getCodec() {
		return CODEC;
	}
}
