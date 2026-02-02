package kr.co.voxelite.world;

import com.badlogic.gdx.math.Vector3;
import java.util.*;

/**
 * Greedy Meshing algorithm implementation
 * 
 * Principle:
 * 1. Process each direction (6 faces) independently
 * 2. Merge adjacent faces with same block type + same visibility
 * 3. Rectangle expansion: expand right → expand down
 */
public class GreedyMeshBuilder {
    
    /**
     * Merged face information
     */
    public static class MergedQuad {
        public final Vector3 origin;      // Start position
        public final int width;            // Width in X or Z direction
        public final int height;           // Height in Y direction
        public final int blockType;        // Block type
        public final int direction;        // Direction (0~5)
        
        public MergedQuad(Vector3 origin, int width, int height, int blockType, int direction) {
            this.origin = origin;
            this.width = width;
            this.height = height;
            this.blockType = blockType;
            this.direction = direction;
        }
    }
    
    /**
     * Converts block data to 3D array
     */
    private static class VoxelGrid {
        private final Map<String, BlockInfo> grid;
        private final int minX, minY, minZ;
        private final int maxX, maxY, maxZ;
        
        static class BlockInfo {
            final int blockType;
            final boolean[] visibleFaces;
            
            BlockInfo(int blockType, boolean[] visibleFaces) {
                this.blockType = blockType;
                this.visibleFaces = visibleFaces;
            }
        }
        
        public VoxelGrid(List<BlockManager.BlockData> blocks, Map<Vector3, boolean[]> visibleFacesMap) {
            this.grid = new HashMap<>();
            
            // Calculate bounds
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            
            for (BlockManager.BlockData block : blocks) {
                Vector3 pos = block.position;
                int x = (int) pos.x;
                int y = (int) pos.y;
                int z = (int) pos.z;
                
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                maxZ = Math.max(maxZ, z);
                
                boolean[] visibleFaces = visibleFacesMap.get(pos);
                if (visibleFaces != null) {
                    grid.put(key(x, y, z), new BlockInfo(block.blockType, visibleFaces));
                }
            }
            
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
        
        private String key(int x, int y, int z) {
            return x + "," + y + "," + z;
        }
        
        public BlockInfo get(int x, int y, int z) {
            return grid.get(key(x, y, z));
        }
        
        public boolean hasFace(int x, int y, int z, int direction) {
            BlockInfo info = get(x, y, z);
            return info != null && info.visibleFaces[direction];
        }
        
        public int getBlockType(int x, int y, int z) {
            BlockInfo info = get(x, y, z);
            return info != null ? info.blockType : -1;
        }
    }
    
    /**
     * Greedy Meshing main function
     */
    public static List<MergedQuad> buildGreedyMesh(
            List<BlockManager.BlockData> blocks, 
            Map<Vector3, boolean[]> visibleFacesMap) {
        
        if (blocks == null || blocks.isEmpty()) {
            return new ArrayList<>();
        }
        
        VoxelGrid grid = new VoxelGrid(blocks, visibleFacesMap);
        List<MergedQuad> quads = new ArrayList<>();
        
        // Process each direction
        // 0: Front (z+), 1: Back (z-), 2: Left (x-), 3: Right (x+), 4: Top (y+), 5: Bottom (y-)
        quads.addAll(buildFacesForDirection(grid, 0)); // Front
        quads.addAll(buildFacesForDirection(grid, 1)); // Back
        quads.addAll(buildFacesForDirection(grid, 2)); // Left
        quads.addAll(buildFacesForDirection(grid, 3)); // Right
        quads.addAll(buildFacesForDirection(grid, 4)); // Top
        quads.addAll(buildFacesForDirection(grid, 5)); // Bottom
        
        return quads;
    }
    
    /**
     * Merges faces in a specific direction using Greedy Meshing
     */
    private static List<MergedQuad> buildFacesForDirection(VoxelGrid grid, int direction) {
        List<MergedQuad> quads = new ArrayList<>();
        
        // Determine traversal order based on direction
        switch (direction) {
            case 0: // Front (z+)
            case 1: // Back (z-)
                buildFacesXY(grid, direction, quads);
                break;
            case 2: // Left (x-)
            case 3: // Right (x+)
                buildFacesZY(grid, direction, quads);
                break;
            case 4: // Top (y+)
            case 5: // Bottom (y-)
                buildFacesXZ(grid, direction, quads);
                break;
        }
        
        return quads;
    }
    
