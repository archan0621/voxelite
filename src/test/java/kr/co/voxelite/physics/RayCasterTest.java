package kr.co.voxelite.physics;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
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
 * Unit tests for RayCaster class
 */
class RayCasterTest extends GdxTestRunner {
    
    private World world;
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
    }
    
    @Test
    void raycast_ShouldFindBlockInPath() {
        // Given
        world.addBlock(new Vector3(0, 0, 5), 0);
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, 1));
        
        // When
        Vector3 result = RayCaster.raycast(ray, world);
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.x, 0.1f);
        assertEquals(0, result.y, 0.1f);
        assertEquals(5, result.z, 0.1f);
    }
    
    @Test
    void raycast_NoBlockInPath_ShouldReturnNull() {
        // Given
        world.addBlock(new Vector3(10, 10, 10), 0);
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, 1));
        
        // When
        Vector3 result = RayCaster.raycast(ray, world);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void raycast_EmptyWorld_ShouldReturnNull() {
        // Given
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, 1));
        
        // When
        Vector3 result = RayCaster.raycast(ray, world);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void raycastWithFace_ShouldReturnRaycastHit() {
        // Given
        world.addBlock(new Vector3(0, 0, 5), 0);
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, 1));
        
        // When
        RaycastHit hit = RayCaster.raycastWithFace(ray, world);
        
        // Then
        assertNotNull(hit);
        assertNotNull(hit.getBlockPosition());
        assertNotNull(hit.getNormal());
    }
    
    @Test
    void raycastWithFace_NoBlock_ShouldReturnNull() {
        // Given
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, 1));
        
        // When
        RaycastHit hit = RayCaster.raycastWithFace(ray, world);
        
        // Then
        assertNull(hit);
    }
    
    @Test
    void raycastWithFace_TopFace_ShouldReturnUpNormal() {
        // Given
        world.addBlock(new Vector3(0, 5, 0), 0);
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 1, 0)); // Ray going up
        
        // When
        RaycastHit hit = RayCaster.raycastWithFace(ray, world);
        
        // Then
        assertNotNull(hit);
        Vector3 normal = hit.getNormal();
        // Should hit bottom face of block (normal pointing down from block's perspective)
        // But from ray's perspective, it's hitting upward
        assertTrue(Math.abs(normal.y) > Math.abs(normal.x) && Math.abs(normal.y) > Math.abs(normal.z));
    }
    
    @Test
    void raycastWithFace_BottomFace_ShouldReturnDownNormal() {
        // Given
        world.addBlock(new Vector3(0, -5, 0), 0);
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, -1, 0)); // Ray going down
        
        // When
        RaycastHit hit = RayCaster.raycastWithFace(ray, world);
        
        // Then
        assertNotNull(hit);
        Vector3 normal = hit.getNormal();
        assertTrue(Math.abs(normal.y) > Math.abs(normal.x) && Math.abs(normal.y) > Math.abs(normal.z));
    }
    
    @Test
    void raycastWithFace_FrontFace_ShouldReturnForwardNormal() {
        // Given
        world.addBlock(new Vector3(0, 0, 5), 0);
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, 1)); // Ray going forward
        
        // When
        RaycastHit hit = RayCaster.raycastWithFace(ray, world);
        
        // Then
        assertNotNull(hit);
        Vector3 normal = hit.getNormal();
        // Should hit back face (z-), so normal should point backward
        assertTrue(Math.abs(normal.z) > Math.abs(normal.x) && Math.abs(normal.z) > Math.abs(normal.y));
    }
    
    @Test
    void raycastWithFace_BackFace_ShouldReturnBackwardNormal() {
        // Given
        world.addBlock(new Vector3(0, 0, -5), 0);
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, -1)); // Ray going backward
        
        // When
        RaycastHit hit = RayCaster.raycastWithFace(ray, world);
        
        // Then
        assertNotNull(hit);
        Vector3 normal = hit.getNormal();
        assertTrue(Math.abs(normal.z) > Math.abs(normal.x) && Math.abs(normal.z) > Math.abs(normal.y));
    }
    
    @Test
    void raycastWithFace_LeftFace_ShouldReturnLeftNormal() {
        // Given
        world.addBlock(new Vector3(-5, 0, 0), 0);
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(-1, 0, 0)); // Ray going left
        
        // When
        RaycastHit hit = RayCaster.raycastWithFace(ray, world);
        
        // Then
        assertNotNull(hit);
        Vector3 normal = hit.getNormal();
        assertTrue(Math.abs(normal.x) > Math.abs(normal.y) && Math.abs(normal.x) > Math.abs(normal.z));
    }
    
    @Test
    void raycastWithFace_RightFace_ShouldReturnRightNormal() {
        // Given
        world.addBlock(new Vector3(5, 0, 0), 0);
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(1, 0, 0)); // Ray going right
        
        // When
        RaycastHit hit = RayCaster.raycastWithFace(ray, world);
        
        // Then
        assertNotNull(hit);
        Vector3 normal = hit.getNormal();
        assertTrue(Math.abs(normal.x) > Math.abs(normal.y) && Math.abs(normal.x) > Math.abs(normal.z));
    }
    
    @Test
    void raycast_ShouldFindNearestBlock() {
        // Given
        world.addBlock(new Vector3(0, 0, 3), 0);
        world.addBlock(new Vector3(0, 0, 6), 0);
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, 1));
        
        // When
        Vector3 result = RayCaster.raycast(ray, world);
        
        // Then
        assertNotNull(result);
        assertEquals(3, result.z, 0.1f); // Should find nearest block
    }
    
    @Test
    void raycast_MaxDistance_ShouldNotFindDistantBlocks() {
        // Given
        world.addBlock(new Vector3(0, 0, 20), 0); // Beyond max distance (10)
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, 1));
        
        // When
        Vector3 result = RayCaster.raycast(ray, world);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void raycast_DiagonalRay_ShouldFindBlock() {
        // Given
        world.addBlock(new Vector3(5, 5, 5), 0);
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(1, 1, 1).nor());
        
        // When
        Vector3 result = RayCaster.raycast(ray, world);
        
        // Then
        assertNotNull(result);
        assertEquals(5, result.x, 0.5f);
        assertEquals(5, result.y, 0.5f);
        assertEquals(5, result.z, 0.5f);
    }
}
