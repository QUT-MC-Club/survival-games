package supercoder79.survivalgames.game.map;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.StructureTemplateManager;
import supercoder79.survivalgames.game.config.Y256Height;
import supercoder79.survivalgames.game.map.gen.structure.ChunkBox;

import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.random.Random;

public final class SurvivalGamesJigsawGenerator {
    private final Random random = Random.create();
    private final DynamicRegistryManager registryManager;
    private final StructureTemplateManager structureManager;
    private final ChunkGenerator generator;

    private BlockPos origin = BlockPos.ORIGIN;
    private final Long2ObjectMap<List<PoolStructurePiece>> piecesByChunk;
    private final ChunkBox box;

    public SurvivalGamesJigsawGenerator(MinecraftServer server, ChunkGenerator generator, Long2ObjectMap<List<PoolStructurePiece>> piecesByChunk) {
        this.registryManager = server.getRegistryManager();
        this.structureManager = server.getStructureTemplateManager();
        this.generator = generator;
        this.piecesByChunk = piecesByChunk;
        this.box = new ChunkBox();
    }

    public void arrangePieces(BlockPos origin, Identifier startPoolId, int depth) {
        this.origin = origin;

        List<PoolStructurePiece> pieces = this.generateArrangedPieces(origin, startPoolId, depth);
        this.associatePiecesByChunk(pieces);

        for (Long pos : this.piecesByChunk.keySet()) {
            this.box.encompass(new ChunkPos(pos));
        }
    }

    private List<PoolStructurePiece> generateArrangedPieces(BlockPos origin, Identifier startPoolId, int depth) {
        List<PoolStructurePiece> pieces = new ArrayList<>();

        // we start with a starting piece which all further pieces will branch off from
        PoolStructurePiece startPiece = this.createStartPiece(origin, startPoolId);
         this.placePieceOnGround(origin, startPiece);
        // Not quite sure how to port this

        pieces.add(startPiece);

        // [Patbox]: I wasn't sure how to get to correctly update this, but looking that this class doesn't seem to be actually used, I commented it out

        // invoke vanilla code to handle the actual arrangement logic
        //StructurePoolBasedGenerator.method_27230(this.registryManager, startPiece, depth, PoolStructurePiece::new, this.generator, this.structureManager, pieces, this.random, Y256Height.INSTANCE);

        return pieces;
    }

    private PoolStructurePiece createStartPiece(BlockPos origin, Identifier startPoolId) {
        StructurePool pool = this.registryManager.getOrThrow(RegistryKeys.TEMPLATE_POOL).get(startPoolId);
        if (pool == null) {
            //throw new IllegalStateException("missing start pool: '" + startPoolId + "'");
        }

        BlockRotation rotation = BlockRotation.random(this.random);
        StructurePoolElement element = pool.getRandomElement(this.random);

        return new PoolStructurePiece(
                this.structureManager, element,
                origin, element.getGroundLevelDelta(), rotation,
                element.getBoundingBox(this.structureManager, origin, rotation),
                StructureLiquidSettings.APPLY_WATERLOGGING
        );
    }

    private void placePieceOnGround(BlockPos origin, PoolStructurePiece piece) {
        BlockBox box = piece.getBoundingBox();
        int centerX = (box.getMaxX() + box.getMaxX()) / 2;
        int centerZ = (box.getMaxZ() + box.getMinZ()) / 2;
        int centerY = origin.getY() + this.generator.getHeightOnGround(centerX, centerZ, Heightmap.Type.WORLD_SURFACE_WG, Y256Height.INSTANCE, null);

        // offset the piece to be level with the ground at its center
        int targetY = box.getMinY() + piece.getGroundLevelDelta();
        piece.translate(0, centerY - targetY, 0);
    }

    private void associatePiecesByChunk(List<PoolStructurePiece> pieces) {
        for (PoolStructurePiece piece : pieces) {
            BlockBox box = piece.getBoundingBox();
            int minChunkX = box.getMinX() >> 4;
            int minChunkZ = box.getMinZ() >> 4;
            int maxChunkX = box.getMaxX() >> 4;
            int maxChunkZ = box.getMaxZ() >> 4;

            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                    long chunkPos = ChunkPos.toLong(chunkX, chunkZ);
                    List<PoolStructurePiece> piecesByChunk = this.piecesByChunk.computeIfAbsent(chunkPos, p -> new ArrayList<>());
                    piecesByChunk.add(piece);
                }
            }
        }
    }

    public void generate(ChunkRegion region, StructureAccessor structures) {
        ChunkPos chunkPos = new ChunkPos(region.getCenterPos().x, region.getCenterPos().x);
        List<PoolStructurePiece> pieces = this.piecesByChunk.get(chunkPos.toLong());

        if (pieces != null) {
            // generate all intersecting pieces with the mask of this chunk
            BlockBox chunkMask = new BlockBox(chunkPos.getStartX(), 0, chunkPos.getStartZ(), chunkPos.getEndX(), 255, chunkPos.getEndZ());
            for (PoolStructurePiece piece : pieces) {
                piece.generate(region, structures, this.generator, this.random, chunkMask, this.origin, false);
            }
        }
    }

    public List<PoolStructurePiece> getPiecesInChunk(ChunkPos pos) {
        return this.piecesByChunk.get(pos.toLong());
    }

    public ChunkBox getBox() {
        return box;
    }
}
