package kr.co.voxelite.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import kr.co.voxelite.camera.CameraController;
import kr.co.voxelite.camera.FPSCamera;
import kr.co.voxelite.entity.Player;
import kr.co.voxelite.input.InputHandler;
import kr.co.voxelite.physics.PhysicsSystem;
import kr.co.voxelite.physics.RayCaster;
import kr.co.voxelite.physics.RaycastHit;
import kr.co.voxelite.render.Renderer;
import kr.co.voxelite.world.*;

import java.util.List;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

/**
 * High-level facade for Voxelite engine
 * Simplifies initialization and provides unified game loop
 */
public class VoxeliteEngine {
    private final VoxeliteConfig config;
    
    // Core systems
    private World world;
    private Player player;
    private FPSCamera camera;
    private PhysicsSystem physics;
    private InputHandler input;
    private CameraController cameraController;
    private Renderer renderer;
    
    private BlockManager blockManager;
    
    // Screen dimensions
    private int screenWidth;
    private int screenHeight;
    
    // Selected block for raycasting
    private Vector3 selectedBlock;
    private RaycastHit raycastHit;
    
    private boolean initialized = false;
    
    // Tick system for chunk updates
    private static final float CHUNK_UPDATE_INTERVAL = 0.05f; // 20Hz (50ms)
    private float chunkUpdateAccumulator = 0f;
    
    /**
     * Create engine with default configuration
     */
    public VoxeliteEngine() {
        this(new VoxeliteConfig(), null);
    }
    
    /**
     * Create engine with custom configuration
     */
    public VoxeliteEngine(VoxeliteConfig config) {
        this(config, null);
    }
    
    /**
     * Create engine with custom configuration and custom player
     * Allows game applications to provide their own Player subclass
     */
    public VoxeliteEngine(VoxeliteConfig config, Player customPlayer) {
        this.config = config;
        this.player = customPlayer;  // Will be initialized later if null
    }
    
    /**
     * Initialize all engine systems
     * Must be called before using the engine
     */
    public void initialize(int screenWidth, int screenHeight) {
        if (initialized) {
            return;
        }
        
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        // OpenGL setup (skip if running in headless mode)
        if (Gdx.gl != null) {
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glEnable(GL20.GL_CULL_FACE);
            Gdx.gl.glCullFace(GL20.GL_BACK);
        }
        
        // Viewport setup (skip if graphics not available)
        if (Gdx.graphics != null) {
            int backBufferWidth = Gdx.graphics.getBackBufferWidth();
            int backBufferHeight = Gdx.graphics.getBackBufferHeight();
            if (Gdx.gl != null) {
                Gdx.gl.glViewport(0, 0, backBufferWidth, backBufferHeight);
            }
        }
        
        // Create world with texture atlas
        if (config.textureAtlasPath != null) {
            blockManager = new BlockManager(config.textureAtlasPath);
        } else {
            blockManager = new BlockManager();
        }
        world = new World(blockManager);
        
        // Optionally create ground
        float spawnY = config.playerStartPosition.y;
        if (config.autoCreateGround) {
            if (config.chunkGenerator != null && config.chunkLoadPolicy != null) {
                // Chunk system (policy injection)
                world.initWithChunks(config.worldSavePath, config.defaultGroundBlockType,
                                    config.chunkGenerator, config.chunkLoadPolicy);
                float terrainHeight = world.generateInitialChunks(
                    config.playerStartPosition.x, 
                    config.playerStartPosition.z, 
                    config.initialChunkRadius,
                    config.chunkPreloadRadius
                );
                spawnY = terrainHeight + 2f;
            } else if (config.useRandomTerrain) {
                // Legacy random terrain (deprecated)
                // Note: World.generateRandomTerrain() method no longer exists
                // Use chunk system instead
                throw new UnsupportedOperationException("Use chunk system instead: .useChunkSystem(true)");
            } else {
                // Legacy flat terrain (deprecated)
                throw new UnsupportedOperationException("Use chunk system instead: .useChunkSystem(true)");
            }
        }
        
        // Create player (only if not provided by application)
        if (player == null) {
            Vector3 adjustedSpawn = new Vector3(config.playerStartPosition.x, spawnY, config.playerStartPosition.z);
            player = new Player(adjustedSpawn);
        } else {
            // Custom player provided by application - just set position
            Vector3 adjustedSpawn = new Vector3(config.playerStartPosition.x, spawnY, config.playerStartPosition.z);
            player.setPosition(adjustedSpawn);
        }
        
        // Create camera
        camera = new FPSCamera(config.fieldOfView, screenWidth, screenHeight);
        camera.setPitch(config.initialPitch);
        
        // Create systems
        physics = new PhysicsSystem(world);
        input = new InputHandler();
        input.getMouseHandler().setMouseSensitivity(config.mouseSensitivity);
        
        // Create camera controller
        cameraController = new CameraController(camera, player, physics, input);
        cameraController.setMoveSpeed(config.playerMoveSpeed);
        
        // Create renderer
        renderer = new Renderer(screenWidth, screenHeight);
        
        initialized = true;
    }
    
