package kr.co.voxelite.world;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkManagerDirtySectionTest {

    @TempDir
    Path tempDir;

    @Test
    void addDirtySectionsAroundBlock_ShouldMarkCurrentAndVerticalNeighborSections() {
        ChunkManager chunkManager = createChunkManager();

        chunkManager.addDirtySectionsAroundBlock(new Vector3(1f, 15f, 1f));

        Set<RenderSectionKey> dirtySections = drainAllDirtySections(chunkManager);
        assertEquals(Set.of(
            new RenderSectionKey(new ChunkCoord(0, 0), 0),
            new RenderSectionKey(new ChunkCoord(0, 0), 1)
        ), dirtySections);
    }

    @Test
    void addDirtySectionsAroundBlock_ShouldMarkHorizontalNeighborChunkSections() {
        ChunkManager chunkManager = createChunkManager();

        chunkManager.addDirtySectionsAroundBlock(new Vector3(0f, 20f, 15f));

        Set<RenderSectionKey> dirtySections = drainAllDirtySections(chunkManager);
        assertEquals(Set.of(
            new RenderSectionKey(new ChunkCoord(0, 0), 1),
            new RenderSectionKey(new ChunkCoord(-1, 0), 1),
            new RenderSectionKey(new ChunkCoord(0, 1), 1)
        ), dirtySections);
    }

    @Test
    void addDirtyChunk_ShouldQueueAllRenderSectionsForTheChunk() {
        ChunkManager chunkManager = createChunkManager();
        ChunkCoord coord = new ChunkCoord(2, 3);

        chunkManager.addDirtyChunk(coord);

        RenderSectionKey first = chunkManager.pollDirtySection();
        assertNotNull(first);
        assertEquals(coord, first.chunkCoord());

        Set<Integer> queuedSections = new HashSet<>();
        queuedSections.add(first.sectionY());
        queuedSections.addAll(chunkManager.drainDirtySections(coord));

        Set<Integer> expectedSections = new HashSet<>();
        for (int sectionY = 0; sectionY < Chunk.getRenderSectionCount(); sectionY++) {
            expectedSections.add(sectionY);
        }

        assertEquals(expectedSections, queuedSections);
        assertNull(chunkManager.pollDirtySection());
    }

    @Test
    void addDirtySectionsAroundBlock_ShouldMarkVisibleSectionWhenEditingBelowWorldFloor() {
        ChunkManager chunkManager = createChunkManager();

        chunkManager.addDirtySectionsAroundBlock(new Vector3(1f, -1f, 1f));

        Set<RenderSectionKey> dirtySections = drainAllDirtySections(chunkManager);
        assertTrue(dirtySections.contains(new RenderSectionKey(new ChunkCoord(0, 0), 0)));
    }

    private ChunkManager createChunkManager() {
        IChunkGenerator generator = (chunk, blockType) -> {
        };
        IChunkLoadPolicy loadPolicy = new IChunkLoadPolicy() {
            @Override
            public boolean shouldLoadToMemory(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
                return false;
            }

            @Override
            public boolean shouldKeepLoaded(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
                return false;
            }

            @Override
            public boolean shouldPregenerate(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
                return false;
            }

            @Override
            public int getMaxLoadedChunks() {
                return 16;
            }
        };
        return new ChunkManager(tempDir.toString(), 0, generator, loadPolicy);
    }

    private Set<RenderSectionKey> drainAllDirtySections(ChunkManager chunkManager) {
        Set<RenderSectionKey> dirtySections = new HashSet<>();
        RenderSectionKey key;
        while ((key = chunkManager.pollDirtySection()) != null) {
            dirtySections.add(key);
        }
        return dirtySections;
    }
}
