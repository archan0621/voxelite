package kr.co.voxelite.world;

import com.badlogic.gdx.graphics.g3d.Model;
import kr.co.voxelite.util.PerformanceLogger;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * World management class - now uses ChunkManager
 */
public class World {
    private BlockManager blockManager;
    private ChunkManager chunkManager;

    public World(BlockManager blockManager) {
        this.blockManager = blockManager;
    }
    
    /**
     * Initialize with chunk-based terrain generation (policy injection)
     */
    public void initWithChunks(String worldPath, int defaultBlockType, 
                              IChunkGenerator generator, IChunkLoadPolicy loadPolicy) {
        this.chunkManager = new ChunkManager(worldPath, defaultBlockType, generator, loadPolicy);
    }
    
    /**
     * Generate initial spawn chunks
     * @param totalRadius - Total radius to generate to file
     * @param loadRadius - Radius to load into memory
     */
    public float generateInitialChunks(float spawnX, float spawnZ, int totalRadius, int loadRadius) {
        if (chunkManager == null) {
            throw new IllegalStateException("ChunkManager not initialized. Call initWithChunks() first.");
        }
        
        chunkManager.generateInitialChunks(spawnX, spawnZ, totalRadius, loadRadius);
        
        return chunkManager.getChunkCenterHeight(spawnX, spawnZ);
    }
    
    /**
     * Update chunks based on player position (Tick-based, 10Hz)
     */
    public void updateChunks(float playerX, float playerZ) {
        if (chunkManager != null) {
            chunkManager.updateLoadedChunks(playerX, playerZ);
        }
    }
    
    /**
     * Process pending chunks (call every frame)
     */
    public void processPendingChunks() {
        if (chunkManager != null) {
            chunkManager.processPendingChunksPublic();
        }
    }
    
    /**
     * Process dirty chunk meshes (rate-limited, max per frame)
     */
    public void processDirtyChunkMeshes(int maxPerFrame) {
        if (chunkManager == null) return;
        
        int built = 0;
        while (built < maxPerFrame) {
            ChunkCoord coord = chunkManager.pollDirtyChunk();
            if (coord == null) break;
            
            Chunk chunk = chunkManager.getChunk(coord);
            if (chunk != null && chunk.isGenerated() && !chunk.hasMesh()) {
                buildChunkMesh(chunk);
                built++;
            }
        }
    }

    /**
     * Clear all blocks from the world
     */
    public void clear() {
        if (chunkManager != null) {
            chunkManager.shutdown();
            chunkManager = null;
        }
    }

    /**
     * Add a block at the specified position with block type
     */
    public void addBlock(Vector3 position, int blockType) {
        if (chunkManager != null) {
            chunkManager.addBlock(position, blockType);
            invalidateAndRebuildChunkMeshImmediate(position);
        }
    }
    
    /**
     * Add a block at the specified position (default blockType = 0)
     */
    public void addBlock(Vector3 position) {
        addBlock(position, 0);
    }
    
    /**
     * Remove a block at the specified position
     */
    public boolean removeBlock(Vector3 position) {
        if (chunkManager != null) {
            boolean removed = chunkManager.removeBlock(position);
            if (removed) {
                invalidateAndRebuildChunkMeshImmediate(position);
            }
            return removed;
        }
        return false;
    }
    
    /**
     * Check if a block exists at the specified position
     */
    public boolean hasBlock(Vector3 position) {
        if (chunkManager != null) {
            return chunkManager.getBlockAt(position) != null;
        }
        return false;
    }
    
    /**
     * Check if a block exists at world coordinates (for DDA raycast).
     * Uses floor for block position: (5.3, 64.7, 10.1) → block (5, 64, 10)
     */
    public boolean hasBlockAt(float worldX, float worldY, float worldZ) {
        if (chunkManager != null) {
            return chunkManager.hasBlockAt(worldX, worldY, worldZ);
        }
        return false;
    }
    
    /**
     * Get all block positions (for collision detection)
     * @deprecated Use getNearbyBlockPositions for better performance
     */
    @Deprecated
    public List<Vector3> getBlockPositions() {
        // ✅ Prevent ConcurrentModificationException with snapshot approach
        if (chunkManager != null) {
            return chunkManager.getBlockPositionsSnapshot();
        }
        return new ArrayList<>();
    }
    
