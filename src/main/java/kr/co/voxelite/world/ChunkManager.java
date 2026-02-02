package kr.co.voxelite.world;

import com.badlogic.gdx.math.Vector3;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Manages chunk loading/unloading (library level)
 * Policies are injected from outside
 */
public class ChunkManager {
    private final Map<ChunkCoord, Chunk> loadedChunks;
    private final LinkedHashMap<ChunkCoord, Long> chunkAccessTime; // LRU tracking
    private final String worldPath;
    private final int defaultBlockType;
    
    // Policy injection
    private final IChunkGenerator chunkGenerator;
    private final IChunkLoadPolicy loadPolicy;
    
    private final ExecutorService executorService;
    private final Queue<Chunk> pendingChunks;
    private volatile boolean chunksChanged = false;
    
    // Track chunk boundary
    private ChunkCoord lastPlayerChunk = null;
    
    // Reusable collections (reduce GC)
    private final Set<ChunkCoord> requiredChunksCache = new HashSet<>();
    private final Map<String, ChunkCoord> chunkCoordCache = new HashMap<>(); // ChunkCoord caching
    
    // ✅ Track loading chunks (prevent duplicates + solve placeholder issue)
    private final Set<ChunkCoord> loadingChunks = ConcurrentHashMap.newKeySet();
    
    public ChunkManager(String worldPath, int defaultBlockType, 
                       IChunkGenerator chunkGenerator, IChunkLoadPolicy loadPolicy) {
        this.worldPath = worldPath;
        this.defaultBlockType = defaultBlockType;
        this.chunkGenerator = chunkGenerator;
        this.loadPolicy = loadPolicy;
        
        this.loadedChunks = new ConcurrentHashMap<>();
        this.chunkAccessTime = new LinkedHashMap<>(16, 0.75f, true);
        this.executorService = Executors.newFixedThreadPool(2);
        this.pendingChunks = new ConcurrentLinkedQueue<>();
        
        new File(worldPath, "chunks").mkdirs();
    }
    
    /**
     * Get or create cached ChunkCoord (object reuse)
     */
    private ChunkCoord getOrCreateChunkCoord(int x, int z) {
        String key = x + "," + z;
        ChunkCoord coord = chunkCoordCache.get(key);
        if (coord == null) {
            coord = new ChunkCoord(x, z);
            chunkCoordCache.put(key, coord);
        }
        return coord;
    }
    
