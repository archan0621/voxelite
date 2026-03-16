package kr.co.voxelite.world;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChunkRenderSectionTest {

    @Test
    void getRenderSectionIndex_ShouldGroupHeightsIntoSixteenBlockBands() {
        assertEquals(0, Chunk.getRenderSectionIndex(0));
        assertEquals(0, Chunk.getRenderSectionIndex(15));
        assertEquals(1, Chunk.getRenderSectionIndex(16));
        assertEquals(15, Chunk.getRenderSectionIndex(255));
    }

    @Test
    void getRenderSectionBounds_ShouldMatchChunkColumnSlice() {
        Chunk chunk = new Chunk(new ChunkCoord(2, 3));

        BoundingBox sectionBounds = chunk.getRenderSectionBounds(2);

        assertEquals(new Vector3(32f, 32f, 48f), sectionBounds.min);
        assertEquals(new Vector3(48f, 48f, 64f), sectionBounds.max);
    }

    @Test
    void getRenderSectionBounds_ShouldRejectOutOfRangeSection() {
        Chunk chunk = new Chunk(new ChunkCoord(0, 0));

        assertThrows(IllegalArgumentException.class, () -> chunk.getRenderSectionBounds(-1));
        assertThrows(IllegalArgumentException.class, () -> chunk.getRenderSectionBounds(Chunk.getRenderSectionCount()));
    }
}
