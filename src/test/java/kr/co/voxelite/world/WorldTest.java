package kr.co.voxelite.world;

import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldTest {
    private World world;

    @BeforeEach
    void setUp() {
        world = new World(new BlockManager());
    }

    @Test
    void addBlock_ShouldCreateChunkBackedBlock() {
        Vector3 pos = new Vector3(0, 0, 0);

        world.addBlock(pos, 2);

        assertTrue(world.hasBlock(pos));
        assertEquals(2, world.getBlockType(pos));
        assertEquals(1, world.getBlockCount());
    }

    @Test
    void removeBlock_ShouldRemoveExistingBlock() {
        Vector3 pos = new Vector3(3, 4, 5);
        world.addBlock(pos, 1);

        assertTrue(world.removeBlock(pos));
        assertFalse(world.hasBlock(pos));
        assertEquals(-1, world.getBlockType(pos));
    }

    @Test
    void clear_ShouldResetLoadedBlocks() {
        world.addBlock(new Vector3(0, 0, 0), 1);
        world.addBlock(new Vector3(10, 0, 10), 2);

        world.clear();

        assertEquals(0, world.getBlockCount());
        assertTrue(world.getBlockPositions().isEmpty());
    }

    @Test
    void getNearbyBlockPositions_ShouldReturnBlocksFromNeighborChunks() {
        world.addBlock(new Vector3(0, 0, 0), 1);
        world.addBlock(new Vector3(16, 0, 0), 2);

        List<Vector3> nearby = world.getNearbyBlockPositions(0, 0, 1);

        assertEquals(2, nearby.size());
    }

    @Test
    void applyChunkAndUnloadChunk_ShouldReflectNetworkSyncedWorldState() {
        Chunk chunk = new Chunk(new ChunkCoord(2, 3));
        chunk.addBlockLocal(1, 5, 2, 7);
        chunk.markAsGenerated();
        chunk.setModified(false);

        world.applyChunk(chunk);

        Vector3 blockPosition = new Vector3(2 * Chunk.CHUNK_SIZE + 1, 5, 3 * Chunk.CHUNK_SIZE + 2);
        assertTrue(world.hasBlock(blockPosition));
        assertEquals(7, world.getBlockType(blockPosition));

        world.unloadChunk(chunk.getCoord());

        assertFalse(world.hasBlock(blockPosition));
    }
}
