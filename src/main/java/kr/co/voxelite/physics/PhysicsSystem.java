package kr.co.voxelite.physics;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.entity.Player;
import kr.co.voxelite.world.ChunkCoord;
import kr.co.voxelite.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft-style axis-separated collision resolution.
 * Applies gravity, resolves collisions per-axis (Y → X → Z), and determines ground state.
 * Uses Fixed Timestep for frame-independent physics simulation.
 */
public class PhysicsSystem {
    private final World world;
    private static final float BLOCK_SIZE = 1.0f;
    private static final float COLLISION_MARGIN = 0.001f; // Collision margin
    private static final int PHYSICS_CHUNK_RADIUS = 1; // Physics calculation chunk radius (3x3)
    
    // Fixed Timestep
    private static final float FIXED_TIMESTEP = 1f / 60f; // 60Hz physics update
    private static final float MAX_FRAME_TIME = 0.25f; // Maximum frame time (prevents spiral of death)
    private float accumulator = 0f;
    
    // Nearby blocks cache (updated only when chunk changes)
    private List<Vector3> nearbyBlocks = new ArrayList<>();
    private ChunkCoord lastPhysicsChunk = null;
    private boolean cacheInvalidated = false; // Cache invalidation flag due to block changes
    
    public PhysicsSystem(World world) {
        this.world = world;
    }
    
    /**
     * Invalidates cache when blocks change (will be updated in next physics step)
     */
    public void invalidateCache() {
        this.cacheInvalidated = true;
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
        // Update nearby blocks (on chunk movement or cache invalidation)
        Vector3 pos = player.getPosition();
        ChunkCoord currentChunk = world.getChunkCoordAt(pos.x, pos.z);
        
        if (lastPhysicsChunk == null || !lastPhysicsChunk.equals(currentChunk) || cacheInvalidated) {
            nearbyBlocks = world.getNearbyBlockPositions(pos.x, pos.z, PHYSICS_CHUNK_RADIUS);
            lastPhysicsChunk = currentChunk;
            cacheInvalidated = false;
        }
        
        applyGravity(player, dt);
        
        float dx = player.getVelocity().x * dt;
        float dy = player.getVelocity().y * dt;
        float dz = player.getVelocity().z * dt;
        
        moveAndCollide(player, dx, dy, dz);
    }
    
    /**
     * Applies gravity and clamps to terminal velocity.
     */
    private void applyGravity(Player player, float delta) {
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
        for (Vector3 blockPos : nearbyBlocks) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersects(blockAABB)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks only Y-axis direction collision (ignores X/Z overlap)
     * Only checks blocks that have actual Y-axis penetration within player AABB's X/Z range
     */
    private boolean checkCollisionY(AABB playerAABB) {
        for (Vector3 blockPos : nearbyBlocks) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersectsOnY(blockAABB)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks only X-axis direction collision (ignores Y/Z overlap)
     */
    private boolean checkCollisionX(AABB playerAABB) {
        for (Vector3 blockPos : nearbyBlocks) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersectsOnX(blockAABB)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks only Z-axis direction collision (ignores X/Y overlap)
     */
    private boolean checkCollisionZ(AABB playerAABB) {
        for (Vector3 blockPos : nearbyBlocks) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersectsOnZ(blockAABB)) {
                return true;
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

        for (Vector3 blockPos : nearbyBlocks) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersectsOnY(blockAABB)) {
                float blockTop = blockPos.y + BLOCK_SIZE / 2f;
                if (blockTop > highestFloor) {
                    highestFloor = blockTop;
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

        for (Vector3 blockPos : nearbyBlocks) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersectsOnY(blockAABB)) {
                float blockBottom = blockPos.y - BLOCK_SIZE / 2f;
                if (blockBottom < lowestCeiling) {
                    lowestCeiling = blockBottom;
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

        for (Vector3 blockPos : nearbyBlocks) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersectsOnX(blockAABB)) {
                float blockLeft = blockPos.x - BLOCK_SIZE / 2f;
                if (blockLeft < leftmostWall) {
                    leftmostWall = blockLeft;
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

        for (Vector3 blockPos : nearbyBlocks) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersectsOnX(blockAABB)) {
                float blockRight = blockPos.x + BLOCK_SIZE / 2f;
                if (blockRight > rightmostWall) {
                    rightmostWall = blockRight;
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

        for (Vector3 blockPos : nearbyBlocks) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersectsOnZ(blockAABB)) {
                float blockBack = blockPos.z - BLOCK_SIZE / 2f;
                if (blockBack < backmostWall) {
                    backmostWall = blockBack;
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

        for (Vector3 blockPos : nearbyBlocks) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersectsOnZ(blockAABB)) {
                float blockFront = blockPos.z + BLOCK_SIZE / 2f;
                if (blockFront > frontmostWall) {
                    frontmostWall = blockFront;
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
        float GROUND_THRESHOLD = 0.02f;  // Maximum Y gap to consider as ground
        float MIN_XZ_OVERLAP = 0.1f;     // Minimum X/Z overlap (excludes corners)
        
        AABB playerAABB = player.getAABB();
        
        for (Vector3 blockPos : nearbyBlocks) {
            float blockTop = blockPos.y + BLOCK_SIZE / 2f;
            
            // 1. Y-axis gap check: Is block top directly below player's feet?
            float yGap = playerBottom - blockTop;
            if (yGap < 0 || yGap > GROUND_THRESHOLD) {
                continue;  // Too far or player is inside block
            }
            
            // 2. X/Z projection area overlap check: Must overlap sufficiently to be considered ground
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            float xOverlap = Math.min(playerAABB.getMax().x, blockAABB.getMax().x) 
                           - Math.max(playerAABB.getMin().x, blockAABB.getMin().x);
            float zOverlap = Math.min(playerAABB.getMax().z, blockAABB.getMax().z) 
                           - Math.max(playerAABB.getMin().z, blockAABB.getMin().z);
            
            if (xOverlap > MIN_XZ_OVERLAP && zOverlap > MIN_XZ_OVERLAP) {
                return true;  // Real ground found
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
}
