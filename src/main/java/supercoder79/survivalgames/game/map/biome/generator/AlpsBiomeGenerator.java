package supercoder79.survivalgames.game.map.biome.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import supercoder79.survivalgames.game.map.biome.alpine.AlpineCliffsGen;
import supercoder79.survivalgames.game.map.biome.alpine.AlpineSlopedForestGen;
import supercoder79.survivalgames.game.map.biome.alpine.AlpsGen;
import supercoder79.survivalgames.game.map.biome.BiomeGen;

import java.util.Map;

public class AlpsBiomeGenerator implements BiomeGenerator {
	public static final MapCodec<AlpsBiomeGenerator> CODEC = MapCodec.unit(new AlpsBiomeGenerator());

	@Override
	public BiomeGen getBiome(double temperature, double rainfall) {
		if (temperature < 0.65) {
			return temperature < 0.24 ? AlpsGen.INSTANCE : AlpineCliffsGen.INSTANCE;
		}
		return AlpineSlopedForestGen.INSTANCE;
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
