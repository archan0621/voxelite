package kr.co.voxelite.entity;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.physics.AABB;
import kr.co.voxelite.test.GdxTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Player class
 */
class PlayerTest extends GdxTestRunner {
    
    private Player player;
    
    @BeforeEach
    void setUp() {
        Vector3 startPos = new Vector3(0, 0, 0);
        player = new Player(startPos);
    }
    
    @Test
    void constructor_ShouldInitializeWithStartPosition() {
        // Given
        Vector3 startPos = new Vector3(10, 20, 30);
        
        // When
        Player newPlayer = new Player(startPos);
        
        // Then
        Vector3 actualPos = newPlayer.getPosition();
        assertEquals(10, actualPos.x, 0.001f);
        assertEquals(20, actualPos.y, 0.001f);
        assertEquals(30, actualPos.z, 0.001f);
    }
    
    @Test
    void constructor_ShouldInitializeOnGround() {
        // Then
        assertTrue(player.isOnGround());
    }
    
    @Test
    void constructor_ShouldInitializeVelocityToZero() {
        // Then
        Vector3 velocity = player.getVelocity();
        assertEquals(0, velocity.x, 0.001f);
        assertEquals(0, velocity.y, 0.001f);
        assertEquals(0, velocity.z, 0.001f);
    }
    
    @Test
    void setPosition_WithVector3_ShouldUpdatePosition() {
        // Given
        Vector3 newPos = new Vector3(5, 10, 15);
        
        // When
        player.setPosition(newPos);
        
        // Then
        Vector3 actualPos = player.getPosition();
        assertEquals(5, actualPos.x, 0.001f);
        assertEquals(10, actualPos.y, 0.001f);
        assertEquals(15, actualPos.z, 0.001f);
    }
    
    @Test
    void setPosition_WithFloats_ShouldUpdatePosition() {
        // When
        player.setPosition(7, 8, 9);
        
        // Then
        Vector3 actualPos = player.getPosition();
        assertEquals(7, actualPos.x, 0.001f);
        assertEquals(8, actualPos.y, 0.001f);
        assertEquals(9, actualPos.z, 0.001f);
    }
    
    @Test
    void setPosition_ShouldUpdateAABB() {
        // Given
        Vector3 newPos = new Vector3(10, 20, 30);
        
        // When
        player.setPosition(newPos);
        
        // Then
        AABB aabb = player.getAABB();
        Vector3 center = aabb.getCenter();
        assertEquals(10, center.x, 0.001f);
        assertEquals(20 + Player.HEIGHT / 2f, center.y, 0.001f);
        assertEquals(30, center.z, 0.001f);
    }
    
    @Test
    void getEyePosition_ShouldReturnCorrectEyeHeight() {
        // Given
        Vector3 pos = new Vector3(0, 0, 0);
        player.setPosition(pos);
        
        // When
        Vector3 eyePos = player.getEyePosition();
        
        // Then
        assertEquals(0, eyePos.x, 0.001f);
        assertEquals(Player.EYE_HEIGHT, eyePos.y, 0.001f);
        assertEquals(0, eyePos.z, 0.001f);
    }
    
    @Test
    void getEyePosition_ShouldReturnNewVector() {
        // When
        Vector3 eyePos1 = player.getEyePosition();
        Vector3 eyePos2 = player.getEyePosition();
        
        // Then
        assertNotSame(eyePos1, eyePos2); // Should be different instances
        assertEquals(eyePos1.x, eyePos2.x, 0.001f);
        assertEquals(eyePos1.y, eyePos2.y, 0.001f);
        assertEquals(eyePos1.z, eyePos2.z, 0.001f);
    }
    
    @Test
    void setOnGround_ShouldUpdateGroundState() {
        // When
        player.setOnGround(false);
        
        // Then
        assertFalse(player.isOnGround());
        
        // When
        player.setOnGround(true);
        
        // Then
        assertTrue(player.isOnGround());
    }
    
    @Test
    void getAABB_ShouldReturnCorrectDimensions() {
        // When
        AABB aabb = player.getAABB();
        
        // Then
        assertEquals(Player.WIDTH / 2f, aabb.getHalfWidth(), 0.001f);
        assertEquals(Player.HEIGHT / 2f, aabb.getHalfHeight(), 0.001f);
        assertEquals(Player.WIDTH / 2f, aabb.getHalfDepth(), 0.001f);
    }
    
    @Test
    void constants_ShouldHaveCorrectValues() {
        // Then
        assertEquals(0.6f, Player.WIDTH);
        assertEquals(1.8f, Player.HEIGHT);
        assertEquals(1.62f, Player.EYE_HEIGHT);
        assertEquals(-20f, Player.GRAVITY);
        assertEquals(7f, Player.JUMP_VELOCITY);
        assertEquals(-50f, Player.TERMINAL_VELOCITY);
    }
    
    @Test
    void getVelocity_ShouldReturnMutableVelocity() {
        // Given
        Vector3 velocity = player.getVelocity();
        
        // When
        velocity.x = 5f;
        velocity.y = 10f;
        velocity.z = 15f;
        
        // Then
        Vector3 actualVelocity = player.getVelocity();
        assertEquals(5f, actualVelocity.x, 0.001f);
        assertEquals(10f, actualVelocity.y, 0.001f);
        assertEquals(15f, actualVelocity.z, 0.001f);
    }
}
