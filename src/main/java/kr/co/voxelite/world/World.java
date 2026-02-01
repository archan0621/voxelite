package kr.co.voxelite.world;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * World management class
 */
public class World {
    private BlockManager blockManager;
    private List<Vector3> blockPositions;
    private List<Integer> blockTypes; // Store block types
    private List<ModelInstance> allBlockInstances;
    
    // Performance optimization: O(1) block lookup
    private Map<String, Integer> blockIndexMap;

    public World(BlockManager blockManager) {
        this.blockManager = blockManager;
        this.blockPositions = new ArrayList<>();
        this.blockTypes = new ArrayList<>();
        this.allBlockInstances = new ArrayList<>();
        this.blockIndexMap = new HashMap<>();
    }
    
    /**
     * Create position key for HashMap lookup
     */
    private String positionKey(Vector3 position) {
        return Math.round(position.x) + "," + Math.round(position.y) + "," + Math.round(position.z);
    }

    /**
     * Clear all blocks from the world
     */
    public void clear() {
        blockPositions.clear();
        blockTypes.clear();
        allBlockInstances.clear();
        blockIndexMap.clear();
    }

    /**
     * Add a block at the specified position with block type
     */
    public void addBlock(Vector3 position, int blockType) {
        String key = positionKey(position);
        if (blockIndexMap.containsKey(key)) {
            return; // Block already exists
        }
        
        int index = blockPositions.size();
        blockPositions.add(new Vector3(position));
        blockTypes.add(blockType);
        blockIndexMap.put(key, index);
        
        List<ModelInstance> instances = blockManager.createBlockInstances(position, blockType);
        allBlockInstances.addAll(instances);
    }
    
    /**
     * Add a block at the specified position (default blockType = 0)
     */
    public void addBlock(Vector3 position) {
        addBlock(position, 0);
    }
    
    /**
     * Remove a block at the specified position
     * Optimized with HashMap for O(1) lookup
     * @return true if block was removed, false if no block exists at position
     */
    public boolean removeBlock(Vector3 position) {
        if (position == null) {
            return false;
        }
        
        String key = positionKey(position);
        Integer blockIndex = blockIndexMap.get(key);
        
        if (blockIndex == null) {
            return false;
        }
        
        // Remove from lists
        blockPositions.remove((int) blockIndex);
        blockTypes.remove((int) blockIndex);
        
        // Update indices in map for all blocks after this one
        blockIndexMap.remove(key);
        for (Map.Entry<String, Integer> entry : blockIndexMap.entrySet()) {
            if (entry.getValue() > blockIndex) {
                entry.setValue(entry.getValue() - 1);
            }
        }
        
        // Remove 6 faces per block
        int instanceStartIndex = blockIndex * 6;
        if (instanceStartIndex < allBlockInstances.size()) {
            for (int i = 0; i < 6 && instanceStartIndex < allBlockInstances.size(); i++) {
                allBlockInstances.remove(instanceStartIndex);
            }
        }
        
        return true;
    }
    
    /**
     * Get block type at position
     * @return block type or -1 if no block exists
     */
    public int getBlockType(Vector3 position) {
        if (position == null) {
            return -1;
        }
        
        String key = positionKey(position);
        Integer blockIndex = blockIndexMap.get(key);
        
        if (blockIndex == null || blockIndex >= blockTypes.size()) {
            return -1;
        }
        
        return blockTypes.get(blockIndex);
    }
    
    /**
     * Add flat ground as a grid with block type
     */
    public void addFlatGround(int gridSize, float blockSpacing, float yPosition, int blockType) {
        float offset = (gridSize - 1) * blockSpacing * 0.5f;

        for (int x = 0; x < gridSize; x++) {
            for (int z = 0; z < gridSize; z++) {
                float blockX = x * blockSpacing - offset;
                float blockZ = z * blockSpacing - offset;

                Vector3 blockPosition = new Vector3(blockX, yPosition, blockZ);
                addBlock(blockPosition, blockType);
            }
        }
    }
    
    /**
     * Add flat ground as a grid (default blockType = 0)
     */
    public void addFlatGround(int gridSize, float blockSpacing, float yPosition) {
        addFlatGround(gridSize, blockSpacing, yPosition, 0);
    }

    /**
     * Add staircase ground with block type
     */
    public void addStaircaseGround(int gridSize, float blockSpacing, float blockHeight, int blockType) {
        float offset = (gridSize - 1) * blockSpacing * 0.5f;

        for (int x = 0; x < gridSize; x++) {
            for (int z = 0; z < gridSize; z++) {
                float blockX = x * blockSpacing - offset;
                float blockZ = z * blockSpacing - offset;
                float blockY = (x + z) * blockHeight;

                Vector3 blockPosition = new Vector3(blockX, blockY, blockZ);
                addBlock(blockPosition, blockType);
            }
        }
    }
    
    /**
     * Add staircase ground (default blockType = 0)
     */
    public void addStaircaseGround(int gridSize, float blockSpacing, float blockHeight) {
        addStaircaseGround(gridSize, blockSpacing, blockHeight, 0);
    }

    /**
     * Create flat ground (clears existing blocks first)
     */
    public void createFlatGround(int gridSize, float blockSpacing, float yPosition, int blockType) {
        clear();
        addFlatGround(gridSize, blockSpacing, yPosition, blockType);
    }
    
    /**
     * Create flat ground (clears existing blocks first, default blockType = 0)
     */
    public void createFlatGround(int gridSize, float blockSpacing, float yPosition) {
        createFlatGround(gridSize, blockSpacing, yPosition, 0);
    }

    /**
     * Create staircase ground (clears existing blocks first)
     */
    public void createStaircaseGround(int gridSize, float blockSpacing, float blockHeight, int blockType) {
        clear();
        addStaircaseGround(gridSize, blockSpacing, blockHeight, blockType);
    }
    
    /**
     * Create staircase ground (clears existing blocks first, default blockType = 0)
     */
    public void createStaircaseGround(int gridSize, float blockSpacing, float blockHeight) {
        createStaircaseGround(gridSize, blockSpacing, blockHeight, 0);
    }

    public List<Vector3> getBlockPositions() {
        return blockPositions;
    }

    public List<ModelInstance> getAllBlockInstances() {
        return allBlockInstances;
    }

    public void dispose() {
        if (blockManager != null) {
            blockManager.dispose();
        }
    }
}
