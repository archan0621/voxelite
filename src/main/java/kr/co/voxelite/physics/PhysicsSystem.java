package kr.co.voxelite.physics;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.entity.Player;
import kr.co.voxelite.world.World;

/**
 * Minecraft-style axis-separated collision resolution.
 * Applies gravity, resolves collisions per-axis (Y → X → Z), and determines ground state.
 * Uses Fixed Timestep for frame-independent physics simulation.
 */
public class PhysicsSystem {
    private final World world;
    private static final float BLOCK_SIZE = 1.0f;
    private static final float BLOCK_HALF_SIZE = BLOCK_SIZE / 2f;
    private static final float COLLISION_MARGIN = 0.001f; // Collision margin
    private static final float GROUND_THRESHOLD = 0.02f;
    private static final float MIN_XZ_OVERLAP = 0.1f;
    
    // Fixed Timestep
    private static final float FIXED_TIMESTEP = 1f / 60f; // 60Hz physics update
    private static final float MAX_FRAME_TIME = 0.25f; // Maximum frame time (prevents spiral of death)
    private float accumulator = 0f;
    private final AABB scratchBlockAABB = new AABB(new Vector3(), BLOCK_HALF_SIZE);
    
    public PhysicsSystem(World world) {
        this.world = world;
    }
    
    /**
     * Invalidates cache when blocks change (will be updated in next physics step)
     */
    public void invalidateCache() {
        // No-op: broad-phase now queries only blocks inside the swept AABB, so there is no chunk cache to invalidate.
    }
    
    /**
     * Updates player physics with Fixed Timestep
     */
    public void update(Player player, float delta) {
        // Prevent spiral of death
        if (delta > MAX_FRAME_TIME) {
            delta = MAX_FRAME_TIME;
        }
        
        accumulator += delta;
        
        // Physics simulation with fixed timestep
        while (accumulator >= FIXED_TIMESTEP) {
            stepPhysics(player, FIXED_TIMESTEP);
            accumulator -= FIXED_TIMESTEP;
        }
        
        // Remaining time carries over to next frame
    }
    
    /**
     * Single physics step with fixed timestep
     */
    private void stepPhysics(Player player, float dt) {
        applyGravity(player, dt);
        
        float dx = player.getVelocity().x * dt;
        float dy = player.getVelocity().y * dt;
        float dz = player.getVelocity().z * dt;
        
        moveAndCollide(player, dx, dy, dz);
    }
    
    /**
     * Applies gravity and clamps to terminal velocity.
     * Skips gravity in fly mode.
     */
    private void applyGravity(Player player, float delta) {
        // Extension point: gravity can be disabled by subclasses (e.g., fly mode, creative mode)
        if (!player.isGravityEnabled()) {
            return;
        }
        
        if (!player.isOnGround()) {
            player.getVelocity().y += Player.GRAVITY * delta;
            
            if (player.getVelocity().y < Player.TERMINAL_VELOCITY) {
                player.getVelocity().y = Player.TERMINAL_VELOCITY;
            }
        }
    }
    
