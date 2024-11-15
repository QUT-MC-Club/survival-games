package supercoder79.survivalgames.game;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import supercoder79.survivalgames.SurvivalGames;
import supercoder79.survivalgames.game.config.SurvivalGamesConfig;
import supercoder79.survivalgames.game.map.SurvivalGamesMap;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;

public final class SurvivalGamesWaiting {
	private final GameSpace world;
	private final SurvivalGamesMap map;
	private final SurvivalGamesConfig config;
	private final GenerationTracker tracker;

	private SurvivalGamesWaiting(GameSpace world, SurvivalGamesMap map, SurvivalGamesConfig config, GenerationTracker tracker) {
		this.world = world;
		this.map = map;
		this.config = config;
		this.tracker = tracker;
	}

	public static GameOpenProcedure open(GameOpenContext<SurvivalGamesConfig> context) {
		GenerationTracker tracker = new GenerationTracker();
		SurvivalGamesMap map = new SurvivalGamesMap();
		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
				.setTimeOfDay(context.config().time)
				.setGenerator(map.chunkGenerator(context.server(), context.config(), tracker))
				//.setSpawner(BubbleWorldSpawner.atSurface(0, 0))
				.setDimensionType(RegistryKey.of(RegistryKeys.DIMENSION_TYPE, context.config().dimension));


		return context.openWithWorld(worldConfig, (game, world) -> {
			SurvivalGamesWaiting waiting = new SurvivalGamesWaiting(game.getGameSpace(), map, context.config(), tracker);
			GameWaitingLobby.addTo(game, context.config().playerConfig);
			var height = world.getChunk(0, 0).getHeightmap(Heightmap.Type.MOTION_BLOCKING).get(0, 0);

			game.allow(SurvivalGames.DISABLE_SPAWNERS);
			game.listen(GameActivityEvents.REQUEST_START, () -> waiting.requestStart(world));
			game.listen(GamePlayerEvents.REMOVE, waiting::onPlayerDeath);
			game.listen(GamePlayerEvents.ACCEPT, offer -> offer.teleport(world, new Vec3d(0, height, 0)));
		});
	}

	private GameResult requestStart(ServerWorld world) {
		SurvivalGamesActive.open(this.world, this.map, this.config, world, this.tracker);
		return GameResult.ok();
	}

	private void onPlayerDeath(ServerPlayerEntity player) {
	}
}
