package kr.co.voxelite.world;

import com.badlogic.gdx.graphics.g3d.Model;
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
    private List<ModelInstance> cachedInstances;
    private boolean instancesDirty = true;
    private int lastBlockCount = 0; // 블록 수 추적

    public World(BlockManager blockManager) {
        this.blockManager = blockManager;
        this.cachedInstances = new ArrayList<>();
    }
    
    /**
     * Initialize with chunk-based terrain generation (정책 주입)
     */
    public void initWithChunks(String worldPath, int defaultBlockType, 
                              IChunkGenerator generator, IChunkLoadPolicy loadPolicy) {
        this.chunkManager = new ChunkManager(worldPath, defaultBlockType, generator, loadPolicy);
    }
    
    /**
     * Generate initial spawn chunks
     * @param totalRadius - 파일로 생성할 총 반경
     * @param loadRadius - 메모리에 로드할 반경
     */
    public float generateInitialChunks(float spawnX, float spawnZ, int totalRadius, int loadRadius) {
        if (chunkManager == null) {
            throw new IllegalStateException("ChunkManager not initialized. Call initWithChunks() first.");
        }
        
        chunkManager.generateInitialChunks(spawnX, spawnZ, totalRadius, loadRadius);
        instancesDirty = true;  // 초기 생성 시 메시 빌드 필요
        
        return chunkManager.getChunkCenterHeight(spawnX, spawnZ);
    }
    
    /**
     * Update chunks based on player position (Tick 기반, 10Hz)
     */
    public void updateChunks(float playerX, float playerZ) {
        if (chunkManager != null) {
            chunkManager.updateLoadedChunks(playerX, playerZ);
            
            // 청크가 추가/제거되었을 때만 재구성
            if (chunkManager.consumeChunksChanged()) {
                instancesDirty = true;
            }
        }
    }
    
    /**
     * Process pending chunks (매 프레임 호출)
     */
    public void processPendingChunks() {
        if (chunkManager != null) {
            chunkManager.processPendingChunksPublic();
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
        cachedInstances.clear();
        instancesDirty = true;
    }

    /**
     * Add a block at the specified position with block type
     */
    public void addBlock(Vector3 position, int blockType) {
        if (chunkManager != null) {
            chunkManager.addBlock(position, blockType);
            invalidateChunkMesh(position);
            instancesDirty = true;
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
                invalidateChunkMesh(position);
                instancesDirty = true;
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
     * Get all block positions (for collision detection)
     * @deprecated 성능 문제로 getNearbyBlockPositions 사용 권장
     */
    @Deprecated
    public List<Vector3> getBlockPositions() {
        List<Vector3> positions = new ArrayList<>();
        
        if (chunkManager != null) {
            for (Chunk.BlockData block : chunkManager.getAllBlocks()) {
                positions.add(block.position);
            }
        }
        
        return positions;
    }
    
    /**
     * Get nearby block positions (플레이어 주변 청크만)
     * 물리/충돌 계산에 사용
     */
    public List<Vector3> getNearbyBlockPositions(float playerX, float playerZ, int chunkRadius) {
        List<Vector3> positions = new ArrayList<>();
        
        if (chunkManager != null) {
            List<Chunk> nearbyChunks = chunkManager.getNearbyChunks(playerX, playerZ, chunkRadius);
            
            for (Chunk chunk : nearbyChunks) {
                for (Chunk.BlockData block : chunk.getBlocks()) {
                    positions.add(block.position);
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
     * @param camera 카메라 (Frustum Culling용, null이면 모든 청크 반환)
     */
    public List<ModelInstance> getAllBlockInstances(com.badlogic.gdx.graphics.Camera camera) {
        if (chunkManager == null) {
            return cachedInstances;
        }
        
        // instancesDirty일 때만 재구성
        if (instancesDirty) {
            rebuildAllChunkMeshes();
        }
        
        // Frustum Culling 적용
        if (camera != null) {
            return getFrustumCulledInstances(camera);
        }
        
        return cachedInstances;
    }
    
    /**
     * Frustum Culling: 화면에 보이는 청크만 반환
     */
    private List<ModelInstance> getFrustumCulledInstances(com.badlogic.gdx.graphics.Camera camera) {
        List<ModelInstance> visibleInstances = new ArrayList<>();
        int culledCount = 0;
        
        for (Chunk chunk : chunkManager.getLoadedChunks()) {
            if (chunk.hasMesh() && chunk.getMesh().hasInstance()) {
                // Frustum 체크
                if (camera.frustum.boundsInFrustum(chunk.getBounds())) {
                    visibleInstances.add(chunk.getMesh().getInstance());
                } else {
                    culledCount++;
                }
            }
        }
        
        if (culledCount > 0) {
            System.out.println("[World] Frustum Culling: " + culledCount + " chunks culled");
        }
        
        return visibleInstances;
    }
    
    /**
     * Rebuild all chunk meshes and collect instances (청크 단위 통합 메시)
     */
    private void rebuildAllChunkMeshes() {
        // 1. 생성되었지만 메시가 없는 청크의 메시 빌드
        for (Chunk chunk : chunkManager.getLoadedChunks()) {
            if (chunk.isGenerated() && !chunk.hasMesh()) {
                buildChunkMesh(chunk);
            }
        }
        
        // 2. 모든 청크의 통합 메시 수집 (1 청크 = 1 ModelInstance)
        cachedInstances.clear();
        for (Chunk chunk : chunkManager.getLoadedChunks()) {
            if (chunk.hasMesh() && chunk.getMesh().hasInstance()) {
                cachedInstances.add(chunk.getMesh().getInstance());
            }
        }
        
        instancesDirty = false;
    }
    
    /**
     * Build mesh for a single chunk (통합 메시: 1 Chunk = 1 Draw Call)
     * - Face Culling
     * - Fully Occluded Block Removal
     */
    private void buildChunkMesh(Chunk chunk) {
        List<BlockManager.BlockData> blockDataList = new ArrayList<>();
        Map<Vector3, boolean[]> visibleFacesMap = new HashMap<>();
        int skippedBlocks = 0;
        
        for (Chunk.BlockData block : chunk.getBlocks()) {
            Vector3 pos = block.position;
            
            // Face Culling: 각 면의 가시성 체크
            boolean[] visibleFaces = new boolean[6];
            visibleFaces[0] = !chunkManager.hasBlockAt(pos.x, pos.y, pos.z + 1); // Front (z+)
            visibleFaces[1] = !chunkManager.hasBlockAt(pos.x, pos.y, pos.z - 1); // Back (z-)
            visibleFaces[2] = !chunkManager.hasBlockAt(pos.x - 1, pos.y, pos.z); // Left (x-)
            visibleFaces[3] = !chunkManager.hasBlockAt(pos.x + 1, pos.y, pos.z); // Right (x+)
            visibleFaces[4] = !chunkManager.hasBlockAt(pos.x, pos.y + 1, pos.z); // Top (y+)
            visibleFaces[5] = !chunkManager.hasBlockAt(pos.x, pos.y - 1, pos.z); // Bottom (y-)
            
            // 완전히 가려진 블록 제거 (모든 면이 숨겨진 경우)
            if (!visibleFaces[0] && !visibleFaces[1] && !visibleFaces[2] && 
                !visibleFaces[3] && !visibleFaces[4] && !visibleFaces[5]) {
                skippedBlocks++;
                continue; // 완전히 가려진 블록은 메시에 추가하지 않음
            }
            
            // 통합 메시에 추가
            blockDataList.add(new BlockManager.BlockData(pos, block.blockType));
            visibleFacesMap.put(pos, visibleFaces);
        }
        
        if (skippedBlocks > 0) {
            System.out.println("[World] Chunk " + chunk.getCoord() + 
                ": skipped " + skippedBlocks + " fully occluded blocks");
        }
        
        // 청크 전체를 1개 통합 메시로 생성 (1 Draw Call!)
        Model chunkModel = blockManager.createChunkMesh(blockDataList, visibleFacesMap);
        
        ChunkMesh mesh = new ChunkMesh();
        mesh.setModel(chunkModel);
        chunk.setMesh(mesh);
    }
    
    /**
     * Invalidate chunk mesh (블록 변경 시)
     * + 인접 청크도 무효화 (Face Culling 때문에)
     */
    private void invalidateChunkMesh(Vector3 position) {
        if (chunkManager != null) {
            ChunkCoord coord = ChunkCoord.fromWorldPos(position.x, position.z, Chunk.CHUNK_SIZE);
            
            // 해당 청크 무효화
            invalidateChunkAt(coord);
            
            // 블록 위치가 청크 경계인지 확인하여 인접 청크도 무효화
            int localX = (int)position.x & (Chunk.CHUNK_SIZE - 1);
            int localZ = (int)position.z & (Chunk.CHUNK_SIZE - 1);
            
            // X 경계 체크
            if (localX == 0) {
                invalidateChunkAt(new ChunkCoord(coord.x - 1, coord.z)); // 왼쪽 청크
            } else if (localX == Chunk.CHUNK_SIZE - 1) {
                invalidateChunkAt(new ChunkCoord(coord.x + 1, coord.z)); // 오른쪽 청크
            }
            
            // Z 경계 체크
            if (localZ == 0) {
                invalidateChunkAt(new ChunkCoord(coord.x, coord.z - 1)); // 뒤쪽 청크
            } else if (localZ == Chunk.CHUNK_SIZE - 1) {
                invalidateChunkAt(new ChunkCoord(coord.x, coord.z + 1)); // 앞쪽 청크
            }
            
            // 대각선 코너 체크
            if (localX == 0 && localZ == 0) {
                invalidateChunkAt(new ChunkCoord(coord.x - 1, coord.z - 1));
            } else if (localX == 0 && localZ == Chunk.CHUNK_SIZE - 1) {
                invalidateChunkAt(new ChunkCoord(coord.x - 1, coord.z + 1));
            } else if (localX == Chunk.CHUNK_SIZE - 1 && localZ == 0) {
                invalidateChunkAt(new ChunkCoord(coord.x + 1, coord.z - 1));
            } else if (localX == Chunk.CHUNK_SIZE - 1 && localZ == Chunk.CHUNK_SIZE - 1) {
                invalidateChunkAt(new ChunkCoord(coord.x + 1, coord.z + 1));
            }
        }
    }
    
    /**
     * Helper: 특정 청크의 메시 무효화
     */
    private void invalidateChunkAt(ChunkCoord coord) {
        if (chunkManager != null) {
            Chunk chunk = chunkManager.getChunk(coord);
            if (chunk != null && chunk.hasMesh()) {
                chunk.getMesh().clear();
            }
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
