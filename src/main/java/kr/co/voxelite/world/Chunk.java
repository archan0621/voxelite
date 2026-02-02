package kr.co.voxelite.world;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

import java.util.*;

/**
 * A chunk represents a 16x16 area of blocks
 */
public class Chunk {
    public static final int CHUNK_SIZE = 16;
    
    private final ChunkCoord coord;
    private final Map<BlockPos, BlockData> blocks; // ✅ BlockPos key -> block
    private ChunkState state = ChunkState.EMPTY;
    private ChunkMesh mesh = null;
    private BoundingBox bounds; // For Frustum Culling
    
    public Chunk(ChunkCoord coord) {
        this.coord = coord;
        this.blocks = new HashMap<>();
        this.bounds = calculateBounds();
    }
    
    /**
     * Calculate chunk bounding box for frustum culling
     */
    private BoundingBox calculateBounds() {
        float worldX = coord.x * CHUNK_SIZE;
        float worldZ = coord.z * CHUNK_SIZE;
        
        // Chunk area (16x16, Y set large enough)
        Vector3 min = new Vector3(worldX, -10f, worldZ);
        Vector3 max = new Vector3(worldX + CHUNK_SIZE, 100f, worldZ + CHUNK_SIZE);
        
        return new BoundingBox(min, max);
    }
    
    /**
     * Get chunk bounding box
     */
    public BoundingBox getBounds() {
        return bounds;
    }
    
    /**
     * State management
     */
    public ChunkState getState() {
        return state;
    }
    
    public void setState(ChunkState state) {
        this.state = state;
    }
    
    public boolean isGenerated() {
        return state.ordinal() >= ChunkState.GENERATED.ordinal();
    }
    
    /**
     * Mesh management
     */
    public ChunkMesh getMesh() {
        return mesh;
    }
    
    public void setMesh(ChunkMesh mesh) {
        this.mesh = mesh;
        if (mesh != null && mesh.hasInstance()) {
            this.state = ChunkState.MESHED;
        }
    }
    
    public boolean hasMesh() {
        return mesh != null && mesh.hasInstance();
    }
    
    /**
     * Add block using local chunk coordinates (0-15)
     * ✅ Modified: Use BlockPos, Y also converted to integer
     */
    public void addBlockLocal(int localX, float worldY, int localZ, int blockType) {
        int blockY = (int) Math.floor(worldY);
        BlockPos pos = new BlockPos(localX, blockY, localZ);
        blocks.put(pos, new BlockData(pos, blockType));
    }
    
    /**
     * Convert local chunk coordinates to world coordinates
     */
    private Vector3 toWorldPos(int localX, float worldY, int localZ) {
        float worldX = coord.x * CHUNK_SIZE + localX;
        float worldZ = coord.z * CHUNK_SIZE + localZ;
        return new Vector3(worldX, worldY, worldZ);
    }
    
    /**
     * Get block at local position
     * ✅ Modified: Use BlockPos
     */
    public BlockData getBlock(int localX, float worldY, int localZ) {
        int blockY = (int) Math.floor(worldY);
        BlockPos pos = new BlockPos(localX, blockY, localZ);
        return blocks.get(pos);
    }
    
    /**
     * Remove block at local position
     * ✅ Modified: Use BlockPos
     */
    public boolean removeBlock(int localX, float worldY, int localZ) {
        int blockY = (int) Math.floor(worldY);
        BlockPos pos = new BlockPos(localX, blockY, localZ);
        return blocks.remove(pos) != null;
    }
    
    /**
     * Add block at world position (must be within chunk bounds)
     */
    public void addBlockWorld(Vector3 worldPos, int blockType) {
        // ✅ Negative coordinate safe: Math.floor → Math.floorMod
        int blockX = (int) Math.floor(worldPos.x);
        int blockZ = (int) Math.floor(worldPos.z);
        int localX = Math.floorMod(blockX, CHUNK_SIZE);
        int localZ = Math.floorMod(blockZ, CHUNK_SIZE);
        
        if (localX >= 0 && localX < CHUNK_SIZE && localZ >= 0 && localZ < CHUNK_SIZE) {
            addBlockLocal(localX, worldPos.y, localZ, blockType);
        }
    }
    
    /**
     * Get all blocks in this chunk
     */
    public Collection<BlockData> getBlocks() {
        return blocks.values();
    }
    
    /**
     * Get snapshot of all block positions (thread-safe)
     * ✅ Prevents ConcurrentModificationException
     */
    public Collection<BlockPos> getBlockPosSnapshot() {
        return new ArrayList<>(blocks.keySet());
    }
    
    /**
     * Get highest terrain Y at center of chunk
     * ✅ Modified: Use BlockPos, integer coordinate comparison
     */
    public float getCenterHeight() {
        int centerX = CHUNK_SIZE / 2;
        int centerZ = CHUNK_SIZE / 2;
        
        int maxY = Integer.MIN_VALUE;
        for (BlockData block : blocks.values()) {
            BlockPos pos = block.pos;
            
            if (pos.x() == centerX && pos.z() == centerZ) {
                if (pos.y() > maxY) {
                    maxY = pos.y();
                }
            }
        }
        
        return maxY == Integer.MIN_VALUE ? -1f : maxY + 1f;
    }
    
    public ChunkCoord getCoord() {
        return coord;
    }
    
    /**
     * Mark chunk as generated (for deserialized chunks)
     */
    public void markAsGenerated() {
        this.state = ChunkState.GENERATED;
    }
    
    /**
     * Check if block exists at local position
     * ✅ Modified: Use BlockPos
     */
    public boolean hasBlockAt(int localX, float worldY, int localZ) {
        if (localX < 0 || localX >= CHUNK_SIZE || localZ < 0 || localZ >= CHUNK_SIZE) {
            return false;
        }
        int blockY = (int) Math.floor(worldY);
        BlockPos pos = new BlockPos(localX, blockY, localZ);
        return blocks.containsKey(pos);
    }
    
    /**
     * Block data (position + type)
     * ✅ Modified: Use integer coordinates
     */
    public static class BlockData {
        public final BlockPos pos;      // ✅ Integer coordinates
        public final int blockType;
        
        public BlockData(BlockPos pos, int blockType) {
            this.pos = pos;
            this.blockType = blockType;
        }
        
        /**
         * Calculate world coordinates for rendering (based on chunk coordinates)
         */
        public Vector3 getWorldPos(ChunkCoord chunkCoord) {
            return new Vector3(
                chunkCoord.x * CHUNK_SIZE + pos.x(),
                pos.y(),
                chunkCoord.z * CHUNK_SIZE + pos.z()
            );
        }
    }
}
