package kr.co.voxelite.world;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.util.PerformanceLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages chunk loading/unloading.
 */
public class ChunkManager {
    private static final long UNLOAD_GRACE_MS = 500L;

    private final Map<ChunkCoord, Chunk> loadedChunks;
    private final LinkedHashMap<ChunkCoord, Long> chunkAccessTime;
    private final String worldPath;
    private final int defaultBlockType;
    private final IChunkGenerator chunkGenerator;
    private final IChunkLoadPolicy loadPolicy;
    private final ExecutorService executorService;
    private final Queue<Chunk> pendingChunks;
    private final Queue<ChunkCoord> dirtyChunks = new ConcurrentLinkedQueue<>();
    private final Set<ChunkCoord> dirtySet = ConcurrentHashMap.newKeySet();
    private final Set<ChunkCoord> loadingChunks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkCoord> requiredChunksCache = new HashSet<>();
    private final Map<ChunkCoord, Long> unloadEligibleSince = new HashMap<>();
    private final Map<String, ChunkCoord> chunkCoordCache = new HashMap<>();

    private ChunkCoord lastPlayerChunk = null;

    public ChunkManager(String worldPath, int defaultBlockType, IChunkGenerator chunkGenerator, IChunkLoadPolicy loadPolicy) {
        this.worldPath = worldPath;
        this.defaultBlockType = defaultBlockType;
        this.chunkGenerator = chunkGenerator;
        this.loadPolicy = loadPolicy;
        loadedChunks = new ConcurrentHashMap<>();
        chunkAccessTime = new LinkedHashMap<>(16, 0.75f, true);
        executorService = Executors.newFixedThreadPool(2);
        pendingChunks = new ConcurrentLinkedQueue<>();

        new File(worldPath, "chunks").mkdirs();
    }

    private ChunkCoord getOrCreateChunkCoord(int x, int z) {
        String key = x + "," + z;
        ChunkCoord coord = chunkCoordCache.get(key);
        if (coord == null) {
            coord = new ChunkCoord(x, z);
            chunkCoordCache.put(key, coord);
        }
        return coord;
    }

    public void updateLoadedChunks(float playerX, float playerZ) {
        updateLoadedChunks(List.of(new Vector3(playerX, 0f, playerZ)));
    }

    public void updateLoadedChunks(Collection<Vector3> playerPositions) {
        long now = System.currentTimeMillis();
        List<ChunkCoord> playerChunks = collectPlayerChunks(playerPositions);
        if (playerChunks.isEmpty()) {
            unloadExpiredChunks(now);
            return;
        }

        long t0 = PerformanceLogger.now();
        lastPlayerChunk = playerChunks.get(0);
        requiredChunksCache.clear();

        int searchRadius = estimateSearchRadius();
        for (ChunkCoord playerChunk : playerChunks) {
            updateRequiredChunksForPlayerChunk(playerChunk, searchRadius, now);
        }

        markChunksForDeferredUnload(now);
        unloadExpiredChunks(now);

        if (loadedChunks.size() > loadPolicy.getMaxLoadedChunks()) {
            unloadOldChunks(requiredChunksCache);
        }

        if (PerformanceLogger.ENABLED) {
            PerformanceLogger.log("ChunkManager", "updateLoadedChunks loaded=" + loadedChunks.size() + " loading=" + loadingChunks.size(), PerformanceLogger.now() - t0);
        }
    }

    private List<ChunkCoord> collectPlayerChunks(Collection<Vector3> playerPositions) {
        List<ChunkCoord> playerChunks = new ArrayList<>();
        Set<ChunkCoord> seen = new HashSet<>();
        if (playerPositions == null) {
            return playerChunks;
        }

        for (Vector3 playerPosition : playerPositions) {
            if (playerPosition == null || !Float.isFinite(playerPosition.x) || !Float.isFinite(playerPosition.z)) {
                continue;
            }

            ChunkCoord chunkCoord = ChunkCoord.fromWorldPos(playerPosition.x, playerPosition.z, Chunk.CHUNK_SIZE);
            if (seen.add(chunkCoord)) {
                playerChunks.add(chunkCoord);
            }
        }
        return playerChunks;
    }

