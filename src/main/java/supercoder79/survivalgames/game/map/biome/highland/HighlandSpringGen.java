package supercoder79.survivalgames.game.map.biome.highland;

import kdotjpg.opensimplex.OpenSimplexNoise;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import supercoder79.survivalgames.game.map.biome.BiomeGen;
import supercoder79.survivalgames.game.map.gen.SpringGen;
import xyz.nucleoid.substrate.gen.MapGen;

import net.minecraft.util.math.random.Random;

public class HighlandSpringGen implements BiomeGen {
    public static final BiomeGen INSTANCE = new HighlandSpringGen();
    public static OpenSimplexNoise GRANITE_NOISE = new OpenSimplexNoise(25);
    public static OpenSimplexNoise GRASS_NOISE = new OpenSimplexNoise(26);

    @Override
    public RegistryKey<Biome> getFakingBiome() {
        return RegistryKey.of(RegistryKeys.BIOME, Identifier.of("survivalgames", "highland"));
    }

    @Override
    public double baseHeight() {
        return 15;
    }

    @Override
    public double upperNoiseFactor() {
        return 6;
    }

    @Override
    public double lowerNoiseFactor() {
        return 4;
    }

    @Override
    public double upperLerpHigh() {
        return 6;
    }

    @Override
    public double upperLerpLow() {
        return 6;
    }

    @Override
    public double lowerLerpHigh() {
        return 6;
    }

    @Override
    public double lowerLerpLow() {
        return 2;
    }

    @Override
    public double detailFactor() {
        return 2;
    }

    @Override
    public BlockState topState(Random random, int x, int z) {
        if (random.nextDouble() < GRANITE_NOISE.eval(x / 45.0, z / 45.0) + 0.1) {
            return Blocks.GRANITE.getDefaultState();
        } else if (random.nextDouble() < GRASS_NOISE.eval(x / 45.0, z / 45.0)) {
            return Blocks.GRASS_BLOCK.getDefaultState();
        }
        return random.nextDouble() < 0.5 ? Blocks.ANDESITE.getDefaultState() : Blocks.STONE.getDefaultState();
    }

    @Override
    public BlockState underState(Random random, int x, int z) {
        return BiomeGen.super.underState(random, x, z);
    }

    @Override
    public BlockState underWaterState(Random random, int x, int z) {
        return BiomeGen.super.underWaterState(random, x, z);
    }

    @Override
    public MapGen tree(int x, int z, Random random) {
        return SpringGen.INSTANCE;
    }

    @Override
    public double modifyTreeChance(double original) {
        return 16;
    }

    @Override
    public int grassChance(int x, int z, Random random) {
        return 64;
    }

    @Override
    public MapGen grass(int x, int z, Random random) {
        return BiomeGen.super.grass(x, z, random);
    }
}
