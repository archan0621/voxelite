package kr.co.voxelite.engine;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.entity.Player;
import kr.co.voxelite.physics.PhysicsSystem;
import kr.co.voxelite.util.PerformanceLogger;
import kr.co.voxelite.world.BlockManager;
import kr.co.voxelite.world.World;

/**
 * Headless facade for Voxelite core systems.
 */
public class VoxeliteEngine {
    private static final float CHUNK_UPDATE_INTERVAL = 0.05f;

    private final VoxeliteConfig config;
    private World world;
    private Player player;
    private PhysicsSystem physics;
    private boolean initialized = false;
    private float chunkUpdateAccumulator = 0f;

    public VoxeliteEngine() {
        this(new VoxeliteConfig(), null);
    }

    public VoxeliteEngine(VoxeliteConfig config) {
        this(config, null);
    }

    public VoxeliteEngine(VoxeliteConfig config, Player customPlayer) {
        this.config = config;
        player = customPlayer;
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        world = new World(new BlockManager());
        float spawnY = config.playerStartPosition.y;

        if (config.chunkGenerator != null && config.chunkLoadPolicy != null) {
            world.initWithChunks(config.worldSavePath, config.defaultGroundBlockType, config.chunkGenerator, config.chunkLoadPolicy);
            if (config.autoCreateGround) {
                float terrainHeight = world.generateInitialChunks(
                    config.playerStartPosition.x,
                    config.playerStartPosition.z,
                    config.initialChunkRadius,
                    config.chunkPreloadRadius
                );
                spawnY = terrainHeight + 2f;
            }
        }

        Vector3 adjustedSpawn = new Vector3(config.playerStartPosition.x, spawnY, config.playerStartPosition.z);
        if (player == null) {
            player = new Player(adjustedSpawn);
        } else {
            player.setPosition(adjustedSpawn);
        }

        physics = new PhysicsSystem(world);
        initialized = true;
    }

    public void update(float delta) {
        if (!initialized) {
            throw new IllegalStateException("Engine not initialized. Call initialize() first.");
        }

        float safeDelta = normalizeDelta(delta);
        chunkUpdateAccumulator += safeDelta;
        if (chunkUpdateAccumulator >= CHUNK_UPDATE_INTERVAL && world.getChunkManager() != null) {
            long t0 = PerformanceLogger.now();
            Vector3 playerPos = player.getPosition();
            world.updateChunks(playerPos.x, playerPos.z);
            PerformanceLogger.log("Engine", "updateChunks", PerformanceLogger.now() - t0);
            chunkUpdateAccumulator -= CHUNK_UPDATE_INTERVAL;
        }

        if (world.getChunkManager() != null) {
            long t0 = PerformanceLogger.now();
            world.processPendingChunks();
            PerformanceLogger.log("Engine", "processPendingChunks", PerformanceLogger.now() - t0);
        }
    }

    public void dispose() {
        if (world != null) {
            world.dispose();
        }
        initialized = false;
    }

    public World getWorld() {
        return world;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public PhysicsSystem getPhysics() {
        return physics;
    }

    public void addBlock(Vector3 position, int blockType) {
        if (world != null) {
            world.addBlock(position, blockType);
            if (physics != null) {
                physics.invalidateCache();
            }
        }
    }

    public void addBlock(Vector3 position) {
        addBlock(position, 0);
    }

    public boolean removeBlock(Vector3 position) {
        if (world != null) {
            boolean removed = world.removeBlock(position);
            if (removed && physics != null) {
                physics.invalidateCache();
            }
            return removed;
        }
        return false;
    }

    public void updatePhysics(float delta) {
        if (initialized && physics != null && player != null) {
            physics.update(player, normalizeDelta(delta));
        }
    }

    public static Builder builder() {
        return new Builder(null);
    }

    public static Builder builder(Player customPlayer) {
        return new Builder(customPlayer);
    }

    private float normalizeDelta(float delta) {
        if (!Float.isFinite(delta) || delta < 0f) {
            return 0f;
        }
        return delta;
    }

    public static class Builder {
        private final VoxeliteConfig.Builder configBuilder = VoxeliteConfig.builder();
        private final Player customPlayer;

        private Builder(Player customPlayer) {
            this.customPlayer = customPlayer;
        }

        public Builder worldSize(int width, int height) {
            configBuilder.worldSize(width, height);
            return this;
        }

        public Builder groundLevel(float level) {
            configBuilder.groundLevel(level);
            return this;
        }

        public Builder playerStart(float x, float y, float z) {
            configBuilder.playerStart(x, y, z);
            return this;
        }

        public Builder autoCreateGround(boolean auto) {
            configBuilder.autoCreateGround(auto);
            return this;
        }

        public Builder defaultGroundBlockType(int blockType) {
            configBuilder.defaultGroundBlockType(blockType);
            return this;
        }

        public Builder worldSeed(long seed) {
            configBuilder.worldSeed(seed);
            return this;
        }

        public Builder chunkGenerator(kr.co.voxelite.world.IChunkGenerator generator) {
            configBuilder.chunkGenerator(generator);
            return this;
        }

        public Builder chunkLoadPolicy(kr.co.voxelite.world.IChunkLoadPolicy policy) {
            configBuilder.chunkLoadPolicy(policy);
            return this;
        }

        public Builder initialChunkRadius(int radius) {
            configBuilder.initialChunkRadius(radius);
            return this;
        }

        public Builder chunkPreloadRadius(int radius) {
            configBuilder.chunkPreloadRadius(radius);
            return this;
        }

        public Builder worldSavePath(String path) {
            configBuilder.worldSavePath(path);
            return this;
        }

        public VoxeliteEngine build() {
            return new VoxeliteEngine(configBuilder.build(), customPlayer);
        }
    }
}