    private void updateRequiredChunksForPlayerChunk(ChunkCoord playerChunk, int searchRadius, long now) {
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                int chunkX = playerChunk.x + dx;
                int chunkZ = playerChunk.z + dz;
                ChunkCoord coord = getOrCreateChunkCoord(chunkX, chunkZ);
                boolean shouldRender = loadPolicy.shouldLoadToMemory(chunkX, chunkZ, playerChunk.x, playerChunk.z);
                boolean shouldKeepLoaded = shouldRender || loadPolicy.shouldKeepLoaded(chunkX, chunkZ, playerChunk.x, playerChunk.z);

                if (shouldKeepLoaded) {
                    requiredChunksCache.add(coord);
                    unloadEligibleSince.remove(coord);
                    if (!loadedChunks.containsKey(coord)) {
                        loadOrGenerateChunkAsync(coord);
                    } else {
                        chunkAccessTime.put(coord, now);
                    }
                } else if (loadPolicy.shouldPregenerate(chunkX, chunkZ, playerChunk.x, playerChunk.z)
                    && !ChunkSerializer.chunkFileExists(worldPath, coord)) {
                    pregenerateChunkToDisk(coord);
                }
            }
        }
    }

    private int estimateSearchRadius() {
        int maxLoaded = Math.max(1, loadPolicy.getMaxLoadedChunks());
        int estimatedKeepLoadedRadius = (int) Math.ceil((Math.sqrt(maxLoaded) - 1f) / 2f);
        return Math.max(10, estimatedKeepLoadedRadius + 2);
    }

    private void pregenerateChunkToDisk(ChunkCoord coord) {
        executorService.submit(() -> {
            try {
                if (!ChunkSerializer.chunkFileExists(worldPath, coord)) {
                    Chunk chunk = new Chunk(coord);
                    chunkGenerator.generateChunk(chunk, defaultBlockType);
                    chunk.markAsGenerated();
                    ChunkSerializer.saveChunk(chunk, ChunkSerializer.getChunkFile(worldPath, coord));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void unloadOldChunks(Set<ChunkCoord> protectedChunks) {
        List<Map.Entry<ChunkCoord, Long>> sorted = new ArrayList<>(chunkAccessTime.entrySet());
        sorted.sort(Map.Entry.comparingByValue());

        int toRemove = loadedChunks.size() - loadPolicy.getMaxLoadedChunks() + 10;
        int removed = 0;

        for (Map.Entry<ChunkCoord, Long> entry : sorted) {
            if (removed >= toRemove) {
                break;
            }

            ChunkCoord coord = entry.getKey();
            if (protectedChunks.contains(coord) || loadingChunks.contains(coord)) {
                continue;
            }

            unloadChunk(coord);
            removed++;
        }
    }

    private void markChunksForDeferredUnload(long now) {
        for (ChunkCoord coord : loadedChunks.keySet()) {
            if (loadingChunks.contains(coord) || requiredChunksCache.contains(coord)) {
                unloadEligibleSince.remove(coord);
                continue;
            }
            unloadEligibleSince.putIfAbsent(coord, now);
        }
    }

    private void unloadExpiredChunks(long now) {
        List<ChunkCoord> expired = new ArrayList<>();
        for (Map.Entry<ChunkCoord, Long> entry : unloadEligibleSince.entrySet()) {
            ChunkCoord coord = entry.getKey();
            if (!loadingChunks.contains(coord) && now - entry.getValue() >= UNLOAD_GRACE_MS) {
                expired.add(coord);
            }
        }

        for (ChunkCoord coord : expired) {
            unloadChunk(coord);
        }
    }

    private void unloadChunk(ChunkCoord coord) {
        Chunk chunk = loadedChunks.get(coord);
        if (chunk != null && chunk.isGenerated()) {
            try {
                ChunkSerializer.saveChunk(chunk, ChunkSerializer.getChunkFile(worldPath, coord));
            } catch (IOException e) {
                System.err.println("[ChunkManager] Failed to save chunk " + coord);
                e.printStackTrace();
            }
        }

        loadedChunks.remove(coord);
        chunkAccessTime.remove(coord);
        unloadEligibleSince.remove(coord);
        dirtySet.remove(coord);
        dirtyChunks.remove(coord);
    }

    private void loadOrGenerateChunkAsync(ChunkCoord coord) {
        if (loadedChunks.containsKey(coord)) {
            chunkAccessTime.put(coord, System.currentTimeMillis());
            return;
        }
        if (loadingChunks.contains(coord)) {
            return;
        }

        Chunk chunk = loadedChunks.computeIfAbsent(coord, Chunk::new);
        loadingChunks.add(coord);

        executorService.submit(() -> {
            try {
                File chunkFile = ChunkSerializer.getChunkFile(worldPath, coord);
                if (ChunkSerializer.chunkFileExists(worldPath, coord)) {
                    try {
                        ChunkSerializer.loadInto(chunk, chunkFile);
                    } catch (IOException e) {
                        chunkGenerator.generateChunk(chunk, defaultBlockType);
                        chunk.markAsGenerated();
                        chunk.setModified(true);
                    }
                } else {
                    chunkGenerator.generateChunk(chunk, defaultBlockType);
                    chunk.markAsGenerated();
                    chunk.setModified(true);
                }

                pendingChunks.offer(chunk);
            } catch (Exception e) {
                e.printStackTrace();
                loadingChunks.remove(coord);
            }
        });
    }

    private void processPendingChunks() {
        long t0 = PerformanceLogger.now();
        Chunk chunk;
        int processed = 0;
        List<ChunkCoord> generatedChunks = new ArrayList<>();

        while ((chunk = pendingChunks.poll()) != null && processed < 4) {
            ChunkCoord coord = chunk.getCoord();
            loadingChunks.remove(coord);
            chunkAccessTime.put(coord, System.currentTimeMillis());
            unloadEligibleSince.remove(coord);
            generatedChunks.add(coord);
            processed++;
        }

        if (lastPlayerChunk != null && generatedChunks.size() > 1) {
            generatedChunks.sort(Comparator.comparingInt(coord -> chunkDistanceSq(coord, lastPlayerChunk)));
        }

        for (ChunkCoord coord : generatedChunks) {
            addDirtyChunk(coord);
        }
        for (ChunkCoord coord : generatedChunks) {
            markAdjacentChunksDirty(coord);
        }

        if (PerformanceLogger.ENABLED && processed > 0) {
            System.out.printf("[PERF][ChunkManager] processPendingChunks: %d chunks, %d ms%n", processed, PerformanceLogger.now() - t0);
        }
    }

    private void markAdjacentChunksDirty(ChunkCoord center) {
        ChunkCoord[] adjacents = {
            center.left(),
            center.right(),
            center.front(),
            center.back()
        };

        for (ChunkCoord adj : adjacents) {
            Chunk chunk = loadedChunks.get(adj);
            if (chunk != null && chunk.isGenerated()) {
                addDirtyChunk(adj);
            }
        }
    }

    public void addDirtyChunk(ChunkCoord coord) {
        if (dirtySet.add(coord)) {
            dirtyChunks.offer(coord);
        }
    }

    public ChunkCoord pollDirtyChunk() {
        ChunkCoord coord = null;
        if (lastPlayerChunk == null) {
            coord = dirtyChunks.poll();
        } else {
            int bestDistance = Integer.MAX_VALUE;
            for (ChunkCoord candidate : dirtyChunks) {
                int distance = chunkDistanceSq(candidate, lastPlayerChunk);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    coord = candidate;
                    if (distance == 0) {
                        break;
                    }
                }
            }
            if (coord != null) {
                dirtyChunks.remove(coord);
            } else {
                coord = dirtyChunks.poll();
            }
        }

        if (coord != null) {
            dirtySet.remove(coord);
        }
        return coord;
    }

    public void processPendingChunksPublic() {
        processPendingChunks();
    }

    public void generateInitialChunks(float centerX, float centerZ, int totalRadius, int loadRadius) {
        ChunkCoord center = ChunkCoord.fromWorldPos(centerX, centerZ, Chunk.CHUNK_SIZE);
        lastPlayerChunk = center;
        unloadEligibleSince.clear();
        int generated = 0;
        int loaded = 0;
        List<ChunkCoord> generatedChunks = new ArrayList<>();

        for (int dx = -totalRadius; dx <= totalRadius; dx++) {
            for (int dz = -totalRadius; dz <= totalRadius; dz++) {
                ChunkCoord coord = new ChunkCoord(center.x + dx, center.z + dz);
                if (!ChunkSerializer.chunkFileExists(worldPath, coord)) {
                    Chunk chunk = new Chunk(coord);
                    chunkGenerator.generateChunk(chunk, defaultBlockType);
                    chunk.markAsGenerated();
                    try {
                        ChunkSerializer.saveChunk(chunk, ChunkSerializer.getChunkFile(worldPath, coord));
                        generated++;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        for (int dx = -loadRadius; dx <= loadRadius; dx++) {
            for (int dz = -loadRadius; dz <= loadRadius; dz++) {
                ChunkCoord coord = new ChunkCoord(center.x + dx, center.z + dz);
                try {
                    Chunk chunk = ChunkSerializer.loadChunk(ChunkSerializer.getChunkFile(worldPath, coord));
                    loadedChunks.put(coord, chunk);
                    chunkAccessTime.put(coord, System.currentTimeMillis());
                    loaded++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        generatedChunks.addAll(loadedChunks.keySet());
        generatedChunks.sort(Comparator.comparingInt(coord -> chunkDistanceSq(coord, center)));
        for (ChunkCoord coord : generatedChunks) {
            addDirtyChunk(coord);
        }
        System.out.println("[ChunkManager] Generated: " + generated + ", Loaded: " + loaded);
    }

    private int chunkDistanceSq(ChunkCoord a, ChunkCoord b) {
        int dx = a.x - b.x;
        int dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    public Chunk getChunk(ChunkCoord coord) {
        return loadedChunks.get(coord);
    }

    public void replaceChunk(Chunk chunk) {
        ChunkCoord coord = chunk.getCoord();
        loadedChunks.put(coord, chunk);
        chunkAccessTime.put(coord, System.currentTimeMillis());
        unloadEligibleSince.remove(coord);
        loadingChunks.remove(coord);
        addDirtyChunk(coord);
        markAdjacentChunksDirty(coord);
    }

    public Chunk.BlockData getBlockAt(Vector3 worldPos) {
        ChunkCoord coord = ChunkCoord.fromWorldPos(worldPos.x, worldPos.z, Chunk.CHUNK_SIZE);
        Chunk chunk = loadedChunks.get(coord);
        if (chunk == null || !chunk.isGenerated()) {
            return null;
        }

        int blockX = (int) Math.floor(worldPos.x);
        int blockZ = (int) Math.floor(worldPos.z);
        int localX = Math.floorMod(blockX, Chunk.CHUNK_SIZE);
        int localZ = Math.floorMod(blockZ, Chunk.CHUNK_SIZE);
        return chunk.getBlock(localX, worldPos.y, localZ);
    }

    public boolean hasBlockAt(float worldX, float worldY, float worldZ) {
        ChunkCoord coord = ChunkCoord.fromWorldPos(worldX, worldZ, Chunk.CHUNK_SIZE);
        Chunk chunk = loadedChunks.get(coord);
        if (chunk == null || !chunk.isGenerated()) {
            return false;
        }

        int blockX = (int) Math.floor(worldX);
        int blockZ = (int) Math.floor(worldZ);
        int localX = Math.floorMod(blockX, Chunk.CHUNK_SIZE);
        int localZ = Math.floorMod(blockZ, Chunk.CHUNK_SIZE);
        return chunk.hasBlockAt(localX, worldY, localZ);
    }

    public void addBlock(Vector3 worldPos, int blockType) {
        ChunkCoord coord = ChunkCoord.fromWorldPos(worldPos.x, worldPos.z, Chunk.CHUNK_SIZE);
        Chunk chunk = loadedChunks.get(coord);

        if (chunk == null) {
            chunk = new Chunk(coord);
            chunk.markAsGenerated();
            chunk.setModified(true);
            loadedChunks.put(coord, chunk);
        } else if (!chunk.isGenerated()) {
            chunk.markAsGenerated();
        }

        chunk.addBlockWorld(worldPos, blockType);
        chunkAccessTime.put(coord, System.currentTimeMillis());
        unloadEligibleSince.remove(coord);
    }

    public boolean removeBlock(Vector3 worldPos) {
        ChunkCoord coord = ChunkCoord.fromWorldPos(worldPos.x, worldPos.z, Chunk.CHUNK_SIZE);
        Chunk chunk = loadedChunks.get(coord);
        if (chunk == null || !chunk.isGenerated()) {
            return false;
        }

        int blockX = (int) Math.floor(worldPos.x);
        int blockZ = (int) Math.floor(worldPos.z);
        int localX = Math.floorMod(blockX, Chunk.CHUNK_SIZE);
        int localZ = Math.floorMod(blockZ, Chunk.CHUNK_SIZE);
        return chunk.removeBlock(localX, worldPos.y, localZ);
    }

    public List<Chunk.BlockData> getAllBlocks() {
        List<Chunk.BlockData> allBlocks = new ArrayList<>();
        for (Chunk chunk : loadedChunks.values()) {
            if (chunk.isGenerated()) {
                allBlocks.addAll(chunk.getBlocks());
            }
        }
        return allBlocks;
    }

    public float getChunkCenterHeight(float worldX, float worldZ) {
        ChunkCoord coord = ChunkCoord.fromWorldPos(worldX, worldZ, Chunk.CHUNK_SIZE);
        Chunk chunk = loadedChunks.get(coord);
        if (chunk != null && chunk.isGenerated()) {
            return chunk.getCenterHeight();
        }
        return 0f;
    }

    public Collection<Chunk> getLoadedChunks() {
        return loadedChunks.values();
    }

    public void removeLoadedChunk(ChunkCoord coord) {
        Chunk removed = loadedChunks.remove(coord);
        if (removed == null) {
            return;
        }

        chunkAccessTime.remove(coord);
        unloadEligibleSince.remove(coord);
        loadingChunks.remove(coord);
        dirtySet.remove(coord);
        dirtyChunks.remove(coord);
    }

    public boolean isChunkVisible(ChunkCoord coord) {
        if (lastPlayerChunk == null) {
            return true;
        }
        return loadPolicy.shouldLoadToMemory(coord.x, coord.z, lastPlayerChunk.x, lastPlayerChunk.z);
    }

    public List<Vector3> getBlockPositionsSnapshot() {
        List<Vector3> snapshot = new ArrayList<>();
        for (Chunk chunk : loadedChunks.values()) {
            if (!chunk.isGenerated()) {
                continue;
            }

            ChunkCoord coord = chunk.getCoord();
            for (BlockPos localPos : chunk.getBlockPosSnapshot()) {
                float worldX = coord.x * Chunk.CHUNK_SIZE + localPos.x();
                float worldY = localPos.y();
                float worldZ = coord.z * Chunk.CHUNK_SIZE + localPos.z();
                snapshot.add(new Vector3(worldX, worldY, worldZ));
            }
        }
        return snapshot;
    }

    public List<Chunk> getNearbyChunks(float worldX, float worldZ, int radius) {
        ChunkCoord centerChunk = ChunkCoord.fromWorldPos(worldX, worldZ, Chunk.CHUNK_SIZE);
        List<Chunk> nearbyChunks = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkCoord coord = new ChunkCoord(centerChunk.x + dx, centerChunk.z + dz);
                Chunk chunk = loadedChunks.get(coord);
                if (chunk != null && chunk.isGenerated()) {
                    nearbyChunks.add(chunk);
                }
            }
        }
        return nearbyChunks;
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        saveLoadedChunks();
    }

    private void saveLoadedChunks() {
        for (Chunk chunk : loadedChunks.values()) {
            if (chunk == null || !chunk.isGenerated() || !chunk.isModified()) {
                continue;
            }
            try {
                ChunkSerializer.saveChunk(chunk, ChunkSerializer.getChunkFile(worldPath, chunk.getCoord()));
            } catch (IOException e) {
                System.err.println("[ChunkManager] Failed to save chunk on shutdown " + chunk.getCoord());
                e.printStackTrace();
            }
        }
    }
}
