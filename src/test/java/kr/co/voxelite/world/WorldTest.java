package kr.co.voxelite.world;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.test.GdxTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Unit tests for World class
 */
class WorldTest extends GdxTestRunner {
    
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
    void addBlock_ShouldAddBlockAtPosition() {
        // Given
        Vector3 pos = new Vector3(0, 0, 0);
        int blockType = 0;
        
        // When
        world.addBlock(pos, blockType);
        
        // Then
        assertTrue(world.getBlockPositions().contains(pos));
        assertEquals(blockType, world.getBlockType(pos));
        assertEquals(1, world.getBlockPositions().size());
    }
    
    @Test
    void addBlock_WithoutBlockType_ShouldUseDefaultType() {
        // Given
        Vector3 pos = new Vector3(1, 2, 3);
        
        // When
        world.addBlock(pos);
        
        // Then
        assertEquals(0, world.getBlockType(pos));
    }
    
    @Test
    void addBlock_AtSamePosition_ShouldNotAddDuplicate() {
        // Given
        Vector3 pos = new Vector3(0, 0, 0);
        world.addBlock(pos, 0);
        int initialSize = world.getBlockPositions().size();
        
        // When
        world.addBlock(pos, 1); // Try to add at same position with different type
        
        // Then
        assertEquals(initialSize, world.getBlockPositions().size());
        assertEquals(0, world.getBlockType(pos)); // Original type remains
    }
    
    @Test
    void removeBlock_ShouldRemoveBlockAtPosition() {
        // Given
        Vector3 pos = new Vector3(1, 2, 3);
        world.addBlock(pos, 0);
        
        // When
        boolean removed = world.removeBlock(pos);
        
        // Then
        assertTrue(removed);
        assertEquals(-1, world.getBlockType(pos));
        assertEquals(0, world.getBlockPositions().size());
    }
    
    @Test
    void removeBlock_NonExistentBlock_ShouldReturnFalse() {
        // Given
        Vector3 pos = new Vector3(999, 999, 999);
        
        // When
        boolean removed = world.removeBlock(pos);
        
        // Then
        assertFalse(removed);
    }
    
    @Test
    void removeBlock_NullPosition_ShouldReturnFalse() {
        // When
        boolean removed = world.removeBlock(null);
        
        // Then
        assertFalse(removed);
    }
    
    @Test
    void getBlockType_ExistingBlock_ShouldReturnCorrectType() {
        // Given
        Vector3 pos = new Vector3(5, 10, 15);
        int expectedType = 3;
        world.addBlock(pos, expectedType);
        
        // When
        int actualType = world.getBlockType(pos);
        
        // Then
        assertEquals(expectedType, actualType);
    }
    
    @Test
    void getBlockType_NonExistentBlock_ShouldReturnNegativeOne() {
        // Given
        Vector3 pos = new Vector3(999, 999, 999);
        
        // When
        int blockType = world.getBlockType(pos);
        
        // Then
        assertEquals(-1, blockType);
    }
    
    @Test
    void getBlockType_NullPosition_ShouldReturnNegativeOne() {
        // When
        int blockType = world.getBlockType(null);
        
        // Then
        assertEquals(-1, blockType);
    }
    
    @Test
    void clear_ShouldRemoveAllBlocks() {
        // Given
        world.addBlock(new Vector3(0, 0, 0), 0);
        world.addBlock(new Vector3(1, 1, 1), 1);
        world.addBlock(new Vector3(2, 2, 2), 2);
        
        // When
        world.clear();
        
        // Then
        assertEquals(0, world.getBlockPositions().size());
        assertEquals(-1, world.getBlockType(new Vector3(0, 0, 0)));
    }
    
    @Test
    void addFlatGround_ShouldCreateGridOfBlocks() {
        // Given
        int gridSize = 3;
        float blockSpacing = 1f;
        float yPosition = 0f;
        int blockType = 0;
        
        // When
        world.addFlatGround(gridSize, blockSpacing, yPosition, blockType);
        
        // Then
        assertEquals(gridSize * gridSize, world.getBlockPositions().size());
        
        // Check some positions
        assertNotEquals(-1, world.getBlockType(new Vector3(-1, 0, -1)));
        assertNotEquals(-1, world.getBlockType(new Vector3(0, 0, 0)));
        assertNotEquals(-1, world.getBlockType(new Vector3(1, 0, 1)));
    }
    
