package kr.co.voxelite.world;

import com.badlogic.gdx.math.Vector3;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Manages chunk loading/unloading (라이브러리 레벨)
 * 정책은 외부에서 주입받음
 */
public class ChunkManager {
    private final Map<ChunkCoord, Chunk> loadedChunks;
    private final LinkedHashMap<ChunkCoord, Long> chunkAccessTime; // LRU tracking
    private final String worldPath;
    private final int defaultBlockType;
    
    // 정책 주입
    private final IChunkGenerator chunkGenerator;
    private final IChunkLoadPolicy loadPolicy;
    
    private final ExecutorService executorService;
    private final Queue<Chunk> pendingChunks;
    private volatile boolean chunksChanged = false;
    
    // 청크 경계 추적
    private ChunkCoord lastPlayerChunk = null;
    
    // 재사용 가능한 컬렉션 (GC 감소)
    private final Set<ChunkCoord> requiredChunksCache = new HashSet<>();
    private final Map<String, ChunkCoord> chunkCoordCache = new HashMap<>(); // ChunkCoord 캐싱
    
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
     * Get or create cached ChunkCoord (객체 재사용)
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
     * Update loaded chunks (청크 경계 이동 시에만)
     */
    public void updateLoadedChunks(float playerX, float playerZ) {
        ChunkCoord playerChunk = ChunkCoord.fromWorldPos(playerX, playerZ, Chunk.CHUNK_SIZE);
        
        // ✅ 청크 경계 이동 체크
        if (lastPlayerChunk != null && lastPlayerChunk.equals(playerChunk)) {
            // 같은 청크 내에서는 processPendingChunks만 실행
            processPendingChunks();
            return;
        }
        
        // 청크 경계 이동 발생!
        lastPlayerChunk = playerChunk;
        requiredChunksCache.clear(); // 재사용
        
        // 정책에 따라 청크 처리
        int searchRadius = Math.max(10, loadPolicy.getMaxLoadedChunks() / 10);
        
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                int chunkX = playerChunk.x + dx;
                int chunkZ = playerChunk.z + dz;
                ChunkCoord coord = getOrCreateChunkCoord(chunkX, chunkZ);
                
                // 1. 메모리 로드 판정
                if (loadPolicy.shouldLoadToMemory(chunkX, chunkZ, playerChunk.x, playerChunk.z)) {
                    requiredChunksCache.add(coord);
                    if (!loadedChunks.containsKey(coord)) {
                        loadOrGenerateChunkAsync(coord);
                    } else {
                        chunkAccessTime.put(coord, System.currentTimeMillis());
                    }
                }
                
                // 2. 사전 생성 판정 (파일만)
                else if (loadPolicy.shouldPregenerate(chunkX, chunkZ, playerChunk.x, playerChunk.z)) {
                    if (!ChunkSerializer.chunkFileExists(worldPath, coord)) {
                        pregenerateChunkToDisk(coord);
                    }
                }
            }
        }
        
        // 메모리 제한 확인
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
        sorted.sort(Map.Entry.comparingByValue()); // 오래된 순
        
        int toRemove = loadedChunks.size() - loadPolicy.getMaxLoadedChunks() + 10; // 10개 추가 제거
        int removed = 0;
        
        for (Map.Entry<ChunkCoord, Long> entry : sorted) {
            if (removed >= toRemove) break;
            
            ChunkCoord coord = entry.getKey();
            
            // 보호된 청크(렌더 거리 내)는 언로드 안함
            if (protectedChunks.contains(coord)) {
                continue;
            }
            
            Chunk chunk = loadedChunks.get(coord);
            if (chunk != null && chunk.isGenerated()) {
                // 디스크에 저장
                try {
                    ChunkSerializer.saveChunk(chunk, ChunkSerializer.getChunkFile(worldPath, coord));
                    System.out.println("[ChunkManager] Unloaded chunk to disk: " + coord);
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
     */
    private void loadOrGenerateChunkAsync(ChunkCoord coord) {
        if (loadedChunks.containsKey(coord)) {
            chunkAccessTime.put(coord, System.currentTimeMillis());
            return;
        }
        
        loadedChunks.put(coord, new Chunk(coord)); // placeholder
        
        executorService.submit(() -> {
            try {
                Chunk chunk;
                
                // 디스크에서 로드 시도
                if (ChunkSerializer.chunkFileExists(worldPath, coord)) {
                    try {
                        chunk = ChunkSerializer.loadChunk(ChunkSerializer.getChunkFile(worldPath, coord));
                    } catch (IOException e) {
                        chunk = new Chunk(coord);
                        chunkGenerator.generateChunk(chunk, defaultBlockType);
                    }
                } else {
                    // 새로 생성
                    chunk = new Chunk(coord);
                    chunkGenerator.generateChunk(chunk, defaultBlockType);
                    
                    // 디스크에 저장
                    try {
                        ChunkSerializer.saveChunk(chunk, ChunkSerializer.getChunkFile(worldPath, coord));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                
                pendingChunks.offer(chunk);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Process chunks generated in background (call from main thread)
     */
    private void processPendingChunks() {
        Chunk chunk;
        int processed = 0;
        while ((chunk = pendingChunks.poll()) != null && processed < 4) { // 프레임당 최대 4개 청크
            loadedChunks.put(chunk.getCoord(), chunk);
            chunkAccessTime.put(chunk.getCoord(), System.currentTimeMillis());
            chunksChanged = true;
            processed++;
        }
    }
    
    /**
     * Public method for processing pending chunks (매 프레임 호출)
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
        
        // 1. 파일로 생성
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
        
        // 2. 메모리 로드
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
        
        int localX = (int) (worldPos.x - coord.x * Chunk.CHUNK_SIZE);
        int localZ = (int) (worldPos.z - coord.z * Chunk.CHUNK_SIZE);
        
        return chunk.getBlock(localX, worldPos.y, localZ);
    }
    
    /**
     * Check if block exists at world position (for face culling)
     */
    public boolean hasBlockAt(float worldX, float worldY, float worldZ) {
        ChunkCoord coord = ChunkCoord.fromWorldPos(worldX, worldZ, Chunk.CHUNK_SIZE);
        Chunk chunk = loadedChunks.get(coord);
        
        if (chunk == null || !chunk.isGenerated()) {
            return false;
        }
        
        int localX = (int) (worldX - coord.x * Chunk.CHUNK_SIZE);
        int localZ = (int) (worldZ - coord.z * Chunk.CHUNK_SIZE);
        
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
        
        int localX = (int) (worldPos.x - coord.x * Chunk.CHUNK_SIZE);
        int localZ = (int) (worldPos.z - coord.z * Chunk.CHUNK_SIZE);
        
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
     * Get nearby chunks within radius (for physics/collision)
     * @param radius 청크 반경 (1 = 3x3, 2 = 5x5)
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
