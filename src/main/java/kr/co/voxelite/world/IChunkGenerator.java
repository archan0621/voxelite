package kr.co.voxelite.world;

/**
 * Chunk generation interface
 * Library provides only interface, implementation is injected by application
 */
public interface IChunkGenerator {
    /**
     * Generates a chunk
     * @param chunk Chunk to generate
     * @param blockType Default block type
     */
    void generateChunk(Chunk chunk, int blockType);
}
