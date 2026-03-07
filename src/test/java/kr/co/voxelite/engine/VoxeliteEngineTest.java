package kr.co.voxelite.engine;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.entity.Player;
import kr.co.voxelite.physics.PhysicsSystem;
import kr.co.voxelite.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxeliteEngineTest {
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
}