    /**
     * Update loaded chunks (only when crossing chunk boundary)
     */
    public void updateLoadedChunks(float playerX, float playerZ) {
        ChunkCoord playerChunk = ChunkCoord.fromWorldPos(playerX, playerZ, Chunk.CHUNK_SIZE);
        
        // ✅ Check chunk boundary crossing
        if (lastPlayerChunk != null && lastPlayerChunk.equals(playerChunk)) {
            // Within same chunk, only execute processPendingChunks
            processPendingChunks();
            return;
        }
        
        // Chunk boundary crossed!
        lastPlayerChunk = playerChunk;
        requiredChunksCache.clear(); // Reuse
        
        // Process chunks according to policy
        int searchRadius = Math.max(10, loadPolicy.getMaxLoadedChunks() / 10);
        
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                int chunkX = playerChunk.x + dx;
                int chunkZ = playerChunk.z + dz;
                ChunkCoord coord = getOrCreateChunkCoord(chunkX, chunkZ);
                
                // 1. Memory load decision
                if (loadPolicy.shouldLoadToMemory(chunkX, chunkZ, playerChunk.x, playerChunk.z)) {
                    requiredChunksCache.add(coord);
                    if (!loadedChunks.containsKey(coord)) {
                        loadOrGenerateChunkAsync(coord);
                    } else {
                        chunkAccessTime.put(coord, System.currentTimeMillis());
                    }
                }
                
                // 2. Pregeneration decision (file only)
                else if (loadPolicy.shouldPregenerate(chunkX, chunkZ, playerChunk.x, playerChunk.z)) {
                    if (!ChunkSerializer.chunkFileExists(worldPath, coord)) {
                        pregenerateChunkToDisk(coord);
                    }
                }
            }
        }
        
        // Check memory limit
        if (loadedChunks.size() > loadPolicy.getMaxLoadedChunks()) {
            unloadOldChunks(requiredChunksCache);
        }
        
        processPendingChunks();
    }
    
    /**
     * Pregenerate chunk to disk only
     */
    private void pregenerateChunkToDisk(ChunkCoord coord) {
        executorService.submit(() -> {
            try {
                if (!ChunkSerializer.chunkFileExists(worldPath, coord)) {
                    Chunk chunk = new Chunk(coord);
                    chunkGenerator.generateChunk(chunk, defaultBlockType);
                    ChunkSerializer.saveChunk(chunk, ChunkSerializer.getChunkFile(worldPath, coord));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Unload old chunks based on LRU (save to disk first)
     */
    private void unloadOldChunks(Set<ChunkCoord> protectedChunks) {
        List<Map.Entry<ChunkCoord, Long>> sorted = new ArrayList<>(chunkAccessTime.entrySet());
        sorted.sort(Map.Entry.comparingByValue()); // Oldest first
        
        int toRemove = loadedChunks.size() - loadPolicy.getMaxLoadedChunks() + 10; // Remove 10 extra
        int removed = 0;
        
        for (Map.Entry<ChunkCoord, Long> entry : sorted) {
            if (removed >= toRemove) break;
            
            ChunkCoord coord = entry.getKey();
            
            // Don't unload protected chunks (within render distance)
            if (protectedChunks.contains(coord)) {
                continue;
            }
            
            Chunk chunk = loadedChunks.get(coord);
            if (chunk != null && chunk.isGenerated()) {
                // Save to disk
                try {
                    ChunkSerializer.saveChunk(chunk, ChunkSerializer.getChunkFile(worldPath, coord));
                } catch (IOException e) {
                    System.err.println("[ChunkManager] Failed to save chunk " + coord);
                    e.printStackTrace();
                }
            }
            
            loadedChunks.remove(coord);
            chunkAccessTime.remove(coord);
            chunksChanged = true;
            removed++;
        }
    }
    
    /**
     * Load or generate chunk asynchronously
     * ✅ Modified: Create placeholder but never replace it
     */
    private void loadOrGenerateChunkAsync(ChunkCoord coord) {
        // If already loaded, only update access time
        if (loadedChunks.containsKey(coord)) {
            chunkAccessTime.put(coord, System.currentTimeMillis());
            return;
        }
        
        // Skip if already loading
        if (loadingChunks.contains(coord)) {
            return;
        }
        
        // ✅ Create placeholder (this object will never be replaced)
        Chunk chunk = loadedChunks.computeIfAbsent(coord, c -> new Chunk(c));
        
        // ✅ Mark as loading (prevent duplicates)
        loadingChunks.add(coord);
        
        executorService.submit(() -> {
            try {
                File chunkFile = ChunkSerializer.getChunkFile(worldPath, coord);
                
                // Try loading from disk
                if (ChunkSerializer.chunkFileExists(worldPath, coord)) {
                    try {
                        // ✅ Fill data into existing object
                        ChunkSerializer.loadInto(chunk, chunkFile);
                    } catch (IOException e) {
                        // Generate new if load fails
                        chunkGenerator.generateChunk(chunk, defaultBlockType);
                        chunk.markAsGenerated();
                    }
                } else {
                    // Generate new
                    chunkGenerator.generateChunk(chunk, defaultBlockType);
                    chunk.markAsGenerated();
                    
                    // Save to disk (commented out)
                    // try {
                    //     ChunkSerializer.saveChunk(chunk, chunkFile);
                    // } catch (IOException e) {
                    //     e.printStackTrace();
                    // }
                }
                
                // ✅ Loading complete - add to pendingChunks
                pendingChunks.offer(chunk);
                
            } catch (Exception e) {
                e.printStackTrace();
                loadingChunks.remove(coord); // Remove loading flag on failure
            }
        });
    }
    
    /**
     * Process chunks generated in background (call from main thread)
     * ✅ Modified: Only remove from loadingChunks (object already in loadedChunks)
     */
    private void processPendingChunks() {
        Chunk chunk;
        int processed = 0;
        List<ChunkCoord> generatedChunks = new ArrayList<>();
        
        while ((chunk = pendingChunks.poll()) != null && processed < 4) { // Max 4 chunks per frame
            ChunkCoord coord = chunk.getCoord();
            
            // ✅ Loading complete - only remove from loadingChunks
            loadingChunks.remove(coord);
            
            // ✅ Update access time (object already in loadedChunks)
            chunkAccessTime.put(coord, System.currentTimeMillis());
            chunksChanged = true;
            generatedChunks.add(coord);
            processed++;
        }
        
        // ✅ Invalidate adjacent chunk meshes
        for (ChunkCoord coord : generatedChunks) {
            invalidateAdjacentChunkMeshes(coord);
        }
    }
    
    /**
     * Invalidate meshes of adjacent chunks
     * ✅ Trigger mesh regeneration so adjacent chunk faces become visible when chunk is created
     */
    private void invalidateAdjacentChunkMeshes(ChunkCoord center) {
        ChunkCoord[] adjacents = {
            center.left(),
            center.right(),
            center.front(),
            center.back()
        };
        
        for (ChunkCoord adj : adjacents) {
            Chunk chunk = loadedChunks.get(adj);
            if (chunk != null && chunk.isGenerated()) {
                // Set mesh to null to trigger regeneration
                chunk.setMesh(null);
                chunksChanged = true;
            }
        }
    }
    
    /**
     * Public method for processing pending chunks (call every frame)
     */
    public void processPendingChunksPublic() {
        processPendingChunks();
    }
    
    /**
     * Generate initial chunks synchronously
     */
    public void generateInitialChunks(float centerX, float centerZ, int totalRadius, int loadRadius) {
        ChunkCoord center = ChunkCoord.fromWorldPos(centerX, centerZ, Chunk.CHUNK_SIZE);
        int generated = 0;
        int loaded = 0;
        
        // 1. Generate to file
        for (int dx = -totalRadius; dx <= totalRadius; dx++) {
            for (int dz = -totalRadius; dz <= totalRadius; dz++) {
                ChunkCoord coord = new ChunkCoord(center.x + dx, center.z + dz);
                
                if (!ChunkSerializer.chunkFileExists(worldPath, coord)) {
                    Chunk chunk = new Chunk(coord);
                    chunkGenerator.generateChunk(chunk, defaultBlockType);
                    
                    try {
                        ChunkSerializer.saveChunk(chunk, ChunkSerializer.getChunkFile(worldPath, coord));
                        generated++;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        // 2. Load to memory
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
        
        System.out.println("[ChunkManager] Generated: " + generated + ", Loaded: " + loaded);
    }
    
    /**
     * Get chunk at coordinate
     */
    public Chunk getChunk(ChunkCoord coord) {
        return loadedChunks.get(coord);
    }
    
    /**
     * Get block at world position
     */
    public Chunk.BlockData getBlockAt(Vector3 worldPos) {
        ChunkCoord coord = ChunkCoord.fromWorldPos(worldPos.x, worldPos.z, Chunk.CHUNK_SIZE);
        Chunk chunk = loadedChunks.get(coord);
        
        if (chunk == null || !chunk.isGenerated()) {
            return null;
        }
        
        // ✅ 음수 좌표 안전: Math.floor → Math.floorMod
        int blockX = (int) Math.floor(worldPos.x);
        int blockZ = (int) Math.floor(worldPos.z);
        int localX = Math.floorMod(blockX, Chunk.CHUNK_SIZE);
        int localZ = Math.floorMod(blockZ, Chunk.CHUNK_SIZE);
        
        return chunk.getBlock(localX, worldPos.y, localZ);
    }
    
    /**
     * Check if block exists at world position (for face culling)
     */
    public boolean hasBlockAt(float worldX, float worldY, float worldZ) {
        ChunkCoord coord = ChunkCoord.fromWorldPos(worldX, worldZ, Chunk.CHUNK_SIZE);
        Chunk chunk = loadedChunks.get(coord);
        
        if (chunk == null || !chunk.isGenerated()) {
            return true;  // UNKNOWN = AIR
        }
        
        // ✅ Negative coordinate safe: Math.floor → Math.floorMod
        int blockX = (int) Math.floor(worldX);
        int blockZ = (int) Math.floor(worldZ);
        int localX = Math.floorMod(blockX, Chunk.CHUNK_SIZE);
        int localZ = Math.floorMod(blockZ, Chunk.CHUNK_SIZE);
        
        return chunk.hasBlockAt(localX, worldY, localZ);
    }
    
    /**
     * Add block at world position
     */
    public void addBlock(Vector3 worldPos, int blockType) {
        ChunkCoord coord = ChunkCoord.fromWorldPos(worldPos.x, worldPos.z, Chunk.CHUNK_SIZE);
        Chunk chunk = loadedChunks.get(coord);
        
        if (chunk != null && chunk.isGenerated()) {
            chunk.addBlockWorld(worldPos, blockType);
        }
    }
    
    /**
     * Remove block at world position
     */
    public boolean removeBlock(Vector3 worldPos) {
        ChunkCoord coord = ChunkCoord.fromWorldPos(worldPos.x, worldPos.z, Chunk.CHUNK_SIZE);
        Chunk chunk = loadedChunks.get(coord);
        
        if (chunk == null || !chunk.isGenerated()) {
            return false;
        }
        
        // ✅ Negative coordinate safe: Math.floor → Math.floorMod
        int blockX = (int) Math.floor(worldPos.x);
        int blockZ = (int) Math.floor(worldPos.z);
        int localX = Math.floorMod(blockX, Chunk.CHUNK_SIZE);
        int localZ = Math.floorMod(blockZ, Chunk.CHUNK_SIZE);
        
        return chunk.removeBlock(localX, worldPos.y, localZ);
    }
    
    /**
     * Get all blocks from loaded chunks
     */
    public List<Chunk.BlockData> getAllBlocks() {
        List<Chunk.BlockData> allBlocks = new ArrayList<>();
        
        for (Chunk chunk : loadedChunks.values()) {
            if (chunk.isGenerated()) {
                allBlocks.addAll(chunk.getBlocks());
            }
        }
        
        return allBlocks;
    }
    
    /**
     * Get center height of chunk at world position
     */
    public float getChunkCenterHeight(float worldX, float worldZ) {
        ChunkCoord coord = ChunkCoord.fromWorldPos(worldX, worldZ, Chunk.CHUNK_SIZE);
        Chunk chunk = loadedChunks.get(coord);
        
        if (chunk != null && chunk.isGenerated()) {
            return chunk.getCenterHeight();
        }
        
        return 0f;
    }
    
    /**
     * Get all loaded chunks
     */
    public Collection<Chunk> getLoadedChunks() {
        return loadedChunks.values();
    }
    
    /**
     * Get snapshot of all block positions as world coordinates (thread-safe)
     * ✅ Prevents ConcurrentModificationException
     */
    public List<Vector3> getBlockPositionsSnapshot() {
        List<Vector3> snapshot = new ArrayList<>();
        
        for (Chunk chunk : loadedChunks.values()) {
            if (!chunk.isGenerated()) continue;
            
            ChunkCoord coord = chunk.getCoord();
            for (BlockPos localPos : chunk.getBlockPosSnapshot()) {
                // Local → World coordinate conversion
                float worldX = coord.x * Chunk.CHUNK_SIZE + localPos.x();
                float worldY = localPos.y();
                float worldZ = coord.z * Chunk.CHUNK_SIZE + localPos.z();
                snapshot.add(new Vector3(worldX, worldY, worldZ));
            }
        }
        
        return snapshot;
    }
    
    /**
     * Get nearby chunks within radius (for physics/collision)
     * @param radius Chunk radius (1 = 3x3, 2 = 5x5)
     */
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
    
    /**
     * Check if chunks changed and reset flag
     */
    public boolean consumeChunksChanged() {
        boolean changed = chunksChanged;
        chunksChanged = false;
        return changed;
    }
    
    /**
     * Shutdown executor threads
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