    /**
     * Get nearby block positions (only chunks around player)
     * Used for physics/collision calculations
     */
    public List<Vector3> getNearbyBlockPositions(float playerX, float playerZ, int chunkRadius) {
        List<Vector3> positions = new ArrayList<>();
        
        if (chunkManager != null) {
            List<Chunk> nearbyChunks = chunkManager.getNearbyChunks(playerX, playerZ, chunkRadius);
            
            for (Chunk chunk : nearbyChunks) {
                for (Chunk.BlockData block : chunk.getBlocks()) {
                    // ✅ BlockPos → Vector3 conversion
                    positions.add(block.getWorldPos(chunk.getCoord()));
                }
            }
        }
        
        return positions;
    }
    
    /**
     * Get chunk coordinate at world position
     */
    public ChunkCoord getChunkCoordAt(float worldX, float worldZ) {
        return ChunkCoord.fromWorldPos(worldX, worldZ, Chunk.CHUNK_SIZE);
    }
    
    /**
     * Get all block model instances for rendering (Chunk 단위, 변경 시에만 재구성)
     */
    public List<ModelInstance> getAllBlockInstances() {
        return getAllBlockInstances(null);
    }
    
    /**
     * Get all block model instances for rendering with Frustum Culling
     * @param camera Camera (for Frustum Culling, returns all chunks if null)
     */
    public List<ModelInstance> getAllBlockInstances(com.badlogic.gdx.graphics.Camera camera) {
        if (chunkManager == null) {
            return new ArrayList<>();
        }
        
        if (camera != null) {
            long t0 = PerformanceLogger.now();
            List<ModelInstance> result = getFrustumCulledInstances(camera);
            PerformanceLogger.log("World", "getFrustumCulledInstances " + result.size() + " chunks", PerformanceLogger.now() - t0);
            return result;
        }
        
        List<ModelInstance> result = new ArrayList<>();
        for (Chunk chunk : chunkManager.getLoadedChunks()) {
            if (chunk.hasMesh() && chunk.getMesh().hasInstance()) {
                result.add(chunk.getMesh().getInstance());
            }
        }
        return result;
    }
    
    /**
     * Frustum Culling: Return only visible chunks on screen
     */
    private List<ModelInstance> getFrustumCulledInstances(com.badlogic.gdx.graphics.Camera camera) {
        List<ModelInstance> visibleInstances = new ArrayList<>();
        int culledCount = 0;
        
        for (Chunk chunk : chunkManager.getLoadedChunks()) {
            if (chunk.hasMesh() && chunk.getMesh().hasInstance()) {
                // Check frustum
                if (camera.frustum.boundsInFrustum(chunk.getBounds())) {
                    visibleInstances.add(chunk.getMesh().getInstance());
                } else {
                    culledCount++;
                }
            }
        }
        
        return visibleInstances;
    }
    
    
    /**
     * Build mesh for a single chunk (unified mesh: 1 Chunk = 1 Draw Call)
     * - Face Culling
     * - Fully Occluded Block Removal
     */
    private void buildChunkMesh(Chunk chunk) {
        long t0 = PerformanceLogger.now();
        List<BlockManager.BlockData> blockDataList = new ArrayList<>();
        Map<Vector3, boolean[]> visibleFacesMap = new HashMap<>();
        int skippedBlocks = 0;
        
        for (Chunk.BlockData block : chunk.getBlocks()) {
            // ✅ BlockPos → Vector3 conversion
            Vector3 pos = block.getWorldPos(chunk.getCoord());
            
            // Face Culling: Check visibility of each face
            boolean[] visibleFaces = new boolean[6];
            visibleFaces[0] = !chunkManager.hasBlockAt(pos.x, pos.y, pos.z + 1); // Front (z+)
            visibleFaces[1] = !chunkManager.hasBlockAt(pos.x, pos.y, pos.z - 1); // Back (z-)
            visibleFaces[2] = !chunkManager.hasBlockAt(pos.x - 1, pos.y, pos.z); // Left (x-)
            visibleFaces[3] = !chunkManager.hasBlockAt(pos.x + 1, pos.y, pos.z); // Right (x+)
            visibleFaces[4] = !chunkManager.hasBlockAt(pos.x, pos.y + 1, pos.z); // Top (y+)
            visibleFaces[5] = !chunkManager.hasBlockAt(pos.x, pos.y - 1, pos.z); // Bottom (y-)
            
            // Remove fully occluded blocks (all faces hidden)
            if (!visibleFaces[0] && !visibleFaces[1] && !visibleFaces[2] && 
                !visibleFaces[3] && !visibleFaces[4] && !visibleFaces[5]) {
                skippedBlocks++;
                continue; // Don't add fully occluded blocks to mesh
            }
            
            // Add to unified mesh
            blockDataList.add(new BlockManager.BlockData(pos, block.blockType));
            visibleFacesMap.put(pos, visibleFaces);
        }
        
        // Create entire chunk as one unified mesh (1 Draw Call!)
        Model chunkModel = blockManager.createChunkMesh(blockDataList, visibleFacesMap);
        
        ChunkMesh mesh = new ChunkMesh();
        mesh.setModel(chunkModel);
        chunk.setMesh(mesh);
        
        if (PerformanceLogger.ENABLED) {
            long ms = PerformanceLogger.now() - t0;
            System.out.printf("[PERF][World] buildChunkMesh chunk(%d,%d): %d ms, blocks=%d, skipped=%d%n",
                chunk.getCoord().x, chunk.getCoord().z, ms, blockDataList.size(), skippedBlocks);
        }
    }
    