    /**
     * XY plane (Front/Back direction)
     */
    private static void buildFacesXY(VoxelGrid grid, int direction, List<MergedQuad> quads) {
        boolean[][][] visited = new boolean[grid.maxX - grid.minX + 1]
                                          [grid.maxY - grid.minY + 1]
                                          [grid.maxZ - grid.minZ + 1];
        
        for (int z = grid.minZ; z <= grid.maxZ; z++) {
            for (int y = grid.minY; y <= grid.maxY; y++) {
                for (int x = grid.minX; x <= grid.maxX; x++) {
                    if (visited[x - grid.minX][y - grid.minY][z - grid.minZ]) {
                        continue;
                    }
                    
                    if (!grid.hasFace(x, y, z, direction)) {
                        continue;
                    }
                    
                    int blockType = grid.getBlockType(x, y, z);
                    
                    // X 방향으로 확장
                    int width = 1;
                    while (x + width <= grid.maxX &&
                           !visited[x + width - grid.minX][y - grid.minY][z - grid.minZ] &&
                           grid.hasFace(x + width, y, z, direction) &&
                           grid.getBlockType(x + width, y, z) == blockType) {
                        width++;
                    }
                    
                    // Expand in Y direction
                    int height = 1;
                    boolean canExpandY = true;
                    while (canExpandY && y + height <= grid.maxY) {
                        // Check all blocks within expanded width
                        for (int dx = 0; dx < width; dx++) {
                            if (visited[x + dx - grid.minX][y + height - grid.minY][z - grid.minZ] ||
                                !grid.hasFace(x + dx, y + height, z, direction) ||
                                grid.getBlockType(x + dx, y + height, z) != blockType) {
                                canExpandY = false;
                                break;
                            }
                        }
                        if (canExpandY) {
                            height++;
                        }
                    }
                    
                    // Create merged face
                    Vector3 origin = new Vector3(x, y, z);
                    quads.add(new MergedQuad(origin, width, height, blockType, direction));
                    
                    // Mark as visited
                    for (int dy = 0; dy < height; dy++) {
                        for (int dx = 0; dx < width; dx++) {
                            visited[x + dx - grid.minX][y + dy - grid.minY][z - grid.minZ] = true;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * ZY plane (Left/Right direction)
     */
    private static void buildFacesZY(VoxelGrid grid, int direction, List<MergedQuad> quads) {
        boolean[][][] visited = new boolean[grid.maxX - grid.minX + 1]
                                          [grid.maxY - grid.minY + 1]
                                          [grid.maxZ - grid.minZ + 1];
        
        for (int x = grid.minX; x <= grid.maxX; x++) {
            for (int y = grid.minY; y <= grid.maxY; y++) {
                for (int z = grid.minZ; z <= grid.maxZ; z++) {
                    if (visited[x - grid.minX][y - grid.minY][z - grid.minZ]) {
                        continue;
                    }
                    
                    if (!grid.hasFace(x, y, z, direction)) {
                        continue;
                    }
                    
                    int blockType = grid.getBlockType(x, y, z);
                    
                    // Expand in Z direction
                    int width = 1;
                    while (z + width <= grid.maxZ &&
                           !visited[x - grid.minX][y - grid.minY][z + width - grid.minZ] &&
                           grid.hasFace(x, y, z + width, direction) &&
                           grid.getBlockType(x, y, z + width) == blockType) {
                        width++;
                    }
                    
                    // Expand in Y direction
                    int height = 1;
                    boolean canExpandY = true;
                    while (canExpandY && y + height <= grid.maxY) {
                        for (int dz = 0; dz < width; dz++) {
                            if (visited[x - grid.minX][y + height - grid.minY][z + dz - grid.minZ] ||
                                !grid.hasFace(x, y + height, z + dz, direction) ||
                                grid.getBlockType(x, y + height, z + dz) != blockType) {
                                canExpandY = false;
                                break;
                            }
                        }
                        if (canExpandY) {
                            height++;
                        }
                    }
                    
                    Vector3 origin = new Vector3(x, y, z);
                    quads.add(new MergedQuad(origin, width, height, blockType, direction));
                    
                    for (int dy = 0; dy < height; dy++) {
                        for (int dz = 0; dz < width; dz++) {
                            visited[x - grid.minX][y + dy - grid.minY][z + dz - grid.minZ] = true;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * XZ plane (Top/Bottom direction)
     */
    private static void buildFacesXZ(VoxelGrid grid, int direction, List<MergedQuad> quads) {
        boolean[][][] visited = new boolean[grid.maxX - grid.minX + 1]
                                          [grid.maxY - grid.minY + 1]
                                          [grid.maxZ - grid.minZ + 1];
        
        for (int y = grid.minY; y <= grid.maxY; y++) {
            for (int z = grid.minZ; z <= grid.maxZ; z++) {
                for (int x = grid.minX; x <= grid.maxX; x++) {
                    if (visited[x - grid.minX][y - grid.minY][z - grid.minZ]) {
                        continue;
                    }
                    
                    if (!grid.hasFace(x, y, z, direction)) {
                        continue;
                    }
                    
                    int blockType = grid.getBlockType(x, y, z);
                    
                    // X 방향으로 확장
                    int width = 1;
                    while (x + width <= grid.maxX &&
                           !visited[x + width - grid.minX][y - grid.minY][z - grid.minZ] &&
                           grid.hasFace(x + width, y, z, direction) &&
                           grid.getBlockType(x + width, y, z) == blockType) {
                        width++;
                    }
                    
                    // Expand in Z direction
                    int depth = 1;
                    boolean canExpandZ = true;
                    while (canExpandZ && z + depth <= grid.maxZ) {
                        for (int dx = 0; dx < width; dx++) {
                            if (visited[x + dx - grid.minX][y - grid.minY][z + depth - grid.minZ] ||
                                !grid.hasFace(x + dx, y, z + depth, direction) ||
                                grid.getBlockType(x + dx, y, z + depth) != blockType) {
                                canExpandZ = false;
                                break;
                            }
                        }
                        if (canExpandZ) {
                            depth++;
                        }
                    }
                    
                    Vector3 origin = new Vector3(x, y, z);
                    quads.add(new MergedQuad(origin, width, depth, blockType, direction));
                    
                    for (int dz = 0; dz < depth; dz++) {
                        for (int dx = 0; dx < width; dx++) {
                            visited[x + dx - grid.minX][y - grid.minY][z + dz - grid.minZ] = true;
                        }
                    }
                }
            }
        }
    }
}
