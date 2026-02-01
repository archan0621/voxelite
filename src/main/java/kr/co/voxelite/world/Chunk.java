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
    private final Map<String, BlockData> blocks; // local position key -> block
    private ChunkState state = ChunkState.EMPTY;
    private ChunkMesh mesh = null;
    private BoundingBox bounds; // Frustum Culling용
    
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
        
        // 청크 영역 (16x16, Y는 충분히 크게)
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
     * 상태 관리
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
     * 메시 관리
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
     */
    public void addBlockLocal(int localX, float worldY, int localZ, int blockType) {
        String key = localX + "," + worldY + "," + localZ;
        Vector3 worldPos = toWorldPos(localX, worldY, localZ);
        blocks.put(key, new BlockData(worldPos, blockType));
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
     */
    public BlockData getBlock(int localX, float worldY, int localZ) {
        String key = localX + "," + worldY + "," + localZ;
        return blocks.get(key);
    }
    
    /**
     * Remove block at local position
     */
    public boolean removeBlock(int localX, float worldY, int localZ) {
        String key = localX + "," + worldY + "," + localZ;
        return blocks.remove(key) != null;
    }
    
    /**
     * Add block at world position (must be within chunk bounds)
     */
    public void addBlockWorld(Vector3 worldPos, int blockType) {
        int localX = (int) (worldPos.x - coord.x * CHUNK_SIZE);
        int localZ = (int) (worldPos.z - coord.z * CHUNK_SIZE);
        
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
     * Get highest terrain Y at center of chunk
     */
    public float getCenterHeight() {
        int centerX = CHUNK_SIZE / 2;
        int centerZ = CHUNK_SIZE / 2;
        
        float maxY = -Float.MAX_VALUE;
        for (BlockData block : blocks.values()) {
            Vector3 pos = block.position;
            int localX = (int) (pos.x - coord.x * CHUNK_SIZE);
            int localZ = (int) (pos.z - coord.z * CHUNK_SIZE);
            
            if (localX == centerX && localZ == centerZ) {
                if (pos.y > maxY) {
                    maxY = pos.y;
                }
            }
        }
        
        return maxY == -Float.MAX_VALUE ? -1f : maxY + 1f;
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
     */
    public boolean hasBlockAt(int localX, float worldY, int localZ) {
        if (localX < 0 || localX >= CHUNK_SIZE || localZ < 0 || localZ >= CHUNK_SIZE) {
            return false;
        }
        String key = localX + "," + worldY + "," + localZ;
        return blocks.containsKey(key);
    }
    
    /**
     * Block data (position + type)
     */
    public static class BlockData {
        public final Vector3 position;
        public final int blockType;
        
        public BlockData(Vector3 position, int blockType) {
            this.position = position;
            this.blockType = blockType;
        }
    }
}
