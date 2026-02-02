package kr.co.voxelite.world;

/**
 * Immutable block position (integer coordinates)
 * ✅ Can be used as Map key (no float precision errors)
 */
public record BlockPos(int x, int y, int z) {
    /**
     * Convert world coordinates to block position
     */
    public static BlockPos fromWorld(float worldX, float worldY, float worldZ) {
        return new BlockPos(
            (int) Math.floor(worldX),
            (int) Math.floor(worldY),
            (int) Math.floor(worldZ)
        );
    }
}
