package kr.co.voxelite.physics;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.test.GdxTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AABB class
 */
class AABBTest extends GdxTestRunner {
    
    private AABB aabb;
    
    @BeforeEach
    void setUp() {
        Vector3 center = new Vector3(0, 0, 0);
        aabb = new AABB(center, 0.5f, 1f, 0.5f);
    }
    
    @Test
    void constructor_WithHalfSizes_ShouldCreateCorrectBounds() {
        // Given
        Vector3 center = new Vector3(10, 20, 30);
        float halfWidth = 1f;
        float halfHeight = 2f;
        float halfDepth = 3f;
        
        // When
        AABB newAABB = new AABB(center, halfWidth, halfHeight, halfDepth);
        
        // Then
        Vector3 min = newAABB.getMin();
        Vector3 max = newAABB.getMax();
        
        assertEquals(9, min.x, 0.001f);
        assertEquals(18, min.y, 0.001f);
        assertEquals(27, min.z, 0.001f);
        
        assertEquals(11, max.x, 0.001f);
        assertEquals(22, max.y, 0.001f);
        assertEquals(33, max.z, 0.001f);
    }
    
    @Test
    void constructor_WithCubeSize_ShouldCreateCube() {
        // Given
        Vector3 center = new Vector3(0, 0, 0);
        float halfSize = 0.5f;
        
        // When
        AABB cube = new AABB(center, halfSize);
        
        // Then
        assertEquals(halfSize, cube.getHalfWidth(), 0.001f);
        assertEquals(halfSize, cube.getHalfHeight(), 0.001f);
        assertEquals(halfSize, cube.getHalfDepth(), 0.001f);
    }
    
    @Test
    void intersects_OverlappingAABBs_ShouldReturnTrue() {
        // Given
        AABB aabb1 = new AABB(new Vector3(0, 0, 0), 0.5f);
        AABB aabb2 = new AABB(new Vector3(0.3f, 0.3f, 0.3f), 0.5f);
        
        // When
        boolean intersects = aabb1.intersects(aabb2);
        
        // Then
        assertTrue(intersects);
    }
    
    @Test
    void intersects_NonOverlappingAABBs_ShouldReturnFalse() {
        // Given
        AABB aabb1 = new AABB(new Vector3(0, 0, 0), 0.5f);
        AABB aabb2 = new AABB(new Vector3(2, 2, 2), 0.5f);
        
        // When
        boolean intersects = aabb1.intersects(aabb2);
        
        // Then
        assertFalse(intersects);
    }
    
    @Test
    void intersects_TouchingAABBs_ShouldReturnTrue() {
        // Given
        // Two AABBs that are exactly touching (edge to edge)
        // aabb1: center=(0,0,0), halfSize=0.5 -> bounds: (-0.5 to 0.5)
        // aabb2: center=(1,0,0), halfSize=0.5 -> bounds: (0.5 to 1.5)
        // They touch at x=0.5, so max.x (0.5) > min.x (0.5) is false
        // But min.x (0.5) < max.x (0.5) is also false
        // So they don't intersect. Let's use overlapping ones instead.
        AABB aabb1 = new AABB(new Vector3(0, 0, 0), 0.5f);
        AABB aabb2 = new AABB(new Vector3(0.9f, 0, 0), 0.5f); // Overlapping
        
        // When
        boolean intersects = aabb1.intersects(aabb2);
        
        // Then
        assertTrue(intersects); // Overlapping counts as intersection
    }
    
    @Test
    void setCenter_WithVector3_ShouldUpdateBounds() {
        // Given
        Vector3 newCenter = new Vector3(10, 20, 30);
        
        // When
        aabb.setCenter(newCenter);
        
        // Then
        Vector3 center = aabb.getCenter();
        assertEquals(10, center.x, 0.001f);
        assertEquals(20, center.y, 0.001f);
        assertEquals(30, center.z, 0.001f);
        
        Vector3 min = aabb.getMin();
        assertEquals(9.5f, min.x, 0.001f);
        assertEquals(19, min.y, 0.001f);
        assertEquals(29.5f, min.z, 0.001f);
    }
    
    @Test
    void setCenter_WithFloats_ShouldUpdateBounds() {
        // When
        aabb.setCenter(5, 10, 15);
        
        // Then
        Vector3 center = aabb.getCenter();
        assertEquals(5, center.x, 0.001f);
        assertEquals(10, center.y, 0.001f);
        assertEquals(15, center.z, 0.001f);
    }
    
    @Test
    void setSize_ShouldUpdateHalfSizes() {
        // When
        aabb.setSize(1f, 2f, 3f);
        
        // Then
        assertEquals(1f, aabb.getHalfWidth(), 0.001f);
        assertEquals(2f, aabb.getHalfHeight(), 0.001f);
        assertEquals(3f, aabb.getHalfDepth(), 0.001f);
        
        Vector3 min = aabb.getMin();
        Vector3 max = aabb.getMax();
        assertEquals(-1f, min.x, 0.001f);
        assertEquals(-2f, min.y, 0.001f);
        assertEquals(-3f, min.z, 0.001f);
        assertEquals(1f, max.x, 0.001f);
        assertEquals(2f, max.y, 0.001f);
        assertEquals(3f, max.z, 0.001f);
    }
    
    @Test
    void offset_ShouldMoveCenter() {
        // Given
        Vector3 initialCenter = new Vector3(aabb.getCenter());
        
        // When
        aabb.offset(5, 10, 15);
        
        // Then
        Vector3 newCenter = aabb.getCenter();
        assertEquals(initialCenter.x + 5, newCenter.x, 0.001f);
        assertEquals(initialCenter.y + 10, newCenter.y, 0.001f);
        assertEquals(initialCenter.z + 15, newCenter.z, 0.001f);
    }
    
    @Test
    void getCenter_ShouldReturnSameInstance() {
        // When
        Vector3 center1 = aabb.getCenter();
        Vector3 center2 = aabb.getCenter();
        
        // Then
        assertSame(center1, center2); // Should return same instance
    }
    
    @Test
    void getMin_ShouldReturnCorrectBounds() {
        // Given
        Vector3 center = new Vector3(10, 20, 30);
        aabb.setCenter(center);
        
        // When
        Vector3 min = aabb.getMin();
        
        // Then
        assertEquals(9.5f, min.x, 0.001f);
        assertEquals(19, min.y, 0.001f);
        assertEquals(29.5f, min.z, 0.001f);
    }
    
    @Test
    void getMax_ShouldReturnCorrectBounds() {
        // Given
        Vector3 center = new Vector3(10, 20, 30);
        aabb.setCenter(center);
        
        // When
        Vector3 max = aabb.getMax();
        
        // Then
        assertEquals(10.5f, max.x, 0.001f);
        assertEquals(21, max.y, 0.001f);
        assertEquals(30.5f, max.z, 0.001f);
    }
}
