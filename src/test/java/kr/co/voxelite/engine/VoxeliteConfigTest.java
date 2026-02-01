package kr.co.voxelite.engine;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.test.GdxTestRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VoxeliteConfig class
 */
class VoxeliteConfigTest extends GdxTestRunner {
    
    @Test
    void constructor_ShouldCreateDefaultConfig() {
        // When
        VoxeliteConfig config = new VoxeliteConfig();
        
        // Then
        assertEquals(11, config.worldWidth);
        assertEquals(11, config.worldHeight);
        assertEquals(-1f, config.groundLevel);
        assertTrue(config.autoCreateGround);
        assertEquals(0, config.defaultGroundBlockType);
        assertNull(config.textureAtlasPath);
        assertEquals(0f, config.playerStartPosition.x, 0.001f);
        assertEquals(-0.5f, config.playerStartPosition.y, 0.001f);
        assertEquals(0f, config.playerStartPosition.z, 0.001f);
        assertEquals(5f, config.playerMoveSpeed);
        assertEquals(67f, config.fieldOfView);
        assertEquals(-20f, config.initialPitch);
        assertEquals(0.1f, config.mouseSensitivity);
    }
    
    @Test
    void builder_WorldSize_ShouldSetWidthAndHeight() {
        // When
        VoxeliteConfig config = VoxeliteConfig.builder()
                .worldSize(20, 30)
                .build();
        
        // Then
        assertEquals(20, config.worldWidth);
        assertEquals(30, config.worldHeight);
    }
    
    @Test
    void builder_GroundLevel_ShouldSetLevel() {
        // When
        VoxeliteConfig config = VoxeliteConfig.builder()
                .groundLevel(5f)
                .build();
        
        // Then
        assertEquals(5f, config.groundLevel);
    }
    
    @Test
    void builder_AutoCreateGround_ShouldSetFlag() {
        // When
        VoxeliteConfig config = VoxeliteConfig.builder()
                .autoCreateGround(false)
                .build();
        
        // Then
        assertFalse(config.autoCreateGround);
    }
    
    @Test
    void builder_PlayerStart_ShouldSetPosition() {
        // When
        VoxeliteConfig config = VoxeliteConfig.builder()
                .playerStart(10f, 20f, 30f)
                .build();
        
        // Then
        Vector3 pos = config.playerStartPosition;
        assertEquals(10f, pos.x, 0.001f);
        assertEquals(20f, pos.y, 0.001f);
        assertEquals(30f, pos.z, 0.001f);
    }
    
    @Test
    void builder_PlayerSpeed_ShouldSetSpeed() {
        // When
        VoxeliteConfig config = VoxeliteConfig.builder()
                .playerSpeed(10f)
                .build();
        
        // Then
        assertEquals(10f, config.playerMoveSpeed);
    }
    
    @Test
    void builder_FieldOfView_ShouldSetFOV() {
        // When
        VoxeliteConfig config = VoxeliteConfig.builder()
                .fieldOfView(90f)
                .build();
        
        // Then
        assertEquals(90f, config.fieldOfView);
    }
    
    @Test
    void builder_CameraPitch_ShouldSetPitch() {
        // When
        VoxeliteConfig config = VoxeliteConfig.builder()
                .cameraPitch(-30f)
                .build();
        
        // Then
        assertEquals(-30f, config.initialPitch);
    }
    
    @Test
    void builder_MouseSensitivity_ShouldSetSensitivity() {
        // When
        VoxeliteConfig config = VoxeliteConfig.builder()
                .mouseSensitivity(0.2f)
                .build();
        
        // Then
        assertEquals(0.2f, config.mouseSensitivity);
    }
    
    @Test
    void builder_DefaultGroundBlockType_ShouldSetType() {
        // When
        VoxeliteConfig config = VoxeliteConfig.builder()
                .defaultGroundBlockType(3)
                .build();
        
        // Then
        assertEquals(3, config.defaultGroundBlockType);
    }
    
    @Test
    void builder_TextureAtlasPath_ShouldSetPath() {
        // When
        VoxeliteConfig config = VoxeliteConfig.builder()
                .textureAtlasPath("texture/atlas.png")
                .build();
        
        // Then
        assertEquals("texture/atlas.png", config.textureAtlasPath);
    }
    
    @Test
    void builder_ChainedCalls_ShouldWork() {
        // When
        VoxeliteConfig config = VoxeliteConfig.builder()
                .worldSize(15, 15)
                .groundLevel(0f)
                .playerStart(5f, 10f, 15f)
                .playerSpeed(8f)
                .fieldOfView(75f)
                .cameraPitch(-25f)
                .mouseSensitivity(0.15f)
                .defaultGroundBlockType(2)
                .textureAtlasPath("test.png")
                .autoCreateGround(false)
                .build();
        
        // Then
        assertEquals(15, config.worldWidth);
        assertEquals(15, config.worldHeight);
        assertEquals(0f, config.groundLevel);
        assertEquals(5f, config.playerStartPosition.x, 0.001f);
        assertEquals(10f, config.playerStartPosition.y, 0.001f);
        assertEquals(15f, config.playerStartPosition.z, 0.001f);
        assertEquals(8f, config.playerMoveSpeed);
        assertEquals(75f, config.fieldOfView);
        assertEquals(-25f, config.initialPitch);
        assertEquals(0.15f, config.mouseSensitivity);
        assertEquals(2, config.defaultGroundBlockType);
        assertEquals("test.png", config.textureAtlasPath);
        assertFalse(config.autoCreateGround);
    }
}
