package kr.co.voxelite.engine;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.world.BlockManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxeliteConfigTest {
    @Test
    void constructor_ShouldCreateHeadlessDefaults() {
        VoxeliteConfig config = new VoxeliteConfig();

        assertEquals(11, config.worldWidth);
        assertEquals(11, config.worldHeight);
        assertEquals(-1f, config.groundLevel);
        assertTrue(config.autoCreateGround);
        assertEquals(0, config.defaultGroundBlockType);
        assertNull(config.chunkGenerator);
        assertNull(config.chunkLoadPolicy);
        assertEquals("saves/world1", config.worldSavePath);
        assertEquals(new Vector3(0f, -0.5f, 0f), config.playerStartPosition);
        assertNull(config.blockPropertiesProvider);
    }

    @Test
    void builder_ShouldSetCoreFields() {
        VoxeliteConfig config = VoxeliteConfig.builder()
            .worldSize(32, 48)
            .groundLevel(2f)
            .autoCreateGround(false)
            .playerStart(1f, 2f, 3f)
            .defaultGroundBlockType(7)
            .initialChunkRadius(6)
            .chunkPreloadRadius(3)
            .worldSavePath("saves/test")
            .worldSeed(42L)
            .blockPropertiesProvider(blockType -> blockType != 6)
            .build();

        assertEquals(32, config.worldWidth);
        assertEquals(48, config.worldHeight);
        assertEquals(2f, config.groundLevel);
        assertFalse(config.autoCreateGround);
        assertEquals(new Vector3(1f, 2f, 3f), config.playerStartPosition);
        assertEquals(7, config.defaultGroundBlockType);
        assertEquals(6, config.initialChunkRadius);
        assertEquals(3, config.chunkPreloadRadius);
        assertEquals("saves/test", config.worldSavePath);
        assertEquals(42L, config.worldSeed);
        assertFalse(config.blockPropertiesProvider.isSolid(6));
    }
}
