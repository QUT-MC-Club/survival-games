package supercoder79.survivalgames.game.map.biome.highland;

import kdotjpg.opensimplex.OpenSimplexNoise;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import supercoder79.survivalgames.game.map.biome.BiomeGen;
import supercoder79.survivalgames.game.map.gen.ThistleGen;
import xyz.nucleoid.substrate.gen.MapGen;
import xyz.nucleoid.substrate.gen.ShrubGen;

import net.minecraft.util.math.random.Random;

public class HighlandHillsGen implements BiomeGen {
    public static final BiomeGen INSTANCE = new HighlandHillsGen();
    public final OpenSimplexNoise STONE_NOISE = new OpenSimplexNoise(22);

    @Override
    public RegistryKey<Biome> getFakingBiome() {
        return RegistryKey.of(RegistryKeys.BIOME, Identifier.of("survivalgames", "highland"));
    }

    @Override
    public double baseHeight() {
        return 32;
    }

    @Override
    public double upperNoiseFactor() {
        return 6;
    }

    @Override
    public double lowerNoiseFactor() {
        return 6;
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
        return 4;
    }

    @Override
    public double lowerLerpLow() {
        return 4;
    }

    @Override
    public double detailFactor() {
        return 2;
    }

    @Override
    public BlockState topState(Random random, int x, int z) {
        if (random.nextDouble() < STONE_NOISE.eval(x / 45.0, z / 45.0)/1.8) {
            return Blocks.STONE.getDefaultState();
        }
        return BiomeGen.super.topState(random, x, z);
    }

    @Override
    public BlockState underState(Random random, int x, int z) {
        return Blocks.DIRT.getDefaultState();
    }

    @Override
    public MapGen tree(int x, int z, Random random) {
        if (random.nextDouble() < 0.8) return ShrubGen.INSTANCE;
        return BiomeGen.super.tree(x, z, random);
    }

    @Override
    public double modifyTreeChance(double original) {
        return 6;
    }

    @Override
    public int grassChance(int x, int z, Random random) {
        return 6;
    }

    @Override
    public MapGen grass(int x, int z, Random random) {
        return ThistleGen.INSTANCE;
    }
}
