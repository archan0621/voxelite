package kr.co.voxelite.world;

/**
 * Identifies a renderable 16x16x16 section inside a world chunk column.
 */
public record RenderSectionKey(ChunkCoord chunkCoord, int sectionY) {
}
