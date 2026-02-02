package kr.co.voxelite.world;

/**
 * Chunk load policy interface
 * Library provides only interface, implementation is injected by application
 */
public interface IChunkLoadPolicy {
    /**
     * Should this chunk be loaded into memory?
     */
    boolean shouldLoadToMemory(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ);
    
    /**
     * Should this chunk be pre-generated?
     */
    boolean shouldPregenerate(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ);
    
    /**
     * Maximum number of loaded chunks
     */
    int getMaxLoadedChunks();
}
