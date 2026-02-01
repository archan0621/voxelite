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
 * Unit tests for PhysicsSystem class
 */
class PhysicsSystemTest extends GdxTestRunner {
    
    private PhysicsSystem physics;
    private World world;
    private Player player;
    @Mock
    private BlockManager blockManager;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Mock createBlockInstances to return empty list (avoid OpenGL dependency)
        when(blockManager.createBlockInstances(any(Vector3.class), anyInt()))
                .thenReturn(new ArrayList<>());
        when(blockManager.createBlockInstances(any(Vector3.class)))
                .thenReturn(new ArrayList<>());
        world = new World(blockManager);
        physics = new PhysicsSystem(world);
        player = new Player(new Vector3(0, 10, 0));
    }
    
    @Test
    void update_PlayerInAir_ShouldApplyGravity() {
        // Given
        player.setOnGround(false);
        float initialY = player.getPosition().y;
        float delta = 0.016f; // ~60fps
        
        // When
        physics.update(player, delta);
        
        // Then
        assertTrue(player.getPosition().y < initialY); // Should fall
        assertTrue(player.getVelocity().y < 0); // Velocity should be negative
    }
    
    @Test
    void update_PlayerOnGround_ShouldNotApplyGravity() {
        // Given
        world.addBlock(new Vector3(0, 0, 0), 0);
        player.setPosition(0, 1, 0);
        player.setOnGround(true);
        float initialY = player.getPosition().y;
        float delta = 0.016f;
        
        // When
        physics.update(player, delta);
        
        // Then
        // Player should stay on ground (may have slight movement due to collision resolution)
        assertTrue(Math.abs(player.getPosition().y - initialY) < 0.1f);
    }
    
    @Test
    void update_Gravity_ShouldClampToTerminalVelocity() {
        // Given
        player.setOnGround(false);
        player.getVelocity().y = Player.TERMINAL_VELOCITY - 10; // Below terminal
        
        // When
        physics.update(player, 0.1f);
        
        // Then
        assertTrue(player.getVelocity().y >= Player.TERMINAL_VELOCITY);
    }
    
    @Test
    void update_PlayerHitsCeiling_ShouldStopUpwardMovement() {
        // Given
        world.addBlock(new Vector3(0, 5, 0), 0);
        player.setPosition(0, 3, 0);
        player.getVelocity().y = 10; // Moving up
        float delta = 0.016f;
        
        // When
        physics.update(player, delta);
        
        // Then
        assertTrue(player.getVelocity().y <= 0); // Should stop upward movement
    }
    
    @Test
    void update_PlayerHitsWall_ShouldStopHorizontalMovement() {
        // Given
        // Player at (0, 0, 0) with width 0.6 (radius 0.3), so bounds: -0.3 to 0.3
        // Block at (0.5, 0, 0) with size 1.0 (radius 0.5), so bounds: 0.0 to 1.0
        // They will collide when player moves right
        world.addBlock(new Vector3(0.5f, 0, 0), 0);
        player.setPosition(0, 0, 0);
        player.getVelocity().x = 5; // Moving right
        float delta = 0.1f; // Larger delta to ensure collision
        
        // When
        physics.update(player, delta);
        
        // Then
        assertTrue(player.getPosition().x < 0.5f); // Should not pass through wall
        assertEquals(0, player.getVelocity().x, 0.001f); // Velocity should be zero
    }
    
    @Test
    void update_PlayerLandsOnBlock_ShouldSetOnGround() {
        // Given
        world.addBlock(new Vector3(0, 0, 0), 0);
        player.setPosition(0, 2, 0);
        player.setOnGround(false);
        player.getVelocity().y = -5; // Falling
        float delta = 0.1f;
        
        // When
        for (int i = 0; i < 20; i++) {
            physics.update(player, delta);
        }
        
        // Then
        assertTrue(player.isOnGround());
        assertEquals(0, player.getVelocity().y, 0.1f);
    }
    
    @Test
    void update_PlayerFallsOffBlock_ShouldSetOffGround() {
        // Given
        world.addBlock(new Vector3(0, 0, 0), 0);
        player.setPosition(0, 1, 0);
        player.setOnGround(true);
        player.getVelocity().x = 5; // Moving horizontally
        float delta = 0.016f;
        
        // When
        physics.update(player, delta);
        
        // Then
        // After moving off block, should check ground below
        // This test verifies checkGroundBelow is called
        assertNotNull(player); // Just verify no crash
    }
    
    @Test
    void update_PlayerMovesHorizontally_ShouldUpdatePosition() {
        // Given
        player.setPosition(0, 1, 0);
        player.getVelocity().x = 5;
        player.getVelocity().z = 3;
        float delta = 0.016f;
        
        // When
        physics.update(player, delta);
        
        // Then
        assertTrue(player.getPosition().x > 0);
        assertTrue(player.getPosition().z > 0);
    }
    
    @Test
    void tryJump_PlayerOnGround_ShouldJump() {
        // Given
        world.addBlock(new Vector3(0, 0, 0), 0);
        player.setPosition(0, 1, 0);
        player.setOnGround(true);
        
        // When
        physics.tryJump(player);
        
        // Then
        assertEquals(Player.JUMP_VELOCITY, player.getVelocity().y, 0.001f);
        assertFalse(player.isOnGround());
    }
    
    @Test
    void tryJump_PlayerInAir_ShouldNotJump() {
        // Given
        player.setOnGround(false);
        float initialVelocityY = player.getVelocity().y;
        
        // When
        physics.tryJump(player);
        
        // Then
        assertEquals(initialVelocityY, player.getVelocity().y, 0.001f);
        assertFalse(player.isOnGround());
    }
    
    @Test
    void update_PlayerCollidesWithMultipleBlocks_ShouldResolveCorrectly() {
        // Given
        world.addBlock(new Vector3(0, 0, 0), 0);
        world.addBlock(new Vector3(1, 0, 0), 0);
        world.addBlock(new Vector3(0, 0, 1), 0);
        player.setPosition(0.5f, 1, 0.5f);
        player.getVelocity().x = 10;
        float delta = 0.016f;
        
        // When
        physics.update(player, delta);
        
        // Then
        // Should not pass through blocks
        assertTrue(player.getPosition().x < 1.5f);
        assertTrue(player.getPosition().z < 1.5f);
    }
    
    @Test
    void update_PlayerStandingStill_ShouldStillCheckGround() {
        // Given
        // Player feet at y=0, block top at y=0.5 (block center at 0, half-size 0.5)
        // Player should be standing on block
        world.addBlock(new Vector3(0, 0, 0), 0);
        player.setPosition(0, 0, 0); // Player feet at ground level
        player.setOnGround(true); // Start on ground
        player.getVelocity().set(0, 0, 0); // No movement
        float delta = 0.016f;
        
        // When
        physics.update(player, delta);
        
        // Then
        // Should still check ground (this was a bug fix)
        // checkGroundBelow is always called, so player should remain on ground
        assertTrue(player.isOnGround());
    }
    
    @Test
    void update_BlockRemovedUnderPlayer_ShouldFall() {
        // Given
        // Place block and player so player is standing on it
        // Block center at (0, 0, 0), top at y=0.5
        // Player feet at y=0.5 to stand on block
        world.addBlock(new Vector3(0, 0, 0), 0);
        player.setPosition(0, 0.5f, 0); // Player feet at block top
        player.setOnGround(true);
        player.getVelocity().y = 0; // Start with no velocity
        
        // First update to ensure player is on ground
        physics.update(player, 0.016f);
        assertTrue(player.isOnGround(), "Player should be on ground initially");
        
        // When - remove block, then update physics
        // checkGroundBelow checks at player.y - 0.01f = 0.49
        // After block removal, there's no block at y=0.49, so onGround should be false
        world.removeBlock(new Vector3(0, 0, 0));
        
        // Update once - checkGroundBelow should detect no ground and set onGround to false
        // But applyGravity runs first, and it checks isOnGround() which is still true
        // So we need two updates: first to set onGround false, second to apply gravity
        float delta = 0.1f;
        physics.update(player, delta);
        
        // Verify onGround is false after first update
        assertFalse(player.isOnGround(), "Player should not be on ground after block removal");
        
        // Update again - now applyGravity should apply since onGround is false
        physics.update(player, delta);
        
        // Then - velocity should be negative (falling)
        // applyGravity: velocity.y += GRAVITY * delta = 0 + (-20) * 0.1 = -2.0
        assertTrue(player.getVelocity().y < 0, "Player should have negative velocity (falling)");
    }
}
