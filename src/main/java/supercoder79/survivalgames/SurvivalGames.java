package supercoder79.survivalgames;

import dev.gegy.noise.compile.NoiseCompiler;
import supercoder79.survivalgames.game.SurvivalGamesWaiting;
import supercoder79.survivalgames.game.map.biome.generator.BiomeGenerators;
import supercoder79.survivalgames.game.map.gen.processor.SurvivalGamesProcessorTypes;
import xyz.nucleoid.plasmid.api.game.GameType;
import supercoder79.survivalgames.game.config.SurvivalGamesConfig;
import supercoder79.survivalgames.game.map.noise.NoiseGenerators;

import net.minecraft.util.Identifier;

import net.fabricmc.api.ModInitializer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;

public class SurvivalGames implements ModInitializer {
	public static final GameRuleType DISABLE_SPAWNERS = GameRuleType.create();
	public static final NoiseCompiler NOISE_COMPILER = NoiseCompiler.create(SurvivalGames.class.getClassLoader());

	@Override
	public void onInitialize() {
		SurvivalGamesProcessorTypes.init();
		BiomeGenerators.init();
		NoiseGenerators.init();

		GameType.register(
				Identifier.of("survivalgames", "survivalgames"),
				SurvivalGamesConfig.CODEC,
				SurvivalGamesWaiting::open
		);
	}
}
