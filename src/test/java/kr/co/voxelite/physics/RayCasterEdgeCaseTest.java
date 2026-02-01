package kr.co.voxelite.physics;

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
 * Edge case tests for RayCaster - covering NaN/Infinity coordinates and boundary cases
 */
class RayCasterEdgeCaseTest extends GdxTestRunner {
    
    private World world;
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
    }
    
    @Test
    void raycast_WithNaNOrigin_ShouldNotCrash() {
        // Given
        Vector3 nanOrigin = new Vector3(Float.NaN, Float.NaN, Float.NaN);
        Vector3 direction = new Vector3(0, 0, 1);
        Ray ray = new Ray(nanOrigin, direction);
        
        // When/Then
        assertDoesNotThrow(() -> {
            Vector3 result = RayCaster.raycast(ray, world);
            // Result may be null or unpredictable, but should not crash
        });
    }
    
    @Test
    void raycast_WithInfinityOrigin_ShouldNotCrash() {
        // Given
        Vector3 infinityOrigin = new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector3 direction = new Vector3(0, 0, 1);
        Ray ray = new Ray(infinityOrigin, direction);
        
        // When/Then
        assertDoesNotThrow(() -> {
            Vector3 result = RayCaster.raycast(ray, world);
        });
    }
    
    @Test
    void raycast_WithNaNDirection_ShouldNotCrash() {
        // Given
        Vector3 origin = new Vector3(0, 0, 0);
        Vector3 nanDirection = new Vector3(Float.NaN, Float.NaN, Float.NaN);
        Ray ray = new Ray(origin, nanDirection);
        
        // When/Then
        assertDoesNotThrow(() -> {
            Vector3 result = RayCaster.raycast(ray, world);
        });
    }
    
    @Test
    void raycast_WithInfinityDirection_ShouldNotCrash() {
        // Given
        Vector3 origin = new Vector3(0, 0, 0);
        Vector3 infinityDirection = new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Ray ray = new Ray(origin, infinityDirection);
        
        // When/Then
        assertDoesNotThrow(() -> {
            Vector3 result = RayCaster.raycast(ray, world);
        });
    }
    
    @Test
    void raycast_WithZeroDirection_ShouldNotCrash() {
        // Given
        Vector3 origin = new Vector3(0, 0, 0);
        Vector3 zeroDirection = new Vector3(0, 0, 0);
        Ray ray = new Ray(origin, zeroDirection);
        
        // When/Then
        assertDoesNotThrow(() -> {
            Vector3 result = RayCaster.raycast(ray, world);
        });
    }
    
    @Test
    void raycastWithFace_WithNaNOrigin_ShouldNotCrash() {
        // Given
        Vector3 nanOrigin = new Vector3(Float.NaN, Float.NaN, Float.NaN);
        Vector3 direction = new Vector3(0, 0, 1);
        Ray ray = new Ray(nanOrigin, direction);
        
        // When/Then
        assertDoesNotThrow(() -> {
            RaycastHit hit = RayCaster.raycastWithFace(ray, world);
        });
    }
    
    @Test
    void raycastWithFace_WithInfinityOrigin_ShouldNotCrash() {
        // Given
        Vector3 infinityOrigin = new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector3 direction = new Vector3(0, 0, 1);
        Ray ray = new Ray(infinityOrigin, direction);
        
        // When/Then
        assertDoesNotThrow(() -> {
            RaycastHit hit = RayCaster.raycastWithFace(ray, world);
        });
    }
    
    @Test
    void raycast_BlockWithNaNPosition_ShouldNotCrash() {
        // Given
        world.addBlock(new Vector3(Float.NaN, Float.NaN, Float.NaN), 0);
        Vector3 origin = new Vector3(0, 0, 0);
        Vector3 direction = new Vector3(0, 0, 1);
        Ray ray = new Ray(origin, direction);
        
        // When/Then
        assertDoesNotThrow(() -> {
            Vector3 result = RayCaster.raycast(ray, world);
        });
    }
    
    @Test
    void raycast_BlockWithInfinityPosition_ShouldNotCrash() {
        // Given
        world.addBlock(new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), 0);
        Vector3 origin = new Vector3(0, 0, 0);
        Vector3 direction = new Vector3(0, 0, 1);
        Ray ray = new Ray(origin, direction);
        
        // When/Then
        assertDoesNotThrow(() -> {
            Vector3 result = RayCaster.raycast(ray, world);
        });
    }
}
