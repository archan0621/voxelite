package kr.co.voxelite.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Queue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ChunkManagerPendingChunksTest {
    @TempDir Path tempDir;

    @Test
    void completionBeyondFrameLimitRemainsQueuedAndFinishesNextFrame() throws Exception {
        IChunkLoadPolicy policy = new IChunkLoadPolicy() {
            public boolean shouldLoadToMemory(int x, int z, int px, int pz) { return true; }
            public boolean shouldKeepLoaded(int x, int z, int px, int pz) { return true; }
            public boolean shouldPregenerate(int x, int z, int px, int pz) { return false; }
            public int getMaxLoadedChunks() { return 100; }
        };
        ChunkManager manager = new ChunkManager(tempDir.toString(), 1, (chunk, type) -> {}, policy);
        try {
            Queue<Chunk> pending = field(manager, "pendingChunks");
            Set<ChunkCoord> loading = field(manager, "loadingChunks");
            for (int i = 0; i < 5; i++) {
                Chunk chunk = new Chunk(new ChunkCoord(i * 2, 0));
                chunk.addBlockLocal(0, 0, 0, 1);
                chunk.markAsGenerated();
                manager.replaceChunk(chunk);
                pending.offer(chunk);
                loading.add(chunk.getCoord());
            }
            while (manager.pollDirtySection() != null) {}

            ChunkCoord fifth = new ChunkCoord(8, 0);
            manager.processPendingChunksPublic();

            assertEquals(1, pending.size());
            assertEquals(fifth, pending.peek().getCoord());
            assertEquals(Set.of(fifth), loading);
            assertTrue(manager.drainDirtySections(fifth).isEmpty());

            manager.processPendingChunksPublic();

            assertTrue(pending.isEmpty());
            assertTrue(loading.isEmpty());
            assertEquals(Set.of(0), manager.drainDirtySections(fifth));
        } finally {
            manager.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
