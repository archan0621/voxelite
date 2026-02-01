package kr.co.voxelite.entity;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.test.GdxTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for Player class - covering NaN/Infinity coordinates and boundary cases
 */
class PlayerEdgeCaseTest extends GdxTestRunner {
    
    private Player player;
    
    @BeforeEach
    void setUp() {
        player = new Player(new Vector3(0, 0, 0));
    }
    
    @Test
    void setPosition_NaNCoordinates_ShouldAccept() {
        // Given
        float nan = Float.NaN;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            player.setPosition(nan, nan, nan);
        });
    }
    
    @Test
    void setPosition_InfinityCoordinates_ShouldAccept() {
        // Given
        float infinity = Float.POSITIVE_INFINITY;
        
        // When/Then
        assertDoesNotThrow(() -> {
            player.setPosition(infinity, infinity, infinity);
        });
    }
    
    @Test
    void setPosition_NegativeInfinityCoordinates_ShouldAccept() {
        // Given
        float negInfinity = Float.NEGATIVE_INFINITY;
        
        // When/Then
        assertDoesNotThrow(() -> {
            player.setPosition(negInfinity, negInfinity, negInfinity);
        });
    }
    
    @Test
    void setPosition_NaNVector3_ShouldNotCrash() {
        // Given
        Vector3 nanVector = new Vector3(Float.NaN, Float.NaN, Float.NaN);
        
        // When/Then - Vector3.set() handles NaN, so should not crash
        assertDoesNotThrow(() -> {
            player.setPosition(nanVector);
        });
    }
    
    @Test
    void setPosition_InfinityVector3_ShouldNotCrash() {
        // Given
        Vector3 infinityVector = new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        
        // When/Then
        assertDoesNotThrow(() -> {
            player.setPosition(infinityVector);
        });
    }
    
    @Test
    void getEyePosition_WithNaNPosition_ShouldReturnNaN() {
        // Given
        player.setPosition(Float.NaN, Float.NaN, Float.NaN);
        
        // When
        Vector3 eyePos = player.getEyePosition();
        
        // Then - Should contain NaN values
        assertTrue(Float.isNaN(eyePos.x) || Float.isNaN(eyePos.y) || Float.isNaN(eyePos.z));
    }
    
    @Test
    void getEyePosition_WithInfinityPosition_ShouldReturnInfinity() {
        // Given
        player.setPosition(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        
        // When
        Vector3 eyePos = player.getEyePosition();
        
        // Then - Should contain infinity values
        assertTrue(Float.isInfinite(eyePos.x) || Float.isInfinite(eyePos.y) || Float.isInfinite(eyePos.z));
    }
    
    @Test
    void getVelocity_ShouldAllowNaNValues() {
        // Given
        Vector3 velocity = player.getVelocity();
        
        // When
        velocity.set(Float.NaN, Float.NaN, Float.NaN);
        
        // Then - Should accept NaN
        assertTrue(Float.isNaN(velocity.x));
        assertTrue(Float.isNaN(velocity.y));
        assertTrue(Float.isNaN(velocity.z));
    }
    
    @Test
    void getVelocity_ShouldAllowInfinityValues() {
        // Given
        Vector3 velocity = player.getVelocity();
        
        // When
        velocity.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        
        // Then - Should accept infinity
        assertTrue(Float.isInfinite(velocity.x));
        assertTrue(Float.isInfinite(velocity.y));
        assertTrue(Float.isInfinite(velocity.z));
    }
    
    @Test
    void getAABB_WithNaNPosition_ShouldNotCrash() {
        // Given
        player.setPosition(Float.NaN, Float.NaN, Float.NaN);
        
        // When/Then
        assertDoesNotThrow(() -> {
            player.getAABB();
        });
    }
    
    @Test
    void getAABB_WithInfinityPosition_ShouldNotCrash() {
        // Given
        player.setPosition(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        
        // When/Then
        assertDoesNotThrow(() -> {
            player.getAABB();
        });
    }
    
    @Test
    void setOnGround_WithNaNPosition_ShouldAccept() {
        // Given
        player.setPosition(Float.NaN, Float.NaN, Float.NaN);
        
        // When/Then
        assertDoesNotThrow(() -> {
            player.setOnGround(true);
            player.setOnGround(false);
        });
    }
}
