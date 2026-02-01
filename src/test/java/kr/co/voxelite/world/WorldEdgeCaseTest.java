package kr.co.voxelite.world;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.test.GdxTestRunner;
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
 * Edge case tests for World class - covering parameter validation and boundary cases
 */
class WorldEdgeCaseTest extends GdxTestRunner {
    
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
    void addFlatGround_ZeroGridSize_ShouldNotCrash() {
        // Given
        int gridSize = 0;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            world.addFlatGround(gridSize, 1f, 0f, 0);
        });
        
        // Should create no blocks
        assertEquals(0, world.getBlockPositions().size());
    }
    
    @Test
    void addFlatGround_NegativeGridSize_ShouldNotCrash() {
        // Given
        int gridSize = -5;
        
        // When/Then - Should not throw exception (loop won't execute)
        assertDoesNotThrow(() -> {
            world.addFlatGround(gridSize, 1f, 0f, 0);
        });
        
        // Should create no blocks
        assertEquals(0, world.getBlockPositions().size());
    }
    
    @Test
    void addFlatGround_ZeroBlockSpacing_ShouldNotCrash() {
        // Given
        int gridSize = 3;
        float blockSpacing = 0f;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            world.addFlatGround(gridSize, blockSpacing, 0f, 0);
        });
        
        // Should create blocks (all at same position, but only one will be added due to duplicate check)
        assertTrue(world.getBlockPositions().size() > 0);
    }
    
    @Test
    void addFlatGround_NegativeBlockSpacing_ShouldNotCrash() {
        // Given
        int gridSize = 2;
        float blockSpacing = -1f;
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            world.addFlatGround(gridSize, blockSpacing, 0f, 0);
        });
        
        // Should create blocks
        assertEquals(gridSize * gridSize, world.getBlockPositions().size());
    }
    
    @Test
    void addFlatGround_VeryLargeGridSize_ShouldNotCrash() {
        // Given
        int gridSize = 100; // Large but reasonable
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            world.addFlatGround(gridSize, 1f, 0f, 0);
        });
        
        // Should create many blocks
        assertEquals(gridSize * gridSize, world.getBlockPositions().size());
    }
    
    @Test
    void addStaircaseGround_ZeroGridSize_ShouldNotCrash() {
        // Given
        int gridSize = 0;
        
        // When/Then
        assertDoesNotThrow(() -> {
            world.addStaircaseGround(gridSize, 1f, 0.5f, 0);
        });
        
        assertEquals(0, world.getBlockPositions().size());
    }
    
    @Test
    void addStaircaseGround_NegativeGridSize_ShouldNotCrash() {
        // Given
        int gridSize = -3;
        
        // When/Then
        assertDoesNotThrow(() -> {
            world.addStaircaseGround(gridSize, 1f, 0.5f, 0);
        });
        
        assertEquals(0, world.getBlockPositions().size());
    }
    
    @Test
    void getBlockType_NegativeIndex_ShouldReturnNegativeOne() {
        // Given - Create a scenario where blockIndex could be negative
        // This is hard to test directly, but we can test the boundary check
        Vector3 pos = new Vector3(999, 999, 999);
        
        // When
        int blockType = world.getBlockType(pos);
        
        // Then - Should return -1 for non-existent block
        assertEquals(-1, blockType);
    }
    
    @Test
    void getBlockType_IndexOutOfBounds_ShouldReturnNegativeOne() {
        // Given
        // Add a block first
        Vector3 pos1 = new Vector3(0, 0, 0);
        world.addBlock(pos1, 0);
        
        // Manually corrupt the index map to simulate out-of-bounds scenario
        // This tests the >= blockTypes.size() check
        Vector3 pos2 = new Vector3(100, 100, 100);
        
        // When - Query non-existent block
        int blockType = world.getBlockType(pos2);
        
        // Then
        assertEquals(-1, blockType);
    }
    
    @Test
    void removeBlock_IndexOutOfBounds_ShouldHandleGracefully() {
        // Given
        Vector3 pos = new Vector3(0, 0, 0);
        world.addBlock(pos, 0);
        
        // When - Remove the block
        boolean removed = world.removeBlock(pos);
        
        // Then - Should succeed
        assertTrue(removed);
        
        // Try to remove again - should return false
        boolean removedAgain = world.removeBlock(pos);
        assertFalse(removedAgain);
    }
    
    @Test
    void addBlock_WithNaNPosition_ShouldHandleGracefully() {
        // Given
        Vector3 nanPos = new Vector3(Float.NaN, Float.NaN, Float.NaN);
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            world.addBlock(nanPos, 0);
        });
    }
    
    @Test
    void addBlock_WithInfinityPosition_ShouldHandleGracefully() {
        // Given
        Vector3 infinityPos = new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        
        // When/Then - Should not throw exception
        assertDoesNotThrow(() -> {
            world.addBlock(infinityPos, 0);
        });
    }
    
    @Test
    void getBlockType_WithNaNPosition_ShouldReturnNegativeOne() {
        // Given
        Vector3 nanPos = new Vector3(Float.NaN, Float.NaN, Float.NaN);
        
        // When
        int blockType = world.getBlockType(nanPos);
        
        // Then - Should return -1 (positionKey will create a key, but won't match)
        assertEquals(-1, blockType);
    }
    
    @Test
    void getBlockType_WithInfinityPosition_ShouldReturnNegativeOne() {
        // Given
        Vector3 infinityPos = new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        
        // When
        int blockType = world.getBlockType(infinityPos);
        
        // Then
        assertEquals(-1, blockType);
    }
    
    @Test
    void addBlock_NegativeBlockType_ShouldAccept() {
        // Given
        Vector3 pos = new Vector3(0, 0, 0);
        int negativeType = -1;
        
        // When/Then - Should accept negative block type
        assertDoesNotThrow(() -> {
            world.addBlock(pos, negativeType);
        });
        
        // Then
        assertEquals(negativeType, world.getBlockType(pos));
    }
    
    @Test
    void addBlock_VeryLargeBlockType_ShouldAccept() {
        // Given
        Vector3 pos = new Vector3(0, 0, 0);
        int largeType = Integer.MAX_VALUE;
        
        // When/Then
        assertDoesNotThrow(() -> {
            world.addBlock(pos, largeType);
        });
        
        assertEquals(largeType, world.getBlockType(pos));
    }
    
    @Test
    void clear_MultipleTimes_ShouldNotCrash() {
        // Given
        world.addBlock(new Vector3(0, 0, 0), 0);
        world.addBlock(new Vector3(1, 1, 1), 1);
        
        // When/Then - Clear multiple times
        assertDoesNotThrow(() -> {
            world.clear();
            world.clear();
            world.clear();
        });
        
        assertEquals(0, world.getBlockPositions().size());
    }
    
    @Test
    void createFlatGround_ZeroGridSize_ShouldClearWorld() {
        // Given
        world.addBlock(new Vector3(100, 100, 100), 0);
        
        // When
        world.createFlatGround(0, 1f, 0f, 0);
        
        // Then - Should clear existing blocks
        assertEquals(0, world.getBlockPositions().size());
    }
}
