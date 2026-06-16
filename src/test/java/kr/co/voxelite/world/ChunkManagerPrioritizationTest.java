package kr.co.voxelite.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkManagerPrioritizationTest {

    @TempDir
    Path tempDir;

    @Test
    void updateLoadedChunks_ShouldThrottleInitialLoadRequestsToNearestChunks() {
        ChunkManager chunkManager = new ChunkManager(
            tempDir.toString(),
            1,
            (chunk, blockType) -> {
            },
            squareLoadPolicy(16)
        );

        chunkManager.updateLoadedChunks(0f, 0f);

        Collection<Chunk> loadedChunks = chunkManager.getLoadedChunks();
        assertTrue(loadedChunks.size() <= 8);
        assertTrue(loadedChunks.stream().anyMatch(chunk -> chunk.getCoord().equals(new ChunkCoord(0, 0))));
        assertTrue(loadedChunks.stream().allMatch(chunk -> distanceSq(chunk.getCoord(), new ChunkCoord(0, 0)) <= 2));
    }

    private IChunkLoadPolicy squareLoadPolicy(int radius) {
        return new IChunkLoadPolicy() {
            @Override
            public boolean shouldLoadToMemory(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
                return Math.abs(chunkX - playerChunkX) <= radius
                    && Math.abs(chunkZ - playerChunkZ) <= radius;
            }

            @Override
            public boolean shouldKeepLoaded(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
                return shouldLoadToMemory(chunkX, chunkZ, playerChunkX, playerChunkZ);
            }

            @Override
            public boolean shouldPregenerate(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
                return false;
            }

            @Override
            public int getMaxLoadedChunks() {
                return 2048;
            }
        };
    }

    private int distanceSq(ChunkCoord coord, ChunkCoord center) {
        int dx = coord.x - center.x;
        int dz = coord.z - center.z;
        return dx * dx + dz * dz;
    }
}
