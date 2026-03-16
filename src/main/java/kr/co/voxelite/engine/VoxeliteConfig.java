package kr.co.voxelite.engine;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.world.IChunkGenerator;
import kr.co.voxelite.world.IChunkLoadPolicy;

/**
 * Configuration for VoxeliteEngine.
 */
public class VoxeliteConfig {
    public int worldWidth = 11;
    public int worldHeight = 11;
    public float groundLevel = -1f;
    public boolean autoCreateGround = true;
    public int defaultGroundBlockType = 0;
    public boolean useRandomTerrain = false;
    public long worldSeed = 0L;
    public IChunkGenerator chunkGenerator = null;
    public IChunkLoadPolicy chunkLoadPolicy = null;
    public int initialChunkRadius = 16;
    public int chunkPreloadRadius = 1;
    public String worldSavePath = "saves/world1";
    public Vector3 playerStartPosition = new Vector3(0f, -0.5f, 0f);
    public float gravity = -20f;
    public float jumpVelocity = 7f;
    public VoxeliteConfig() {
    }

    public static Builder builder() {
        return new Builder();
    }

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

        public Builder defaultGroundBlockType(int blockType) {
            config.defaultGroundBlockType = blockType;
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
}
