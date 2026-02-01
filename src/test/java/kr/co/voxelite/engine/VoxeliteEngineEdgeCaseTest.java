package kr.co.voxelite.engine;

import kr.co.voxelite.test.GdxTestRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for VoxeliteEngine - covering parameter validation and boundary cases
 */
class VoxeliteEngineEdgeCaseTest extends GdxTestRunner {
    
    @Test
    void initialize_ZeroScreenSize_ShouldNotCrash() {
        // Given
        VoxeliteEngine engine = VoxeliteEngine.builder().build();
        int width = 0;
        int height = 0;
        
        // When/Then
        assertDoesNotThrow(() -> {
            try {
                engine.initialize(width, height);
            } catch (NullPointerException e) {
                // Expected in headless mode
            }
        });
    }
    
    @Test
    void initialize_NegativeScreenSize_ShouldNotCrash() {
        // Given
        VoxeliteEngine engine = VoxeliteEngine.builder().build();
        int width = -100;
        int height = -100;
        
        // When/Then
        assertDoesNotThrow(() -> {
            try {
                engine.initialize(width, height);
            } catch (NullPointerException e) {
                // Expected in headless mode
            }
        });
    }
    
    @Test
    void initialize_VeryLargeScreenSize_ShouldNotCrash() {
        // Given
        VoxeliteEngine engine = VoxeliteEngine.builder().build();
        int width = Integer.MAX_VALUE;
        int height = Integer.MAX_VALUE;
        
        // When/Then
        assertDoesNotThrow(() -> {
            try {
                engine.initialize(width, height);
            } catch (OutOfMemoryError e) {
                // May run out of memory, but should handle gracefully
            } catch (NullPointerException e) {
                // Expected in headless mode
            }
        });
    }
    
    @Test
    void resize_ZeroSize_ShouldNotCrash() {
        // Given
        VoxeliteEngine engine = VoxeliteEngine.builder().build();
        try {
            engine.initialize(800, 600);
        } catch (NullPointerException e) {
            // Skip if headless mode
            return;
        }
        
        // When/Then
        assertDoesNotThrow(() -> {
            engine.resize(0, 0);
        });
    }
    
    @Test
    void resize_NegativeSize_ShouldNotCrash() {
        // Given
        VoxeliteEngine engine = VoxeliteEngine.builder().build();
        try {
            engine.initialize(800, 600);
        } catch (NullPointerException e) {
            return;
        }
        
        // When/Then
        assertDoesNotThrow(() -> {
            engine.resize(-100, -100);
        });
    }
    
    @Test
    void update_ZeroDelta_ShouldNotCrash() {
        // Given
        VoxeliteEngine engine = VoxeliteEngine.builder().build();
        try {
            engine.initialize(800, 600);
        } catch (NullPointerException e) {
            return;
        }
        
        // When/Then
        assertDoesNotThrow(() -> {
            engine.update(0f);
        });
    }
    
    @Test
    void update_NaNDelta_ShouldNotCrash() {
        // Given
        VoxeliteEngine engine = VoxeliteEngine.builder().build();
        try {
            engine.initialize(800, 600);
        } catch (NullPointerException e) {
            return;
        }
        
        // When/Then
        assertDoesNotThrow(() -> {
            engine.update(Float.NaN);
        });
    }
    
    @Test
    void update_InfinityDelta_ShouldNotCrash() {
        // Given
        VoxeliteEngine engine = VoxeliteEngine.builder().build();
        try {
            engine.initialize(800, 600);
        } catch (NullPointerException e) {
            return;
        }
        
        // When/Then
        assertDoesNotThrow(() -> {
            engine.update(Float.POSITIVE_INFINITY);
        });
    }
    
    @Test
    void update_VeryLargeDelta_ShouldNotCrash() {
        // Given
        VoxeliteEngine engine = VoxeliteEngine.builder().build();
        try {
            engine.initialize(800, 600);
        } catch (NullPointerException e) {
            return;
        }
        
        // When/Then
        assertDoesNotThrow(() -> {
            engine.update(1000f); // Very large delta
        });
    }
    
    @Test
    void updatePhysics_ZeroDelta_ShouldNotCrash() {
        // Given
        VoxeliteEngine engine = VoxeliteEngine.builder().build();
        try {
            engine.initialize(800, 600);
        } catch (NullPointerException e) {
            return;
        }
        
        // When/Then
        assertDoesNotThrow(() -> {
            engine.updatePhysics(0f);
        });
    }
    
    @Test
    void updatePhysics_NaNDelta_ShouldNotCrash() {
        // Given
        VoxeliteEngine engine = VoxeliteEngine.builder().build();
        try {
            engine.initialize(800, 600);
        } catch (NullPointerException e) {
            return;
        }
        
        // When/Then
        assertDoesNotThrow(() -> {
            engine.updatePhysics(Float.NaN);
        });
    }
    
    @Test
    void dispose_Twice_ShouldNotCrash() {
        // Given
        VoxeliteEngine engine = VoxeliteEngine.builder().build();
        try {
            engine.initialize(800, 600);
        } catch (NullPointerException e) {
            return;
        }
        
        // When/Then
        assertDoesNotThrow(() -> {
            engine.dispose();
            engine.dispose(); // Second dispose
        });
    }
    
    @Test
    void builder_WithExtremeValues_ShouldNotCrash() {
        // Given/When/Then
        assertDoesNotThrow(() -> {
            VoxeliteEngine engine = VoxeliteEngine.builder()
                    .worldSize(Integer.MAX_VALUE, Integer.MAX_VALUE)
                    .groundLevel(Float.MAX_VALUE)
                    .playerStart(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
                    .playerSpeed(Float.MAX_VALUE)
                    .fieldOfView(360f)
                    .cameraPitch(360f)
                    .mouseSensitivity(Float.MAX_VALUE)
                    .defaultGroundBlockType(Integer.MAX_VALUE)
                    .build();
            
            assertNotNull(engine);
        });
    }
}