    /**
     * Moves player per-axis (Y → X → Z) and resolves collisions.
     *
     * Core principles:
     * 1. After each axis movement, position → AABB synchronization is mandatory
     * 2. Each axis is processed independently
     * 3. onGround is managed only during Y-axis processing (no rechecking after X/Z movement)
     */
    private void moveAndCollide(Player player, float dx, float dy, float dz) {
        // Use a copy of current position (prevents direct modification of internal reference)
        Vector3 currentPos = new Vector3(player.getPosition());

        // === Y-axis movement and collision resolution ===
        if (dy != 0) {
            currentPos.y += dy;
            player.setPosition(currentPos);  // AABB synchronized

            AABB aabb = player.getAABB();    // Get synchronized AABB
            if (checkCollisionY(aabb)) {     // Check only Y-axis collision
                if (dy > 0) {
                    // Ceiling collision
                    float ceilingY = findCeilingY(aabb);
                    currentPos.y = ceilingY - Player.HEIGHT;
                    player.setOnGround(false);  // Explicitly set to airborne on ceiling collision
                } else {
                    // Floor collision (landing)
                    float floorY = findFloorY(aabb);
                    currentPos.y = floorY;
                    player.setOnGround(true);   // Only Y-axis downward collision is considered landing
                }
                player.setPosition(currentPos);  // Re-synchronize AABB with corrected position
                player.getVelocity().y = 0;
            } else if (dy < 0) {
                // Y-axis descent with no collision → airborne state
                player.setOnGround(false);
            }
            // If dy > 0 and no collision, no change to onGround (ascending)
        }
        
        // === Cliff edge detection (immediately after Y-axis processing, before X/Z movement) ===
        // Check only when on ground: detects when walking off a cliff
        if (player.isOnGround() && dy == 0) {
            // Check if there's ground at current position
            if (!hasGroundDirectlyBelow(player)) {
                player.setOnGround(false);
            }
        }

        // === X-axis movement and collision resolution ===
        if (dx != 0) {
            currentPos.x += dx;
            player.setPosition(currentPos);  // AABB synchronized

            AABB aabb = player.getAABB();    // Get synchronized AABB
            if (checkCollisionX(aabb)) {     // Check only X-axis collision
                if (dx > 0) {
                    float wallX = findWallXPositive(aabb);
                    currentPos.x = wallX - Player.WIDTH / 2f - COLLISION_MARGIN;
                } else {
                    float wallX = findWallXNegative(aabb);
                    currentPos.x = wallX + Player.WIDTH / 2f + COLLISION_MARGIN;
                }
                player.setPosition(currentPos);  // Re-synchronize AABB with corrected position
                player.getVelocity().x = 0;
            }
        }

        // === Z-axis movement and collision resolution ===
        if (dz != 0) {
            currentPos.z += dz;
            player.setPosition(currentPos);  // AABB synchronized

            AABB aabb = player.getAABB();    // Get synchronized AABB
            if (checkCollisionZ(aabb)) {     // Check only Z-axis collision
                if (dz > 0) {
                    float wallZ = findWallZPositive(aabb);
                    currentPos.z = wallZ - Player.WIDTH / 2f - COLLISION_MARGIN;
                } else {
                    float wallZ = findWallZNegative(aabb);
                    currentPos.z = wallZ + Player.WIDTH / 2f + COLLISION_MARGIN;
                }
                player.setPosition(currentPos);  // Re-synchronize AABB with corrected position
                player.getVelocity().z = 0;
            }
        }

        // Do not recheck ground after X/Z movement: causes state flickering
    }
    
