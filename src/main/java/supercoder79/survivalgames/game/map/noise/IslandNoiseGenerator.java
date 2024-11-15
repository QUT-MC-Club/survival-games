package supercoder79.survivalgames.game.map.noise;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.random.Random;

import com.mojang.serialization.Codec;
import dev.gegy.noise.sampler.NoiseSampler2d;
import supercoder79.survivalgames.SurvivalGames;
import supercoder79.survivalgames.game.config.SurvivalGamesConfig;
import supercoder79.survivalgames.game.map.biome.BiomeGen;
import supercoder79.survivalgames.game.map.biome.source.FakeBiomeSource;
import supercoder79.survivalgames.noise.simplex.OpenSimplexNoise;

import net.minecraft.util.math.MathHelper;

public class IslandNoiseGenerator implements NoiseGenerator {
	public static final MapCodec<IslandNoiseGenerator> CODEC = MapCodec.unit(new IslandNoiseGenerator());
	private NoiseSampler2d baseNoise;
	private NoiseSampler2d interpolationNoise;
	private NoiseSampler2d lowerInterpolatedNoise;
	private NoiseSampler2d upperInterpolatedNoise;
	private NoiseSampler2d detailNoise;

	private double radius;

	@Override
	public void initialize(Random random, SurvivalGamesConfig config) {
		this.baseNoise = compile(random, 256.0);
		this.interpolationNoise = compile(random, 50.0);
		this.lowerInterpolatedNoise = compile(random,  60.0);
		this.upperInterpolatedNoise = compile(random,  60.0);
		this.detailNoise = compile(random, 20.0);
		this.radius = (config.borderConfig.startSize / 2.0) * 0.75;
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
		double weight = 0;

		for (int aX = -4; aX <= 4; aX++) {
			for (int aZ = -4; aZ <= 4; aZ++) {
				BiomeGen biome = biomeSource.getRealBiome(x + aX, z + aZ);
				baseHeight += biome.baseHeight();
				upperNoiseFactor += biome.upperNoiseFactor();
				lowerNoiseFactor += biome.lowerNoiseFactor();
				upperLerpHigh += biome.upperLerpHigh();
				upperLerpLow += biome.upperLerpLow();
				lowerLerpHigh += biome.lowerLerpHigh();
				lowerLerpLow += biome.lowerLerpLow();
				detailFactor += biome.detailFactor();

				weight++;
			}
		}

		baseHeight /= weight;
		upperNoiseFactor /= weight;
		lowerNoiseFactor /= weight;
		upperLerpHigh /= weight;
		upperLerpLow /= weight;
		lowerLerpHigh /= weight;
		lowerLerpLow /= weight;
		detailFactor /= weight;

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

		double ax = x / this.radius;
		double az = z / this.radius;
		double rad = ax * ax + az * az;

		noise = MathHelper.clampedLerp(noise, -20, rad / 2.0);

		return noise;
	}

	@Override
	public double maxSpawnDistFactor() {
		return 0.70;
	}

	@Override
	public double minSpawnDistFactor() {
		return 0.60;
	}

	@Override
	public boolean shouldOutskirtsSpawn(int x, int z) {
		double ax = x / this.radius;
		double az = z / this.radius;
		double rad = ax * ax + az * az;

		return rad <= 1.0;
	}

	@Override
	public MapCodec<? extends NoiseGenerator> getCodec() {
		return CODEC;
	}
}