    /**
     * Update all engine systems
     * Call this every frame before render()
     */
    public void update(float delta) {
        if (!initialized) {
            throw new IllegalStateException("Engine not initialized. Call initialize() first.");
        }
        
        // Tick-based chunk updates (10Hz)
        chunkUpdateAccumulator += delta;
        if (chunkUpdateAccumulator >= CHUNK_UPDATE_INTERVAL) {
            if (world.getChunkManager() != null) {
                Vector3 playerPos = player.getPosition();
                world.updateChunks(playerPos.x, playerPos.z);
            }
            chunkUpdateAccumulator -= CHUNK_UPDATE_INTERVAL;
        }
        
        // Process pending chunks every frame (fast responsiveness)
        if (world.getChunkManager() != null) {
            world.processPendingChunks();
        }
        
        // Update input
        input.update(delta);
        
        // Update camera and player
        cameraController.update(delta);
        
        // Perform raycasting for block selection
        float screenCenterX = Gdx.graphics != null ? Gdx.graphics.getWidth() / 2f : screenWidth / 2f;
        float screenCenterY = Gdx.graphics != null ? Gdx.graphics.getHeight() / 2f : screenHeight / 2f;
        Ray ray = camera.getCamera().getPickRay(screenCenterX, screenCenterY);
        raycastHit = RayCaster.raycastWithFace(ray, world);
        selectedBlock = raycastHit != null ? raycastHit.getBlockPosition() : null;
    }
    
    /**
     * Render the world
     * Call this every frame after update()
     */
    public void render() {
        if (!initialized) {
            throw new IllegalStateException("Engine not initialized. Call initialize() first.");
        }
        
        renderer.render(camera, world, screenWidth, screenHeight, selectedBlock, player.getPosition());
    }
    
    /**
     * Handle screen resize
     */
    public void resize(int width, int height) {
        if (!initialized) {
            return;
        }
        
        this.screenWidth = width;
        this.screenHeight = height;
        
        camera.resize(width, height);
        renderer.resize(width, height);
        
        if (Gdx.graphics != null && Gdx.gl != null) {
            int backBufferWidth = Gdx.graphics.getBackBufferWidth();
            int backBufferHeight = Gdx.graphics.getBackBufferHeight();
            Gdx.gl.glViewport(0, 0, backBufferWidth, backBufferHeight);
        }
    }
    
    /**
     * Clean up resources
     */
    public void dispose() {
        if (renderer != null) {
            renderer.dispose();
        }
        if (world != null) {
            world.dispose();
        }
    }
    
    // Accessors for game logic
    
    public World getWorld() {
        return world;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public FPSCamera getCamera() {
        return camera;
    }
    
    public Vector3 getSelectedBlock() {
        return selectedBlock;
    }
    
    public RaycastHit getRaycastHit() {
        return raycastHit;
    }
    
    public InputHandler getInput() {
        return input;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public PhysicsSystem getPhysics() {
        return physics;
    }
    
    /**
     * Replace the camera controller with a custom implementation.
     * Allows game applications to provide their own camera controller logic.
     */
    public void setCameraController(kr.co.voxelite.camera.CameraController controller) {
        this.cameraController = controller;
    }
    
    /**
     * Add block to world and invalidate physics cache
     */
    public void addBlock(Vector3 position, int blockType) {
        if (world != null) {
            world.addBlock(position, blockType);
            // Invalidate physics system cache when adding block
            if (physics != null) {
                physics.invalidateCache();
            }
        }
    }
    
    /**
     * Add block to world (default blockType = 0) and invalidate physics cache
     */
    public void addBlock(Vector3 position) {
        addBlock(position, 0);
    }
    
    /**
     * Remove block from world and invalidate physics cache
     */
    public boolean removeBlock(Vector3 position) {
        if (world != null) {
            boolean removed = world.removeBlock(position);
            // Invalidate physics system cache when removing block
            if (removed && physics != null) {
                physics.invalidateCache();
            }
            return removed;
        }
        return false;
    }
    
    /**
     * Force physics update (useful after world changes)
     */
    public void updatePhysics(float delta) {
        if (initialized && physics != null && player != null) {
            physics.update(player, delta);
        }
    }
    
    // Builder pattern
    
    public static Builder builder() {
        return new Builder(null);
    }
    
    public static Builder builder(Player customPlayer) {
        return new Builder(customPlayer);
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
        
        public Builder playerSpeed(float speed) {
            configBuilder.playerSpeed(speed);
            return this;
        }
        
        public Builder fieldOfView(float fov) {
            configBuilder.fieldOfView(fov);
            return this;
        }
        
        public Builder cameraPitch(float pitch) {
            configBuilder.cameraPitch(pitch);
            return this;
        }
        
        public Builder mouseSensitivity(float sensitivity) {
            configBuilder.mouseSensitivity(sensitivity);
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
        
        public Builder textureAtlasPath(String path) {
            configBuilder.textureAtlasPath(path);
            return this;
        }
        
        public Builder worldSeed(long seed) {
            configBuilder.worldSeed(seed);
            return this;
        }
        
        public Builder chunkGenerator(IChunkGenerator generator) {
            configBuilder.chunkGenerator(generator);
            return this;
        }
        
        public Builder chunkLoadPolicy(IChunkLoadPolicy policy) {
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
