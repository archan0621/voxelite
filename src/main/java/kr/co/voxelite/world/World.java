package kr.co.voxelite.world;

import com.badlogic.gdx.math.Vector3;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * World management class backed by chunk data.
 */
public class World {
    private final BlockManager blockManager;
    private ChunkManager chunkManager;

    public World(BlockManager blockManager) {
        this.blockManager = blockManager != null ? blockManager : new BlockManager();
        chunkManager = createDefaultChunkManager();
    }

    public void initWithChunks(String worldPath, int defaultBlockType, IChunkGenerator generator, IChunkLoadPolicy loadPolicy) {
        if (chunkManager != null) {
            chunkManager.shutdown();
        }
        chunkManager = new ChunkManager(worldPath, defaultBlockType, generator, loadPolicy);
    }

    public float generateInitialChunks(float spawnX, float spawnZ, int totalRadius, int loadRadius) {
        ensureChunkManager();
        chunkManager.generateInitialChunks(spawnX, spawnZ, totalRadius, loadRadius);
        return chunkManager.getChunkCenterHeight(spawnX, spawnZ);
    }

    public void updateChunks(float playerX, float playerZ) {
        if (chunkManager != null) {
            chunkManager.updateLoadedChunks(playerX, playerZ);
        }
    }

    public void updateChunks(Collection<Vector3> playerPositions) {
        if (chunkManager != null) {
            chunkManager.updateLoadedChunks(playerPositions);
        }
    }

    public void processPendingChunks() {
        if (chunkManager != null) {
            chunkManager.processPendingChunksPublic();
        }
    }

    public void clear() {
        if (chunkManager != null) {
            chunkManager.shutdown();
        }
        chunkManager = createDefaultChunkManager();
    }

    public void addBlock(Vector3 position, int blockType) {
        if (!isFinite(position) || chunkManager == null) {
            return;
        }
        chunkManager.addBlock(position, blockType);
        markChunksDirty(position);
    }

    public void addBlock(Vector3 position) {
        addBlock(position, 0);
    }

    public boolean removeBlock(Vector3 position) {
        if (!isFinite(position) || chunkManager == null) {
            return false;
        }
        boolean removed = chunkManager.removeBlock(position);
        if (removed) {
            markChunksDirty(position);
        }
        return removed;
    }

    public boolean hasBlock(Vector3 position) {
        if (!isFinite(position) || chunkManager == null) {
            return false;
        }
        return chunkManager.getBlockAt(position) != null;
    }

    public boolean hasBlockAt(float worldX, float worldY, float worldZ) {
        if (chunkManager == null) {
            return false;
        }
        return chunkManager.hasBlockAt(worldX, worldY, worldZ);
    }

    @Deprecated
    public List<Vector3> getBlockPositions() {
        if (chunkManager != null) {
            return chunkManager.getBlockPositionsSnapshot();
        }
        return new ArrayList<>();
    }

    public List<Vector3> getNearbyBlockPositions(float playerX, float playerZ, int chunkRadius) {
        List<Vector3> positions = new ArrayList<>();
        if (chunkManager == null) {
            return positions;
        }

        List<Chunk> nearbyChunks = chunkManager.getNearbyChunks(playerX, playerZ, chunkRadius);
        for (Chunk chunk : nearbyChunks) {
            for (Chunk.BlockData block : chunk.getBlocks()) {
                positions.add(block.getWorldPos(chunk.getCoord()));
            }
        }
        return positions;
    }

    public ChunkCoord getChunkCoordAt(float worldX, float worldZ) {
        return ChunkCoord.fromWorldPos(worldX, worldZ, Chunk.CHUNK_SIZE);
    }

    public int getBlockType(Vector3 position) {
        if (!isFinite(position) || chunkManager == null) {
            return -1;
        }
        Chunk.BlockData block = chunkManager.getBlockAt(position);
        return block != null ? block.blockType : -1;
    }

    public int getBlockCount() {
        if (chunkManager == null) {
            return 0;
        }
        return chunkManager.getAllBlocks().size();
    }

    public BlockManager getBlockManager() {
        return blockManager;
    }

    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    public void dispose() {
        if (chunkManager != null) {
            chunkManager.shutdown();
            chunkManager = null;
        }
    }

    public void applyChunk(Chunk chunk) {
        if (chunk == null) {
            return;
        }
        ensureChunkManager();
        chunkManager.replaceChunk(chunk);
    }

    public void unloadChunk(ChunkCoord coord) {
        if (coord == null || chunkManager == null) {
            return;
        }
        chunkManager.removeLoadedChunk(coord);
    }

    private ChunkManager createDefaultChunkManager() {
        String worldPath = Path.of(
            System.getProperty("java.io.tmpdir"),
            "voxelite-world-" + UUID.randomUUID()
        ).toString();

        IChunkGenerator generator = (chunk, blockType) -> {
        };

        IChunkLoadPolicy loadPolicy = new IChunkLoadPolicy() {
            @Override
            public boolean shouldLoadToMemory(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
                int dx = chunkX - playerChunkX;
                int dz = chunkZ - playerChunkZ;
                return dx * dx + dz * dz <= 16;
            }

            @Override
            public boolean shouldKeepLoaded(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
                int dx = chunkX - playerChunkX;
                int dz = chunkZ - playerChunkZ;
                return dx * dx + dz * dz <= 25;
            }

            @Override
            public boolean shouldPregenerate(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
                return false;
            }

            @Override
            public int getMaxLoadedChunks() {
                return 128;
            }
        };

        return new ChunkManager(worldPath, 0, generator, loadPolicy);
    }

    private void markChunksDirty(Vector3 position) {
        ChunkCoord coord = ChunkCoord.fromWorldPos(position.x, position.z, Chunk.CHUNK_SIZE);
        markChunkDirty(coord);

        int blockX = (int) Math.floor(position.x);
        int blockZ = (int) Math.floor(position.z);
        int localX = Math.floorMod(blockX, Chunk.CHUNK_SIZE);
        int localZ = Math.floorMod(blockZ, Chunk.CHUNK_SIZE);

        if (localX == 0) {
            markChunkDirty(new ChunkCoord(coord.x - 1, coord.z));
        } else if (localX == Chunk.CHUNK_SIZE - 1) {
            markChunkDirty(new ChunkCoord(coord.x + 1, coord.z));
        }

        if (localZ == 0) {
            markChunkDirty(new ChunkCoord(coord.x, coord.z - 1));
        } else if (localZ == Chunk.CHUNK_SIZE - 1) {
            markChunkDirty(new ChunkCoord(coord.x, coord.z + 1));
        }
    }

    private void markChunkDirty(ChunkCoord coord) {
        if (chunkManager != null) {
            chunkManager.addDirtyChunk(coord);
        }
    }

    private void ensureChunkManager() {
        if (chunkManager == null) {
            chunkManager = createDefaultChunkManager();
        }
    }

    private boolean isFinite(Vector3 position) {
        return position != null
            && Float.isFinite(position.x)
            && Float.isFinite(position.y)
            && Float.isFinite(position.z);
    }
}
