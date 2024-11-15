package supercoder79.survivalgames.game.map;

import com.google.common.collect.ImmutableList;
import dev.gegy.noise.sampler.NoiseSampler2d;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.noise.NoiseConfig;
import supercoder79.survivalgames.SurvivalGames;
import supercoder79.survivalgames.game.GameTrackable;
import supercoder79.survivalgames.game.GenerationTracker;
import supercoder79.survivalgames.game.config.SurvivalGamesConfig;
import supercoder79.survivalgames.game.map.biome.BiomeGen;
import supercoder79.survivalgames.game.map.biome.generator.BiomeGenerator;
import supercoder79.survivalgames.game.map.biome.source.FakeBiomeSource;
import supercoder79.survivalgames.game.map.gen.structure.ChunkBox;
import supercoder79.survivalgames.game.map.gen.structure.ChunkMask;
import supercoder79.survivalgames.game.map.gen.structure.StructureGen;
import supercoder79.survivalgames.game.map.gen.structure.Structures;
import supercoder79.survivalgames.game.map.loot.LootHelper;
import supercoder79.survivalgames.game.map.loot.LootProviders;
import supercoder79.survivalgames.game.map.noise.NoiseGenerator;
import supercoder79.survivalgames.noise.WorleyNoise;
import supercoder79.survivalgames.noise.simplex.OpenSimplexNoise;
import xyz.nucleoid.substrate.gen.DiskGen;
import xyz.nucleoid.plasmid.api.game.world.generator.GameChunkGenerator;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.random.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SurvivalGamesChunkGenerator extends GameChunkGenerator {


	private final NoiseSampler2d treeDensityNoise;

	private final WorleyNoise structureNoise;
	private final WorleyNoise chestNoise;

	private final FakeBiomeSource biomeSource;
	private final BiomeGenerator biomeGenerator;
	private final NoiseGenerator noiseGenerator;

	private final Long2ObjectMap<List<PoolStructurePiece>> piecesByChunk;
	private final List<SurvivalGamesJigsawGenerator> jigsawGenerator;

	private final BlockState defaultState;
	private final BlockState defaultFluid;
	private final GenerationTracker tracker;

	public SurvivalGamesChunkGenerator(MinecraftServer server, SurvivalGamesConfig config, GenerationTracker tracker) {
		super(new FakeBiomeSource(server.getRegistryManager().getOrThrow(RegistryKeys.BIOME), Random.create().nextLong(), config.biomeGenerator));
		Random random = Random.create();
		this.tracker = tracker;

		this.biomeGenerator = config.biomeGenerator;
		this.noiseGenerator = config.noiseGenerator;
		this.biomeSource = (FakeBiomeSource) this.getBiomeSource();

		this.treeDensityNoise = compile(random, 180.0);

		this.structureNoise = new WorleyNoise(random.nextLong());
		this.chestNoise = new WorleyNoise(random.nextLong());

		this.piecesByChunk = new Long2ObjectOpenHashMap<>();
		this.piecesByChunk.defaultReturnValue(ImmutableList.of());

		List<SurvivalGamesJigsawGenerator> generators = new ArrayList<>();

		this.noiseGenerator.initialize(random, config);

		this.defaultState = config.defaultState;
		this.defaultFluid = config.defaultFluid;

		ChunkMask mask = new ChunkMask();
		ChunkBox townArea = new ChunkBox();

		if (config.townDepth > 0) {
			SurvivalGamesJigsawGenerator generator = new SurvivalGamesJigsawGenerator(server, this, piecesByChunk);
			generator.arrangePieces(new BlockPos(0, 64, 0), Identifier.of("survivalgames", "starts"), config.townDepth);
			townArea = generator.getBox();
			generators.add(generator);
			mask.and(townArea);
		}

		for (int i = 0; i < config.outskirtsBuildingCount; i++) {
			int startX = random.nextInt(config.borderConfig.startSize / 2) - random.nextInt(config.borderConfig.startSize / 2);
			int startZ = random.nextInt(config.borderConfig.startSize / 2) - random.nextInt(config.borderConfig.startSize / 2);

			if (!this.noiseGenerator.shouldOutskirtsSpawn(startX, startZ)) {
				continue;
			}

			BlockPos start = new BlockPos(startX, 0, startZ);
			ChunkPos chunkPos = new ChunkPos(start);

			if (mask.isIn(chunkPos)) {
				continue;
			}

			SurvivalGamesJigsawGenerator outskirtGenerator = new SurvivalGamesJigsawGenerator(server, this, piecesByChunk);
			outskirtGenerator.arrangePieces(start, config.outskirtsPool, 0);

			mask.and(chunkPos);

			generators.add(outskirtGenerator);
		}

		this.jigsawGenerator = generators;

		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			DebugJigsawMapper.map(config, this.piecesByChunk, townArea, mask);
		}
	}

	public static NoiseSampler2d compile(Random random, double scale) {
		return SurvivalGames.NOISE_COMPILER.compile(OpenSimplexNoise.create().scale(1 / scale, 1 / scale), NoiseSampler2d.TYPE).create(random.nextLong());
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
		populateNoise(structureAccessor, chunk);
		return CompletableFuture.completedFuture(chunk);
	}

	public void populateNoise(StructureAccessor structures, Chunk chunk) {
		int chunkX = chunk.getPos().x * 16;
		int chunkZ = chunk.getPos().z * 16;

		// To smooth into other chunks, we need to gather all of the nearby structures
		Set<PoolStructurePiece> pieces = new ObjectOpenHashSet<>();
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				pieces.addAll(this.piecesByChunk.get(new ChunkPos(chunk.getPos().x + x, chunk.getPos().z + z).toLong()));
			}
		}

		BlockPos.Mutable mutable = new BlockPos.Mutable();
		Random random = Random.create();

		for (int x = chunkX; x < chunkX + 16; x++) {
			for (int z = chunkZ; z < chunkZ + 16; z++) {
				double noise = getNoise(x, z);
				BiomeGen biome = this.biomeSource.getRealBiome(x, z);

				int height = (int) (56 + noise);

				// TODO: this is really slow and bad
				for (PoolStructurePiece piece : pieces) {
					BlockBox box = piece.getBoundingBox();
					if (piece.getPoolElement().getProjection() == StructurePool.Projection.RIGID) {
						// At structure: raise to level
						if (box.intersectsXZ(x, z, x, z)) {
							height = Math.max(48, piece.getPos().getY());
						} else if (box.intersectsXZ(x - 8, z - 8, x + 8, z + 8)) {
							// Within radius: smooth
							// I won't lie, I have no idea what I just wrote here.
							// It seems to work though so... it stays.

							double dx = Math.max(0, Math.max(box.getMinX() - x, x - box.getMaxX())) / 8.0;
							double dz = Math.max(0, Math.max(box.getMinZ() - z, z - box.getMaxZ())) / 8.0;
							double rad = dx * dx + dz * dz;

							double falloff = rad >= 1 ? 0 : (1 - rad) * (1 - rad);

							height = (int) MathHelper.lerp(falloff, 56 + noise, piece.getPos().getY());
						}
					}
				}

				// Generation height ensures that the generator iterates up to at least the water level.
				int genHeight = Math.max(height, 48);
				for (int y = 0; y <= genHeight; y++) {
					// Simple surface building
					BlockState state = this.defaultState;
					if (y == height) {
						// If the height and the generation height are the same, it means that we're on land
						if (height == genHeight) {
							state = biome.topState(random, x, z);
						} else {
							// height and genHeight are different, so we're under water. Place dirt instead of grass.
							state = biome.underState(random, x, z);
						}
					} else if ((height - y) <= 3) { //TODO: biome controls under depth
						state = biome.underState(random, x, z);
					} else if (y == 0) {
						state = Blocks.BEDROCK.getDefaultState();
					}

					// If the y is higher than the land height, then we must place water
					if (y > height) {
						state = defaultFluid;
					}

					// Set the state here
					chunk.setBlockState(mutable.set(x, y, z), state, false);
				}
			}
		}
	}

	private double getNoise(int x, int z) {
		return this.noiseGenerator.getHeightAt(this.biomeSource, x, z);
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
		int height = (int) (56 + getNoise(x, z));
		return Math.max(height, 50);
	}

	public void generateJigsaws(StructureWorldAccess region, Chunk chunk, StructureAccessor structures) {
		ChunkPos chunkPos = chunk.getPos();
		List<PoolStructurePiece> pieces = this.piecesByChunk.get(chunkPos.toLong());

		if (pieces != null) {
			// generate all intersecting pieces with the mask of this chunk
			BlockBox chunkMask = new BlockBox(chunkPos.getStartX(), 0, chunkPos.getStartZ(), chunkPos.getEndX(), 255, chunkPos.getEndZ());
			for (PoolStructurePiece piece : pieces) {
				piece.generate(region, structures, this, Random.create(), chunkMask, BlockPos.ORIGIN, false);
			}
		}
	}

	@Override
	public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structures) {
		generateJigsaws(world, chunk, structures);

		int chunkX = chunk.getPos().x * 16;
		int chunkZ = chunk.getPos().z * 16;
		Random random = Random.create();

		BlockPos.Mutable mutable = new BlockPos.Mutable();
		boolean spawnedStructure = false;

		for (int x = chunkX; x < chunkX + 16; x++) {
			for (int z = chunkZ; z < chunkZ + 16; z++) {
				BiomeGen biome = this.biomeSource.getRealBiome(x, z);

				int y = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, x, z);

				if (y > 48) {
					if (this.chestNoise.sample(x / 45.0, z / 45.0) < 0.01) {
						LootHelper.placeProviderChest(world, mutable.set(x, y, z).toImmutable(), LootProviders.GENERIC);
					}

					if (!spawnedStructure && this.structureNoise.sample(x / 120.0, z / 120.0) < 0.005) {
						spawnedStructure = true;

						StructureGen structure = Structures.POOL.shuffle().stream().findFirst().get();
						structure.generate(world, mutable.set(x, y, z).toImmutable(), random);

						for (int i = 0; i < structure.nearbyChestCount(random); i++) {
							int ax = x + (random.nextInt(16) - random.nextInt(16));
							int az = z + (random.nextInt(16) - random.nextInt(16));
							int ay = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, ax, az);

							LootHelper.placeProviderChest(world, mutable.set(ax, ay, az).toImmutable(), structure.getLootProvider());
						}

						if (structure instanceof GameTrackable trackable) {
							trackable.getTracker().track(this.tracker, mutable.set(x, y, z).toImmutable());
						}
					}

					y = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, x, z);

					int treeDensity = (int) biome.modifyTreeChance((treeDensityNoise.get(x, z) + 1) * 64);

					if (random.nextInt(96 + treeDensity) == 0) {
						biome.tree(x, z, random).generate(world, mutable.set(x, y, z).toImmutable(), random);
					}

					if (random.nextInt(biome.grassChance(x, z, random)) == 0) {
						biome.grass(x, z, random).generate(world, mutable.set(x, y, z).toImmutable(), random);
					}
				} else {
					if (!this.biomeGenerator.generateSnow()) {
						if (random.nextInt(384) == 0) {
							generateBoats(world, random, new BlockPos(x, world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z), z));
						}
					}

					if (random.nextInt(64) == 0) {
						DiskGen.INSTANCE.generate(world, mutable.set(x, y, z).toImmutable(), random);
					}
				}
			}
		}

		if (this.biomeGenerator.generateSnow()) {
			mutable = new BlockPos.Mutable();
			for (int x = chunkX; x < chunkX + 16; x++) {
				for (int z = chunkZ; z < chunkZ + 16; z++) {
					int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
					mutable.set(x, y, z);

					if (world.isAir(mutable)) {
						BlockState down = world.getBlockState(mutable.down());
						if (down.isOpaqueFullCube() || down.isIn(BlockTags.LEAVES)) {
							world.setBlockState(mutable, Blocks.SNOW.getDefaultState(), 3);

							if (down.contains(Properties.SNOWY)) {
								world.setBlockState(mutable.down(), down.with(Properties.SNOWY, true), 3);
							}
						} else if (down.getFluidState().isIn(FluidTags.WATER)) {
							world.setBlockState(mutable.down(), Blocks.ICE.getDefaultState(), 3);
						}
					}
				}
			}
		}
	}

	private static void generateBoats(StructureWorldAccess world, Random random, BlockPos pos) {
		int count = 0;

		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				if (world.getBlockState(new BlockPos(pos.getX() + x, world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX() + x, pos.getZ() + z), pos.getZ() + z).down()).isOf(Blocks.WATER)) {
					count++;
				}
			}
		}

		if (count > 16 && count < 38) {
			for (int x = -3; x <= 3; x++) {
				for (int z = -3; z <= 3; z++) {
					if (x * x + z * z < 3 * 3 + random.nextInt(3)) {
						BlockPos local = new BlockPos(pos.getX() + x, world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX() + x, pos.getZ() + z), pos.getZ() + z).down();

						if (world.getBlockState(local).isOf(Blocks.WATER)) {
							world.setBlockState(local, Blocks.OAK_PLANKS.getDefaultState(), 3);
						}
					}
				}
			}

			int boatCount = 6 + random.nextInt(8);
			int placed = 0;
			for (int i = 0; i < boatCount; i++) {
				int dx = random.nextInt(5) - random.nextInt(5);
				int dz = random.nextInt(5) - random.nextInt(5);

				BlockPos local = new BlockPos(pos.getX() + dx, world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, pos.getX() + dx, pos.getZ() + dz), pos.getZ() + dz).down();

				if (world.getBlockState(local).isOf(Blocks.WATER)) {
					placed++;
					BoatEntity boat = EntityType.OAK_BOAT.create(world.toServerWorld(), SpawnReason.CHUNK_GENERATION);
					boat.setPos(local.getX(), local.getY() + 1, local.getZ());
					world.spawnEntity(boat);

					if (placed >= 4) {
						return;
					}
				}
			}
		}
	}

	@Override
	public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess world, StructureAccessor structureAccessor, Chunk chunk) {
	}
}