package kr.co.voxelite.engine;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.world.IChunkGenerator;
import kr.co.voxelite.world.IChunkLoadPolicy;

/**
 * Configuration for VoxeliteEngine
 */
public class VoxeliteConfig {
    // World settings
    public int worldWidth = 11;
    public int worldHeight = 11;
    public float groundLevel = -1f;
    public boolean autoCreateGround = true;
    public int defaultGroundBlockType = 0;
    public boolean useRandomTerrain = false;
    public long worldSeed = 0L;
    // Chunk system (정책 주입 방식)
    public IChunkGenerator chunkGenerator = null;
    public IChunkLoadPolicy chunkLoadPolicy = null;
    public int initialChunkRadius = 16; // 초기 생성 반경
    public int chunkPreloadRadius = 1; // 초기 메모리 로드 반경
    public String worldSavePath = "saves/world1";
    
    // Texture settings
    public String textureAtlasPath = null;
    
    // Player settings
    public Vector3 playerStartPosition = new Vector3(0f, -0.5f, 0f);
    public float playerMoveSpeed = 5f;
    
    // Camera settings
    public float fieldOfView = 67f;
    public float initialPitch = -20f;
    
    // Physics settings
    public float gravity = -20f;
    public float jumpVelocity = 7f;
    
    // Input settings
    public float mouseSensitivity = 0.1f;
    
    /**
     * Create default configuration
     */
    public VoxeliteConfig() {
    }
    
    /**
     * Builder for fluent configuration
     */
    public static class Builder {
        private final VoxeliteConfig config = new VoxeliteConfig();
        
        public Builder worldSize(int width, int height) {
            config.worldWidth = width;
            config.worldHeight = height;
            return this;
        }
        
        public Builder groundLevel(float level) {
            config.groundLevel = level;
            return this;
        }
        
        public Builder autoCreateGround(boolean auto) {
            config.autoCreateGround = auto;
            return this;
        }
        
        public Builder playerStart(float x, float y, float z) {
            config.playerStartPosition = new Vector3(x, y, z);
            return this;
        }
        
        public Builder playerSpeed(float speed) {
            config.playerMoveSpeed = speed;
            return this;
        }
        
        public Builder fieldOfView(float fov) {
            config.fieldOfView = fov;
            return this;
        }
        
        public Builder cameraPitch(float pitch) {
            config.initialPitch = pitch;
            return this;
        }
        
        public Builder mouseSensitivity(float sensitivity) {
            config.mouseSensitivity = sensitivity;
            return this;
        }
        
        public Builder defaultGroundBlockType(int blockType) {
            config.defaultGroundBlockType = blockType;
            return this;
        }
        
        public Builder textureAtlasPath(String path) {
            config.textureAtlasPath = path;
            return this;
        }
        
        public Builder worldSeed(long seed) {
            config.worldSeed = seed;
            return this;
        }
        
        public Builder chunkGenerator(IChunkGenerator generator) {
            config.chunkGenerator = generator;
            return this;
        }
        
        public Builder chunkLoadPolicy(IChunkLoadPolicy policy) {
            config.chunkLoadPolicy = policy;
            return this;
        }
        
        public Builder initialChunkRadius(int radius) {
            config.initialChunkRadius = radius;
            return this;
        }
        
        public Builder chunkPreloadRadius(int radius) {
            config.chunkPreloadRadius = radius;
            return this;
        }
        
        public Builder worldSavePath(String path) {
            config.worldSavePath = path;
            return this;
        }
        
        public VoxeliteConfig build() {
            return config;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
