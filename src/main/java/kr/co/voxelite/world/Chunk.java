package kr.co.voxelite.world;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A chunk represents a 16x16 area of blocks.
 */
public class Chunk {
    public static final int CHUNK_SIZE = 16;
    public static final int RENDER_SECTION_HEIGHT = 16;
    private static final int MAX_RENDER_HEIGHT = 256;
    private static final int RENDER_SECTION_COUNT = MAX_RENDER_HEIGHT / RENDER_SECTION_HEIGHT;
    private static final float BOUNDS_MIN_Y = -1f;
    private static final float BOUNDS_MAX_Y = 256f;

    private final ChunkCoord coord;
    private final Map<BlockPos, BlockData> blocks;
    private ChunkState state = ChunkState.EMPTY;
    private boolean modified = false;
    private final BoundingBox bounds;

    public Chunk(ChunkCoord coord) {
        this.coord = coord;
        blocks = new HashMap<>();
        bounds = calculateBounds();
    }

    private BoundingBox calculateBounds() {
        float worldX = coord.x * CHUNK_SIZE;
        float worldZ = coord.z * CHUNK_SIZE;
        Vector3 min = new Vector3(worldX, BOUNDS_MIN_Y, worldZ);
        Vector3 max = new Vector3(worldX + CHUNK_SIZE, BOUNDS_MAX_Y, worldZ + CHUNK_SIZE);
        return new BoundingBox(min, max);
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public BoundingBox getRenderSectionBounds(int sectionY) {
        if (sectionY < 0 || sectionY >= RENDER_SECTION_COUNT) {
            throw new IllegalArgumentException("Invalid render section index: " + sectionY);
        }

        float worldX = coord.x * CHUNK_SIZE;
        float worldZ = coord.z * CHUNK_SIZE;
        float minY = sectionY * RENDER_SECTION_HEIGHT;
        float maxY = minY + RENDER_SECTION_HEIGHT;
        Vector3 min = new Vector3(worldX, minY, worldZ);
        Vector3 max = new Vector3(worldX + CHUNK_SIZE, maxY, worldZ + CHUNK_SIZE);
        return new BoundingBox(min, max);
    }

    public static int getRenderSectionCount() {
        return RENDER_SECTION_COUNT;
    }

    public static boolean isValidRenderSectionIndex(int sectionY) {
        return sectionY >= 0 && sectionY < RENDER_SECTION_COUNT;
    }

    public static int getRenderSectionIndex(int blockY) {
        return Math.floorDiv(blockY, RENDER_SECTION_HEIGHT);
    }

    public ChunkState getState() {
        return state;
    }

    public void setState(ChunkState state) {
        this.state = state;
    }

    public boolean isGenerated() {
        return state == ChunkState.GENERATED;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public void addBlockLocal(int localX, float worldY, int localZ, int blockType) {
        int blockY = (int) Math.floor(worldY);
        BlockPos pos = new BlockPos(localX, blockY, localZ);
        blocks.put(pos, new BlockData(pos, blockType));
    }

    public BlockData getBlock(int localX, float worldY, int localZ) {
        int blockY = (int) Math.floor(worldY);
        BlockPos pos = new BlockPos(localX, blockY, localZ);
        return blocks.get(pos);
    }

    public boolean removeBlock(int localX, float worldY, int localZ) {
        int blockY = (int) Math.floor(worldY);
        BlockPos pos = new BlockPos(localX, blockY, localZ);
        boolean removed = blocks.remove(pos) != null;
        if (removed) {
            modified = true;
        }
        return removed;
    }

    public void addBlockWorld(Vector3 worldPos, int blockType) {
        int blockX = (int) Math.floor(worldPos.x);
        int blockZ = (int) Math.floor(worldPos.z);
        int localX = Math.floorMod(blockX, CHUNK_SIZE);
        int localZ = Math.floorMod(blockZ, CHUNK_SIZE);

        if (localX >= 0 && localX < CHUNK_SIZE && localZ >= 0 && localZ < CHUNK_SIZE) {
            addBlockLocal(localX, worldPos.y, localZ, blockType);
            modified = true;
        }
    }

    public Collection<BlockData> getBlocks() {
        return blocks.values();
    }

    public Collection<BlockPos> getBlockPosSnapshot() {
        return new ArrayList<>(blocks.keySet());
    }

    public float getCenterHeight() {
        int centerX = CHUNK_SIZE / 2;
        int centerZ = CHUNK_SIZE / 2;
        int maxY = Integer.MIN_VALUE;

        for (BlockData block : blocks.values()) {
            BlockPos pos = block.pos;
            if (pos.x() == centerX
                && pos.z() == centerZ
                && pos.y() > maxY) {
                maxY = pos.y();
            }
        }

        return maxY == Integer.MIN_VALUE ? -1f : maxY + 1f;
    }

    public ChunkCoord getCoord() {
        return coord;
    }

    public void markAsGenerated() {
        state = ChunkState.GENERATED;
    }

    public boolean hasBlockAt(int localX, float worldY, int localZ) {
        if (localX < 0 || localX >= CHUNK_SIZE || localZ < 0 || localZ >= CHUNK_SIZE) {
            return false;
        }
        int blockY = (int) Math.floor(worldY);
        BlockPos pos = new BlockPos(localX, blockY, localZ);
        return blocks.containsKey(pos);
    }

    public static class BlockData {
        public final BlockPos pos;
        public final int blockType;

        public BlockData(BlockPos pos, int blockType) {
            this.pos = pos;
            this.blockType = blockType;
        }

        public Vector3 getWorldPos(ChunkCoord chunkCoord) {
            return new Vector3(
                chunkCoord.x * CHUNK_SIZE + pos.x(),
                pos.y(),
                chunkCoord.z * CHUNK_SIZE + pos.z()
            );
        }
    }
}
