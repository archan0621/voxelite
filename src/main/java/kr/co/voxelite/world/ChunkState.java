package kr.co.voxelite.world;

/**
 * Lifecycle state of a chunk
 */
public enum ChunkState {
    EMPTY,      // Only exists
    GENERATED,  // Blocks generated
    MESHED,     // Mesh generated
    UPLOADED,   // GPU upload complete (if needed)
    ACTIVE      // In use for rendering/physics
}
