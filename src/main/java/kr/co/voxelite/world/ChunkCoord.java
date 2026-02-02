package kr.co.voxelite.world;

import java.util.Objects;

/**
 * Chunk coordinate (x, z)
 */
public class ChunkCoord {
    public final int x;
    public final int z;
    
    public ChunkCoord(int x, int z) {
        this.x = x;
        this.z = z;
    }
    
    /**
     * Convert world position to chunk coordinate
     */
    public static ChunkCoord fromWorldPos(float worldX, float worldZ, int chunkSize) {
        int chunkX = (int) Math.floor(worldX / chunkSize);
        int chunkZ = (int) Math.floor(worldZ / chunkSize);
        return new ChunkCoord(chunkX, chunkZ);
    }
    
    /**
     * Get adjacent chunk coordinates
     */
    public ChunkCoord left() {
        return new ChunkCoord(x - 1, z);
    }
    
    public ChunkCoord right() {
        return new ChunkCoord(x + 1, z);
    }
    
    public ChunkCoord front() {
        return new ChunkCoord(x, z + 1);
    }
    
    public ChunkCoord back() {
        return new ChunkCoord(x, z - 1);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkCoord that = (ChunkCoord) o;
        return x == that.x && z == that.z;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
    
    @Override
    public String toString() {
        return "ChunkCoord{" + x + ", " + z + "}";
    }
}
