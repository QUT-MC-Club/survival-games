package supercoder79.survivalgames.game.map.noise;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.random.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gegy.noise.sampler.NoiseSampler2d;
import net.minecraft.world.biome.Biome;
import supercoder79.survivalgames.SurvivalGames;
import supercoder79.survivalgames.game.config.SurvivalGamesConfig;
import supercoder79.survivalgames.game.map.biome.BiomeGen;
import supercoder79.survivalgames.game.map.biome.blend.CachingBlender;
import supercoder79.survivalgames.game.map.biome.blend.LinkedBiomeWeightMap;
import supercoder79.survivalgames.game.map.biome.source.FakeBiomeSource;
import net.minecraft.util.math.MathHelper;
import supercoder79.survivalgames.noise.simplex.OpenSimplexNoise;

public class DefaultNoiseGenerator implements NoiseGenerator {
	public static final MapCodec<DefaultNoiseGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
	        Codec.BOOL.optionalFieldOf("rivers", true).forGetter(c -> c.rivers)
	).apply(instance, DefaultNoiseGenerator::new));
	private final boolean rivers;

	private final CachingBlender blender = new CachingBlender(0.04, 24, 16);
	private NoiseSampler2d baseNoise;
	private NoiseSampler2d interpolationNoise;
	private NoiseSampler2d lowerInterpolatedNoise;
	private NoiseSampler2d upperInterpolatedNoise;
	private NoiseSampler2d detailNoise;

	private NoiseSampler2d riverNoise;
	private NoiseSampler2d riverNoise2;
	private NoiseSampler2d riverDepthNoise;
	private long seed;

	public DefaultNoiseGenerator(boolean rivers) {
		this.rivers = rivers;
	}

	@Override
	public void initialize(Random random, SurvivalGamesConfig config) {
		this.seed = random.nextLong();
		this.baseNoise = compile(random, 256.0);
		this.interpolationNoise = compile(random, 50.0);
		this.lowerInterpolatedNoise = compile(random,  60.0);
		this.upperInterpolatedNoise = compile(random,  60.0);
		this.detailNoise = compile(random, 20.0);

		this.riverNoise = compile(random, 160.0);
		this.riverNoise2 = compile(random, 32.0); // TODO: octave sampler
		this.riverDepthNoise = compile(random, 60.0);
	}

	public static NoiseSampler2d compile(Random random, double scale) {
		return SurvivalGames.NOISE_COMPILER.compile(OpenSimplexNoise.create().scale(1 / scale, 1 / scale), NoiseSampler2d.TYPE).create(random.nextLong());
	}

	@Override
	public double getHeightAt(FakeBiomeSource biomeSource, int x, int z) {
		double baseHeight = 0;
		double upperNoiseFactor = 0;
		double lowerNoiseFactor = 0;
		double upperLerpHigh = 0;
		double upperLerpLow = 0;
		double lowerLerpHigh = 0;
		double lowerLerpLow = 0;
		double detailFactor = 0;

		LinkedBiomeWeightMap weights = this.blender.blend(this.seed, (x >> 4) << 4, (z >> 4) << 4, (x0, z0) -> biomeSource.getRealBiome((int) x0, (int) z0));

		int idx = ((z & 15) * 16) + (x & 15);

		for (LinkedBiomeWeightMap entry = weights; entry != null; entry = entry.getNext()) {
			double weight = entry.getWeights()[idx];
			BiomeGen biome = entry.getBiome();

			baseHeight += biome.baseHeight() * weight;
			upperNoiseFactor += biome.upperNoiseFactor() * weight;
			lowerNoiseFactor += biome.lowerNoiseFactor() * weight;
			upperLerpHigh += biome.upperLerpHigh() * weight;
			upperLerpLow += biome.upperLerpLow() * weight;
			lowerLerpHigh += biome.lowerLerpHigh() * weight;
			lowerLerpLow += biome.lowerLerpLow() * weight;
			detailFactor += biome.detailFactor() * weight;
		}

		// Create base terrain
		double noise = baseNoise.get(x, z);
		noise *= noise > 0 ? upperNoiseFactor : lowerNoiseFactor;

		// Add hills in a similar method to mc interpolation noise
		double lerp = interpolationNoise.get(x, z) * 2.5;
		if (lerp > 1) {
			double upperNoise = upperInterpolatedNoise.get(x, z);
			upperNoise *= upperNoise > 0 ? upperLerpHigh : upperLerpLow;
			noise += upperNoise;
		} else if (lerp < 0) {
			double lowerNoise = lowerInterpolatedNoise.get(x, z);
			lowerNoise *= lowerNoise > 0 ? lowerLerpHigh : lowerLerpLow;
			noise += lowerNoise;
		} else {
			double upperNoise = upperInterpolatedNoise.get(x, z);
			upperNoise *= upperNoise > 0 ? upperLerpHigh : upperLerpLow;

			double lowerNoise = lowerInterpolatedNoise.get(x, z);
			lowerNoise *= lowerNoise > 0 ? lowerLerpHigh : lowerLerpLow;

			noise += MathHelper.lerp(lerp, lowerNoise, upperNoise);
		}

		noise += baseHeight;

		// Add small details to make the terrain less rounded
		noise += detailNoise.get(x, z) * detailFactor;

		if (this.rivers) {
			// River gen
			double river = this.riverNoise.get(x, z) + this.riverNoise2.get(x, z) * 0.2;
			if (river > -0.24 && river < 0.24) {
				double depth = -10 + this.riverDepthNoise.get(x, z) * 1.75;

				noise = MathHelper.lerp(smoothstep(river / 0.24), noise, depth);
			}
		}

		return noise;
	}

	private static double smoothstep(double x) {
		return (1 - x * x) * (1 - x * x) * (1 - x * x);
	}

	@Override
	public MapCodec<? extends NoiseGenerator> getCodec() {
		return CODEC;
	}
}
