package kr.co.voxelite.engine;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.entity.Player;
import kr.co.voxelite.physics.PhysicsSystem;
import kr.co.voxelite.world.Chunk;
import kr.co.voxelite.world.IChunkLoadPolicy;
import kr.co.voxelite.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxeliteEngineTest {
    @TempDir
    Path tempDir;

    @Test
    void initialize_ShouldCreateHeadlessSystems() {
        VoxeliteEngine engine = VoxeliteEngine.builder()
            .autoCreateGround(false)
            .playerStart(10f, 20f, 30f)
            .build();

        engine.initialize();

        assertTrue(engine.isInitialized());
        assertNotNull(engine.getWorld());
        assertNotNull(engine.getPlayer());
        assertNotNull(engine.getPhysics());
        assertEquals(new Vector3(10f, 20f, 30f), engine.getPlayer().getPosition());
    }

    @Test
    void update_BeforeInitialize_ShouldThrow() {
        VoxeliteEngine engine = VoxeliteEngine.builder().build();
        assertThrows(IllegalStateException.class, () -> engine.update(0.016f));
    }

    @Test
    void addAndRemoveBlock_ShouldMutateWorld() {
        VoxeliteEngine engine = VoxeliteEngine.builder().autoCreateGround(false).build();
        engine.initialize();

        Vector3 pos = new Vector3(1, 2, 3);
        engine.addBlock(pos, 4);
        assertEquals(4, engine.getWorld().getBlockType(pos));

        assertTrue(engine.removeBlock(pos));
        assertEquals(-1, engine.getWorld().getBlockType(pos));
    }

    @Test
    void dispose_ShouldBeIdempotent() {
        VoxeliteEngine engine = VoxeliteEngine.builder().autoCreateGround(false).build();
        engine.initialize();

        engine.dispose();
        engine.dispose();

        assertFalse(engine.isInitialized());
    }

    @Test
    void getters_ShouldExposeCoreTypes() {
        VoxeliteEngine engine = VoxeliteEngine.builder().autoCreateGround(false).build();
        engine.initialize();

        assertTrue(engine.getWorld() instanceof World);
        assertTrue(engine.getPlayer() instanceof Player);
        assertTrue(engine.getPhysics() instanceof PhysicsSystem);
    }

    @Test
    void initialize_ShouldSpawnOnTopOfTheBlockAtTheExactSpawnColumn() {
        VoxeliteEngine engine = VoxeliteEngine.builder()
            .playerStart(0f, 100f, 0f)
            .autoCreateGround(true)
            .worldSavePath(tempDir.resolve("spawn-world").toString())
            .chunkGenerator((chunk, blockType) -> {
                chunk.addBlockLocal(0, 5, 0, blockType);
                chunk.addBlockLocal(Chunk.CHUNK_SIZE / 2, 20, Chunk.CHUNK_SIZE / 2, blockType);
            })
            .chunkLoadPolicy(new IChunkLoadPolicy() {
                @Override
                public boolean shouldLoadToMemory(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
                    return chunkX == playerChunkX && chunkZ == playerChunkZ;
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
                    return 1;
                }
            })
            .initialChunkRadius(0)
            .chunkPreloadRadius(0)
            .defaultGroundBlockType(1)
            .build();

        engine.initialize();

        assertEquals(5.5f, engine.getPlayer().getPosition().y);
        engine.dispose();
    }
}