    @Test
    void addFlatGround_WithoutBlockType_ShouldUseDefaultType() {
        // Given
        int gridSize = 2;
        
        // When
        world.addFlatGround(gridSize, 1f, 0f);
        
        // Then
        List<Vector3> positions = world.getBlockPositions();
        for (Vector3 pos : positions) {
            assertEquals(0, world.getBlockType(pos));
        }
    }
    
    @Test
    void addStaircaseGround_ShouldCreateStaircasePattern() {
        // Given
        int gridSize = 3;
        float blockSpacing = 1f;
        float blockHeight = 0.5f;
        int blockType = 1;
        
        // When
        world.addStaircaseGround(gridSize, blockSpacing, blockHeight, blockType);
        
        // Then
        assertEquals(gridSize * gridSize, world.getBlockPositions().size());
        
        // Check that blocks have different Y positions
        List<Vector3> positions = world.getBlockPositions();
        boolean hasDifferentY = false;
        float firstY = positions.get(0).y;
        for (Vector3 pos : positions) {
            if (Math.abs(pos.y - firstY) > 0.01f) {
                hasDifferentY = true;
                break;
            }
        }
        assertTrue(hasDifferentY);
    }
    
    @Test
    void createFlatGround_ShouldClearAndCreateGround() {
        // Given
        world.addBlock(new Vector3(100, 100, 100), 0);
        int gridSize = 2;
        
        // When
        world.createFlatGround(gridSize, 1f, 0f, 0);
        
        // Then
        assertEquals(gridSize * gridSize, world.getBlockPositions().size());
        assertEquals(-1, world.getBlockType(new Vector3(100, 100, 100)));
    }
    
    @Test
    void createStaircaseGround_ShouldClearAndCreateStaircase() {
        // Given
        world.addBlock(new Vector3(200, 200, 200), 0);
        int gridSize = 2;
        
        // When
        world.createStaircaseGround(gridSize, 1f, 0.5f, 0);
        
        // Then
        assertEquals(gridSize * gridSize, world.getBlockPositions().size());
        assertEquals(-1, world.getBlockType(new Vector3(200, 200, 200)));
    }
    
    @Test
    void removeBlock_ShouldUpdateIndicesCorrectly() {
        // Given
        Vector3 pos1 = new Vector3(0, 0, 0);
        Vector3 pos2 = new Vector3(1, 0, 0);
        Vector3 pos3 = new Vector3(2, 0, 0);
        
        world.addBlock(pos1, 0);
        world.addBlock(pos2, 1);
        world.addBlock(pos3, 2);
        
        // When
        world.removeBlock(pos2); // Remove middle block
        
        // Then
        assertEquals(2, world.getBlockPositions().size());
        assertEquals(0, world.getBlockType(pos1));
        assertEquals(2, world.getBlockType(pos3));
        assertEquals(-1, world.getBlockType(pos2));
    }
    
    @Test
    void positionKey_Rounding_ShouldHandleFloatPositions() {
        // Given
        // Both positions round to (0, 1, 1) due to Math.round()
        Vector3 pos1 = new Vector3(0.3f, 0.7f, 0.9f);
        Vector3 pos2 = new Vector3(0.6f, 0.4f, 0.1f);
        
        // When
        world.addBlock(pos1, 0);
        world.addBlock(pos2, 1);
        
        // Then
        // Both round to (0, 1, 1), so should be treated as same position
        // Math.round(0.3) = 0, Math.round(0.7) = 1, Math.round(0.9) = 1
        // Math.round(0.6) = 1, Math.round(0.4) = 0, Math.round(0.1) = 0
        // Actually they round to different positions: (0,1,1) vs (1,0,0)
        // So this test should verify they are different
        assertEquals(2, world.getBlockPositions().size());
    }
}