    /**
     * Checks if player AABB intersects any block (nearby chunks only)
     */
    private boolean checkCollision(AABB playerAABB) {
        int minBlockX = getMinBlockCenter(playerAABB.getMin().x);
        int maxBlockX = getMaxBlockCenter(playerAABB.getMax().x);
        int minBlockY = getMinBlockCenter(playerAABB.getMin().y);
        int maxBlockY = getMaxBlockCenter(playerAABB.getMax().y);
        int minBlockZ = getMinBlockCenter(playerAABB.getMin().z);
        int maxBlockZ = getMaxBlockCenter(playerAABB.getMax().z);

        for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
            for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                    if (!world.isSolidBlockAt(blockX, blockY, blockZ)) {
                        continue;
                    }

                    scratchBlockAABB.setCenter(blockX, blockY, blockZ);
                    if (playerAABB.intersects(scratchBlockAABB)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks only Y-axis direction collision (ignores X/Z overlap)
     * Only checks blocks that have actual Y-axis penetration within player AABB's X/Z range
     */
    private boolean checkCollisionY(AABB playerAABB) {
        int minBlockX = getMinBlockCenter(playerAABB.getMin().x);
        int maxBlockX = getMaxBlockCenter(playerAABB.getMax().x);
        int minBlockY = getMinBlockCenter(playerAABB.getMin().y);
        int maxBlockY = getMaxBlockCenter(playerAABB.getMax().y);
        int minBlockZ = getMinBlockCenter(playerAABB.getMin().z);
        int maxBlockZ = getMaxBlockCenter(playerAABB.getMax().z);

        for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
            for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                    if (!world.isSolidBlockAt(blockX, blockY, blockZ)) {
                        continue;
                    }

                    scratchBlockAABB.setCenter(blockX, blockY, blockZ);
                    if (playerAABB.intersectsOnY(scratchBlockAABB)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks only X-axis direction collision (ignores Y/Z overlap)
     */
    private boolean checkCollisionX(AABB playerAABB) {
        int minBlockX = getMinBlockCenter(playerAABB.getMin().x);
        int maxBlockX = getMaxBlockCenter(playerAABB.getMax().x);
        int minBlockY = getMinBlockCenter(playerAABB.getMin().y);
        int maxBlockY = getMaxBlockCenter(playerAABB.getMax().y);
        int minBlockZ = getMinBlockCenter(playerAABB.getMin().z);
        int maxBlockZ = getMaxBlockCenter(playerAABB.getMax().z);

        for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
            for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                    if (!world.isSolidBlockAt(blockX, blockY, blockZ)) {
                        continue;
                    }

                    scratchBlockAABB.setCenter(blockX, blockY, blockZ);
                    if (playerAABB.intersectsOnX(scratchBlockAABB)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks only Z-axis direction collision (ignores X/Y overlap)
     */
    private boolean checkCollisionZ(AABB playerAABB) {
        int minBlockX = getMinBlockCenter(playerAABB.getMin().x);
        int maxBlockX = getMaxBlockCenter(playerAABB.getMax().x);
        int minBlockY = getMinBlockCenter(playerAABB.getMin().y);
        int maxBlockY = getMaxBlockCenter(playerAABB.getMax().y);
        int minBlockZ = getMinBlockCenter(playerAABB.getMin().z);
        int maxBlockZ = getMaxBlockCenter(playerAABB.getMax().z);

        for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
            for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                    if (!world.isSolidBlockAt(blockX, blockY, blockZ)) {
                        continue;
                    }

                    scratchBlockAABB.setCenter(blockX, blockY, blockZ);
                    if (playerAABB.intersectsOnZ(scratchBlockAABB)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Returns highest floor Y among Y-axis collision blocks (used for landing)
     * Uses per-axis collision detection - excludes blocks with X/Z boundary overlap
     */
    private float findFloorY(AABB playerAABB) {
        float highestFloor = -Float.MAX_VALUE;

        int minBlockX = getMinBlockCenter(playerAABB.getMin().x);
        int maxBlockX = getMaxBlockCenter(playerAABB.getMax().x);
        int minBlockY = getMinBlockCenter(playerAABB.getMin().y);
        int maxBlockY = getMaxBlockCenter(playerAABB.getMax().y);
        int minBlockZ = getMinBlockCenter(playerAABB.getMin().z);
        int maxBlockZ = getMaxBlockCenter(playerAABB.getMax().z);

        for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
            for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                    if (!world.isSolidBlockAt(blockX, blockY, blockZ)) {
                        continue;
                    }

                    scratchBlockAABB.setCenter(blockX, blockY, blockZ);
                    if (!playerAABB.intersectsOnY(scratchBlockAABB)) {
                        continue;
                    }

                    float blockTop = blockY + BLOCK_HALF_SIZE;
                    if (blockTop > highestFloor) {
                        highestFloor = blockTop;
                    }
                }
            }
        }

        return highestFloor;
    }

    /**
     * Returns lowest ceiling Y among Y-axis collision blocks (used for jump ceiling collision)
     */
    private float findCeilingY(AABB playerAABB) {
        float lowestCeiling = Float.MAX_VALUE;

        int minBlockX = getMinBlockCenter(playerAABB.getMin().x);
        int maxBlockX = getMaxBlockCenter(playerAABB.getMax().x);
        int minBlockY = getMinBlockCenter(playerAABB.getMin().y);
        int maxBlockY = getMaxBlockCenter(playerAABB.getMax().y);
        int minBlockZ = getMinBlockCenter(playerAABB.getMin().z);
        int maxBlockZ = getMaxBlockCenter(playerAABB.getMax().z);

        for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
            for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                    if (!world.isSolidBlockAt(blockX, blockY, blockZ)) {
                        continue;
                    }

                    scratchBlockAABB.setCenter(blockX, blockY, blockZ);
                    if (!playerAABB.intersectsOnY(scratchBlockAABB)) {
                        continue;
                    }

                    float blockBottom = blockY - BLOCK_HALF_SIZE;
                    if (blockBottom < lowestCeiling) {
                        lowestCeiling = blockBottom;
                    }
                }
            }
        }

        return lowestCeiling;
    }

    /**
     * Returns leftmost (min X) wall among X-axis collision blocks (used for rightward movement)
     */
    private float findWallXPositive(AABB playerAABB) {
        float leftmostWall = Float.MAX_VALUE;

        int minBlockX = getMinBlockCenter(playerAABB.getMin().x);
        int maxBlockX = getMaxBlockCenter(playerAABB.getMax().x);
        int minBlockY = getMinBlockCenter(playerAABB.getMin().y);
        int maxBlockY = getMaxBlockCenter(playerAABB.getMax().y);
        int minBlockZ = getMinBlockCenter(playerAABB.getMin().z);
        int maxBlockZ = getMaxBlockCenter(playerAABB.getMax().z);

        for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
            for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                    if (!world.isSolidBlockAt(blockX, blockY, blockZ)) {
                        continue;
                    }

                    scratchBlockAABB.setCenter(blockX, blockY, blockZ);
                    if (!playerAABB.intersectsOnX(scratchBlockAABB)) {
                        continue;
                    }

                    float blockLeft = blockX - BLOCK_HALF_SIZE;
                    if (blockLeft < leftmostWall) {
                        leftmostWall = blockLeft;
                    }
                }
            }
        }