    /**
     * 블록 변경 시 즉시 무효화 + 재빌드 (깜빡임 방지)
     * 사용자 액션(add/remove block)에서만 호출
     */
    private void invalidateAndRebuildChunkMeshImmediate(Vector3 position) {
        if (chunkManager == null) return;
        
        ChunkCoord coord = ChunkCoord.fromWorldPos(position.x, position.z, Chunk.CHUNK_SIZE);
        invalidateAndRebuildChunkAtImmediate(coord);
        
        int blockX = (int) Math.floor(position.x);
        int blockZ = (int) Math.floor(position.z);
        int localX = Math.floorMod(blockX, Chunk.CHUNK_SIZE);
        int localZ = Math.floorMod(blockZ, Chunk.CHUNK_SIZE);
        
        if (localX == 0) invalidateAndRebuildChunkAtImmediate(new ChunkCoord(coord.x - 1, coord.z));
        else if (localX == Chunk.CHUNK_SIZE - 1) invalidateAndRebuildChunkAtImmediate(new ChunkCoord(coord.x + 1, coord.z));
        if (localZ == 0) invalidateAndRebuildChunkAtImmediate(new ChunkCoord(coord.x, coord.z - 1));
        else if (localZ == Chunk.CHUNK_SIZE - 1) invalidateAndRebuildChunkAtImmediate(new ChunkCoord(coord.x, coord.z + 1));
        if (localX == 0 && localZ == 0) invalidateAndRebuildChunkAtImmediate(new ChunkCoord(coord.x - 1, coord.z - 1));
        else if (localX == 0 && localZ == Chunk.CHUNK_SIZE - 1) invalidateAndRebuildChunkAtImmediate(new ChunkCoord(coord.x - 1, coord.z + 1));
        else if (localX == Chunk.CHUNK_SIZE - 1 && localZ == 0) invalidateAndRebuildChunkAtImmediate(new ChunkCoord(coord.x + 1, coord.z - 1));
        else if (localX == Chunk.CHUNK_SIZE - 1 && localZ == Chunk.CHUNK_SIZE - 1) invalidateAndRebuildChunkAtImmediate(new ChunkCoord(coord.x + 1, coord.z + 1));
    }
    
    /** 즉시 무효화 + 재빌드 (dirty 큐 사용 안 함) */
    private void invalidateAndRebuildChunkAtImmediate(ChunkCoord coord) {
        if (chunkManager == null) return;
        Chunk chunk = chunkManager.getChunk(coord);
        if (chunk == null || !chunk.isGenerated()) return;
        
        if (chunk.hasMesh()) chunk.setMesh(null);
        buildChunkMesh(chunk);
    }
    
    /**
     * Invalidate chunk mesh (청크 로드 시 인접 청크용 - dirty 큐에 추가)
     */
    private void invalidateChunkAt(ChunkCoord coord) {
        if (chunkManager != null) {
            Chunk chunk = chunkManager.getChunk(coord);
            if (chunk != null && chunk.hasMesh()) {
                chunk.setMesh(null);
            }
            chunkManager.addDirtyChunk(coord);
        }
    }
    
    /**
     * Get block type at position
     */
    public int getBlockType(Vector3 position) {
        if (chunkManager != null) {
            Chunk.BlockData block = chunkManager.getBlockAt(position);
            return block != null ? block.blockType : -1;
        }
        return -1;
    }
    
    /**
     * Get total block count
     */
    public int getBlockCount() {
        if (chunkManager != null) {
            return chunkManager.getAllBlocks().size();
        }
        return 0;
    }
    
    public BlockManager getBlockManager() {
        return blockManager;
    }
    
    public ChunkManager getChunkManager() {
        return chunkManager;
    }
    
    /**
     * Clean up resources
     */
    public void dispose() {
        if (chunkManager != null) {
            chunkManager.shutdown();
        }
    }
}
