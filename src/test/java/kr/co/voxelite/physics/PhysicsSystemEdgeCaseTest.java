package kr.co.voxelite.physics;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.entity.Player;
import kr.co.voxelite.test.GdxTestRunner;
import kr.co.voxelite.world.BlockManager;
import kr.co.voxelite.world.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Edge case tests for PhysicsSystem - covering delta validation, boundary cases, and NaN/Infinity handling
 */
class PhysicsSystemEdgeCaseTest extends GdxTestRunner {
    
    private PhysicsSystem physics;
    private World world;
    private Player player;
    @Mock
    private BlockManager blockManager;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(blockManager.createBlockInstances(any(Vector3.class), anyInt()))
                .thenReturn(new ArrayList<>());
        when(blockManager.createBlockInstances(any(Vector3.class)))
                .thenReturn(new ArrayList<>());
        world = new World(blockManager);
        physics = new PhysicsSystem(world);
        player = new Player(new Vector3(0, 10, 0));
    }
    
    @Test
    void update_ZeroDelta_ShouldNotCrash() {
        // Given
        float delta = 0f;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            physics.update(player, delta);
        });
        
        // Position should not change
        Vector3 pos = player.getPosition();
        assertEquals(0, pos.x, 0.001f);
        assertEquals(10, pos.y, 0.001f);
        assertEquals(0, pos.z, 0.001f);
    }
    
    @Test
    void update_NegativeDelta_ShouldNotCrash() {
        // Given
        float delta = -0.016f;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            physics.update(player, delta);
        });
    }
    
    @Test
    void update_VeryLargeDelta_ShouldNotCrash() {
        // Given
        float delta = 10.0f; // Very large delta
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            physics.update(player, delta);
        });
    }
    
    @Test
    void update_NaNDelta_ShouldNotCrash() {
        // Given
        float delta = Float.NaN;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            physics.update(player, delta);
        });
    }
    
    @Test
    void update_PositiveInfinityDelta_ShouldNotCrash() {
        // Given
        float delta = Float.POSITIVE_INFINITY;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            physics.update(player, delta);
        });
    }
    
    @Test
    void update_NegativeInfinityDelta_ShouldNotCrash() {
        // Given
        float delta = Float.NEGATIVE_INFINITY;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            physics.update(player, delta);
        });
    }
    
    @Test
    void update_PlayerWithNaNPosition_ShouldNotCrash() {
        // Given
        player.setPosition(Float.NaN, Float.NaN, Float.NaN);
        float delta = 0.016f;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            physics.update(player, delta);
        });
    }
    
    @Test
    void update_PlayerWithInfinityPosition_ShouldNotCrash() {
        // Given
        player.setPosition(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        float delta = 0.016f;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            physics.update(player, delta);
        });
    }
    
    @Test
    void update_PlayerWithNaNVelocity_ShouldNotCrash() {
        // Given
        player.getVelocity().set(Float.NaN, Float.NaN, Float.NaN);
        float delta = 0.016f;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            physics.update(player, delta);
        });
    }
    
    @Test
    void update_PlayerWithInfinityVelocity_ShouldNotCrash() {
        // Given
        player.getVelocity().set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        float delta = 0.016f;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            physics.update(player, delta);
        });
    }
    
    @Test
    void update_NoCollision_ShouldNotReturnExtremeValues() {
        // Given - Player in empty world
        player.setPosition(0, 10, 0);
        player.getVelocity().x = 5f; // Moving right
        float delta = 0.016f;
        
        // When
        physics.update(player, delta);
        
        // Then - Position should change, but not to extreme values
        Vector3 pos = player.getPosition();
        assertTrue(pos.x > 0); // Should move
        assertFalse(Float.isInfinite(pos.x));
        assertFalse(Float.isNaN(pos.x));
    }
    
    @Test
    void tryJump_PlayerWithNaNPosition_ShouldNotCrash() {
        // Given
        player.setPosition(Float.NaN, Float.NaN, Float.NaN);
        player.setOnGround(true);
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            physics.tryJump(player);
        });
    }
    
    @Test
    void tryJump_PlayerWithInfinityPosition_ShouldNotCrash() {
        // Given
        player.setPosition(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        player.setOnGround(true);
        
        // When/Then
        assertDoesNotThrow(() -> {
            physics.tryJump(player);
        });
    }
    
    @Test
    void update_VerySmallDelta_ShouldNotCrash() {
        // Given
        float delta = 0.0001f; // Very small delta
        
        // When/Then
        assertDoesNotThrow(() -> {
            physics.update(player, delta);
        });
    }
    
    @Test
    void update_PlayerFallingWithLargeDelta_ShouldClampVelocity() {
        // Given
        player.setPosition(0, 10, 0);
        player.setOnGround(false);
        float delta = 10.0f; // Very large delta
        
        // When
        physics.update(player, delta);
        
        // Then - Velocity should be clamped to terminal velocity
        assertTrue(player.getVelocity().y >= Player.TERMINAL_VELOCITY);
    }
    
    @Test
    void update_PlayerMovingWithLargeDelta_ShouldMoveFar() {
        // Given
        player.setPosition(0, 1, 0);
        player.getVelocity().x = 10f; // Fast movement
        float delta = 1.0f; // Large delta
        
        // When
        physics.update(player, delta);
        
        // Then - Should move far
        assertTrue(player.getPosition().x > 5f);
    }
}