        return leftmostWall;
    }

    /**
     * Returns rightmost (max X) wall among X-axis collision blocks (used for leftward movement)
     */
    private float findWallXNegative(AABB playerAABB) {
        float rightmostWall = -Float.MAX_VALUE;

        int minBlockX = getMinBlockCenter(playerAABB.getMin().x);
        int maxBlockX = getMaxBlockCenter(playerAABB.getMax().x);
        int minBlockY = getMinBlockCenter(playerAABB.getMin().y);
        int maxBlockY = getMaxBlockCenter(playerAABB.getMax().y);
        int minBlockZ = getMinBlockCenter(playerAABB.getMin().z);
        int maxBlockZ = getMaxBlockCenter(playerAABB.getMax().z);

        for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
            for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                    if (!world.isSolidBlockAt(blockX, blockY, blockZ)) {
                        continue;
                    }

                    scratchBlockAABB.setCenter(blockX, blockY, blockZ);
                    if (!playerAABB.intersectsOnX(scratchBlockAABB)) {
                        continue;
                    }

                    float blockRight = blockX + BLOCK_HALF_SIZE;
                    if (blockRight > rightmostWall) {
                        rightmostWall = blockRight;
                    }
                }
            }
        }

        return rightmostWall;
    }

    /**
     * Returns backmost (min Z) wall among Z-axis collision blocks (used for forward movement)
     */
    private float findWallZPositive(AABB playerAABB) {
        float backmostWall = Float.MAX_VALUE;

        int minBlockX = getMinBlockCenter(playerAABB.getMin().x);
        int maxBlockX = getMaxBlockCenter(playerAABB.getMax().x);
        int minBlockY = getMinBlockCenter(playerAABB.getMin().y);
        int maxBlockY = getMaxBlockCenter(playerAABB.getMax().y);
        int minBlockZ = getMinBlockCenter(playerAABB.getMin().z);
        int maxBlockZ = getMaxBlockCenter(playerAABB.getMax().z);

        for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
            for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                    if (!world.isSolidBlockAt(blockX, blockY, blockZ)) {
                        continue;
                    }

                    scratchBlockAABB.setCenter(blockX, blockY, blockZ);
                    if (!playerAABB.intersectsOnZ(scratchBlockAABB)) {
                        continue;
                    }

                    float blockBack = blockZ - BLOCK_HALF_SIZE;
                    if (blockBack < backmostWall) {
                        backmostWall = blockBack;
                    }
                }
            }
        }

        return backmostWall;
    }

    /**
     * Returns frontmost (max Z) wall among Z-axis collision blocks (used for backward movement)
     */
    private float findWallZNegative(AABB playerAABB) {
        float frontmostWall = -Float.MAX_VALUE;

        int minBlockX = getMinBlockCenter(playerAABB.getMin().x);
        int maxBlockX = getMaxBlockCenter(playerAABB.getMax().x);
        int minBlockY = getMinBlockCenter(playerAABB.getMin().y);
        int maxBlockY = getMaxBlockCenter(playerAABB.getMax().y);
        int minBlockZ = getMinBlockCenter(playerAABB.getMin().z);
        int maxBlockZ = getMaxBlockCenter(playerAABB.getMax().z);

        for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
            for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                    if (!world.isSolidBlockAt(blockX, blockY, blockZ)) {
                        continue;
                    }

                    scratchBlockAABB.setCenter(blockX, blockY, blockZ);
                    if (!playerAABB.intersectsOnZ(scratchBlockAABB)) {
                        continue;
                    }

                    float blockFront = blockZ + BLOCK_HALF_SIZE;
                    if (blockFront > frontmostWall) {
                        frontmostWall = blockFront;
                    }
                }
            }
        }

        return frontmostWall;
    }
    
    /**
     * Checks if there's ground directly below player's feet (for cliff edge detection)
     * 
     * Call timing: Immediately after Y-axis processing, before X/Z movement
     * Purpose: Detects when walking off a cliff
     * 
     * Important: Do not call after X/Z movement (causes false positives at block boundaries)
     */
    private boolean hasGroundDirectlyBelow(Player player) {
        Vector3 pos = player.getPosition();
        float playerBottom = pos.y;
        AABB playerAABB = player.getAABB();

        int minBlockX = getMinBlockCenter(playerAABB.getMin().x);
        int maxBlockX = getMaxBlockCenter(playerAABB.getMax().x);
        int minBlockY = getMinBlockCenter(playerBottom - GROUND_THRESHOLD);
        int maxBlockY = getMaxBlockCenter(playerBottom);
        int minBlockZ = getMinBlockCenter(playerAABB.getMin().z);
        int maxBlockZ = getMaxBlockCenter(playerAABB.getMax().z);

        for (int blockX = minBlockX; blockX <= maxBlockX; blockX++) {
            for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
                for (int blockZ = minBlockZ; blockZ <= maxBlockZ; blockZ++) {
                    if (!world.isSolidBlockAt(blockX, blockY, blockZ)) {
                        continue;
                    }

                    float blockTop = blockY + BLOCK_HALF_SIZE;

                    // 1. Y-axis gap check: Is block top directly below player's feet?
                    float yGap = playerBottom - blockTop;
                    if (yGap < 0 || yGap > GROUND_THRESHOLD) {
                        continue;  // Too far or player is inside block
                    }

                    // 2. X/Z projection area overlap check: Must overlap sufficiently to be considered ground
                    scratchBlockAABB.setCenter(blockX, blockY, blockZ);
                    float xOverlap = Math.min(playerAABB.getMax().x, scratchBlockAABB.getMax().x)
                        - Math.max(playerAABB.getMin().x, scratchBlockAABB.getMin().x);
                    float zOverlap = Math.min(playerAABB.getMax().z, scratchBlockAABB.getMax().z)
                        - Math.max(playerAABB.getMin().z, scratchBlockAABB.getMin().z);

                    if (xOverlap > MIN_XZ_OVERLAP && zOverlap > MIN_XZ_OVERLAP) {
                        return true;  // Real ground found
                    }
                }
            }
        }
        
        return false;  // No ground (cliff edge)
    }
    
    /**
     * Attempts to make player jump if on ground.
     */
    public void tryJump(Player player) {
        if (player.isOnGround()) {
            player.getVelocity().y = Player.JUMP_VELOCITY;
            player.setOnGround(false);
        }
    }

    private int getMinBlockCenter(float min) {
        return (int) Math.floor(min + BLOCK_HALF_SIZE);
    }

    private int getMaxBlockCenter(float max) {
        return (int) Math.floor(Math.nextAfter(max + BLOCK_HALF_SIZE, Float.NEGATIVE_INFINITY));
    }
}
