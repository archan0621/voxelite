package kr.co.voxelite.physics;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.test.GdxTestRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for AABB class - covering parameter validation and boundary cases
 */
class AABBEdgeCaseTest extends GdxTestRunner {
    
    @Test
    void constructor_NegativeHalfSize_ShouldAccept() {
        // Given
        Vector3 center = new Vector3(0, 0, 0);
        float negativeHalfSize = -0.5f;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            AABB aabb = new AABB(center, negativeHalfSize);
            assertNotNull(aabb);
        });
    }
    
    @Test
    void constructor_ZeroHalfSize_ShouldAccept() {
        // Given
        Vector3 center = new Vector3(0, 0, 0);
        float zeroHalfSize = 0f;
        
        // When/Then
        assertDoesNotThrow(() -> {
            AABB aabb = new AABB(center, zeroHalfSize);
            assertNotNull(aabb);
        });
    }
    
    @Test
    void constructor_VeryLargeHalfSize_ShouldAccept() {
        // Given
        Vector3 center = new Vector3(0, 0, 0);
        float largeHalfSize = Float.MAX_VALUE / 2f;
        
        // When/Then
        assertDoesNotThrow(() -> {
            AABB aabb = new AABB(center, largeHalfSize);
            assertNotNull(aabb);
        });
    }
    
    @Test
    void constructor_NaNHalfSize_ShouldAccept() {
        // Given
        Vector3 center = new Vector3(0, 0, 0);
        float nanHalfSize = Float.NaN;
        
        // When/Then
        assertDoesNotThrow(() -> {
            AABB aabb = new AABB(center, nanHalfSize);
            assertNotNull(aabb);
        });
    }
    
    @Test
    void constructor_InfinityHalfSize_ShouldAccept() {
        // Given
        Vector3 center = new Vector3(0, 0, 0);
        float infinityHalfSize = Float.POSITIVE_INFINITY;
        
        // When/Then
        assertDoesNotThrow(() -> {
            AABB aabb = new AABB(center, infinityHalfSize);
            assertNotNull(aabb);
        });
    }
    
    @Test
    void constructor_NaNCenter_ShouldAccept() {
        // Given
        Vector3 nanCenter = new Vector3(Float.NaN, Float.NaN, Float.NaN);
        float halfSize = 0.5f;
        
        // When/Then
        assertDoesNotThrow(() -> {
            AABB aabb = new AABB(nanCenter, halfSize);
            assertNotNull(aabb);
        });
    }
    
    @Test
    void constructor_InfinityCenter_ShouldAccept() {
        // Given
        Vector3 infinityCenter = new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        float halfSize = 0.5f;
        
        // When/Then
        assertDoesNotThrow(() -> {
            AABB aabb = new AABB(infinityCenter, halfSize);
            assertNotNull(aabb);
        });
    }
    
    @Test
    void setSize_NegativeValues_ShouldAccept() {
        // Given
        AABB aabb = new AABB(new Vector3(0, 0, 0), 0.5f);
        
        // When/Then
        assertDoesNotThrow(() -> {
            aabb.setSize(-1f, -2f, -3f);
        });
    }
    
    @Test
    void setSize_NaNValues_ShouldAccept() {
        // Given
        AABB aabb = new AABB(new Vector3(0, 0, 0), 0.5f);
        
        // When/Then
        assertDoesNotThrow(() -> {
            aabb.setSize(Float.NaN, Float.NaN, Float.NaN);
        });
    }
    
    @Test
    void setCenter_NaNValues_ShouldAccept() {
        // Given
        AABB aabb = new AABB(new Vector3(0, 0, 0), 0.5f);
        
        // When/Then
        assertDoesNotThrow(() -> {
            aabb.setCenter(Float.NaN, Float.NaN, Float.NaN);
        });
    }
    
    @Test
    void setCenter_InfinityValues_ShouldAccept() {
        // Given
        AABB aabb = new AABB(new Vector3(0, 0, 0), 0.5f);
        
        // When/Then
        assertDoesNotThrow(() -> {
            aabb.setCenter(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        });
    }
    
    @Test
    void intersects_WithNaNValues_ShouldNotCrash() {
        // Given
        AABB aabb1 = new AABB(new Vector3(0, 0, 0), 0.5f);
        AABB aabb2 = new AABB(new Vector3(Float.NaN, Float.NaN, Float.NaN), 0.5f);
        
        // When/Then
        assertDoesNotThrow(() -> {
            boolean intersects = aabb1.intersects(aabb2);
            // Result may be unpredictable, but should not crash
        });
    }
    
    @Test
    void intersects_WithInfinityValues_ShouldNotCrash() {
        // Given
        AABB aabb1 = new AABB(new Vector3(0, 0, 0), 0.5f);
        AABB aabb2 = new AABB(new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), 0.5f);
        
        // When/Then
        assertDoesNotThrow(() -> {
            boolean intersects = aabb1.intersects(aabb2);
        });
    }
    
    @Test
    void offset_WithNaNValues_ShouldNotCrash() {
        // Given
        AABB aabb = new AABB(new Vector3(0, 0, 0), 0.5f);
        
        // When/Then
        assertDoesNotThrow(() -> {
            aabb.offset(Float.NaN, Float.NaN, Float.NaN);
        });
    }
    
    @Test
    void offset_WithInfinityValues_ShouldNotCrash() {
        // Given
        AABB aabb = new AABB(new Vector3(0, 0, 0), 0.5f);
        
        // When/Then
        assertDoesNotThrow(() -> {
            aabb.offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        });
    }
}
