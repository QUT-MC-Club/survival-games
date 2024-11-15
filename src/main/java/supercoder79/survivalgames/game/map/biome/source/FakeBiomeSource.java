package supercoder79.survivalgames.game.map.biome.source;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gegy.noise.sampler.NoiseSampler2d;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import supercoder79.survivalgames.noise.simplex.OpenSimplexNoise;
import supercoder79.survivalgames.SurvivalGames;
import supercoder79.survivalgames.game.map.biome.*;
import supercoder79.survivalgames.game.map.biome.generator.BiomeGenerator;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;

import java.util.stream.Stream;

public final class FakeBiomeSource extends BiomeSource {
	private final Registry<Biome> biomeRegistry;
	private final long seed;
	private final BiomeGenerator biomeGenerator;

	private final NoiseSampler2d temperatureNoise;
	private final NoiseSampler2d rainfallNoise;
	private final NoiseSampler2d roughnessNoise;

	public FakeBiomeSource(Registry<Biome> biomeRegistry, long seed, BiomeGenerator biomeGenerator) {
		super();
		this.biomeRegistry = biomeRegistry;
		this.seed = seed;
		this.biomeGenerator = biomeGenerator;

		temperatureNoise = compile(seed + 79, 240.0, 0.85);
		rainfallNoise = compile(seed - 79, 240.0, 0.85);
		roughnessNoise = compile(seed, 60.0, 0.15);
	}

	private static NoiseSampler2d compile(long seed, double scale, double amp) {
		return SurvivalGames.NOISE_COMPILER.compile(OpenSimplexNoise.create().scale(1 / scale, 1 / scale).add(1.0).div(2.0).mul(amp), NoiseSampler2d.TYPE).create(seed);
	}

	@Override
	protected MapCodec<? extends BiomeSource> getCodec() {
		return MapCodec.unit(this);
	}

	@Override
	protected Stream<RegistryEntry<Biome>> biomeStream() {
		return this.biomeRegistry.stream().map(x -> this.biomeRegistry.getEntry(x));
	}

	@Override
	public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
		return biomeRegistry.getOrThrow(getRealBiome(x << 2, z << 2).getFakingBiome());
	}

	public BiomeGen getRealBiome(int x, int z) {
		double roughness = this.roughnessNoise.get(x,  z);
		double temperature = this.temperatureNoise.get(x,  z) + roughness;
		double rainfall = this.rainfallNoise.get(x,  z) + roughness;

		return this.biomeGenerator.getBiome(temperature, rainfall);
	}
}
