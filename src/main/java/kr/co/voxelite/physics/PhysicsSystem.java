package kr.co.voxelite.physics;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.entity.Player;
import kr.co.voxelite.world.World;

/**
 * Minecraft-style axis-separated collision resolution.
 * Applies gravity, resolves collisions per-axis (Y → X → Z), and determines ground state.
 */
public class PhysicsSystem {
    private final World world;
    private static final float BLOCK_SIZE = 1.0f;
    
    public PhysicsSystem(World world) {
        this.world = world;
    }
    
    /**
     * Updates player physics (gravity, movement, collision).
     */
    public void update(Player player, float delta) {
        applyGravity(player, delta);
        
        float dx = player.getVelocity().x * delta;
        float dy = player.getVelocity().y * delta;
        float dz = player.getVelocity().z * delta;
        
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
     */
    private void moveAndCollide(Player player, float dx, float dy, float dz) {
        Vector3 pos = player.getPosition();
        AABB aabb = player.getAABB();
        
        if (dy != 0) {
            pos.y += dy;
            player.setPosition(pos);
            
            if (checkCollision(aabb)) {
                if (dy > 0) {
                    float ceilingY = findCeilingY(aabb);
                    pos.y = ceilingY - Player.HEIGHT;
                    player.setPosition(pos);
                    player.getVelocity().y = 0;
                } else {
                    float floorY = findFloorY(aabb);
                    pos.y = floorY;
                    player.setPosition(pos);
                    player.getVelocity().y = 0;
                    player.setOnGround(true);
                }
            } else if (dy < 0) {
                player.setOnGround(false);
            }
        }
        
        if (dx != 0) {
            pos.x += dx;
            player.setPosition(pos);
            
            if (checkCollision(aabb)) {
                if (dx > 0) {
                    float wallX = findWallXPositive(aabb);
                    pos.x = wallX - Player.WIDTH / 2f;
                } else {
                    float wallX = findWallXNegative(aabb);
                    pos.x = wallX + Player.WIDTH / 2f;
                }
                player.setPosition(pos);
                player.getVelocity().x = 0;
            }
        }
        
        if (dz != 0) {
            pos.z += dz;
            player.setPosition(pos);
            
            if (checkCollision(aabb)) {
                if (dz > 0) {
                    float wallZ = findWallZPositive(aabb);
                    pos.z = wallZ - Player.WIDTH / 2f;
                } else {
                    float wallZ = findWallZNegative(aabb);
                    pos.z = wallZ + Player.WIDTH / 2f;
                }
                player.setPosition(pos);
                player.getVelocity().z = 0;
            }
        }
        
        // Always check ground (not just when moving horizontally)
        checkGroundBelow(player);
    }
    
    /**
     * Checks if player AABB intersects any block.
     */
    private boolean checkCollision(AABB playerAABB) {
        for (Vector3 blockPos : world.getBlockPositions()) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersects(blockAABB)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Finds highest floor Y among colliding blocks.
     */
    private float findFloorY(AABB playerAABB) {
        float highestFloor = -Float.MAX_VALUE;
        
        for (Vector3 blockPos : world.getBlockPositions()) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersects(blockAABB)) {
                float blockTop = blockPos.y + BLOCK_SIZE / 2f;
                if (blockTop > highestFloor) {
                    highestFloor = blockTop;
                }
            }
        }
        
        return highestFloor;
    }
    
    /**
     * Finds lowest ceiling Y among colliding blocks.
     */
    private float findCeilingY(AABB playerAABB) {
        float lowestCeiling = Float.MAX_VALUE;
        
        for (Vector3 blockPos : world.getBlockPositions()) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersects(blockAABB)) {
                float blockBottom = blockPos.y - BLOCK_SIZE / 2f;
                if (blockBottom < lowestCeiling) {
                    lowestCeiling = blockBottom;
                }
            }
        }
        
        return lowestCeiling;
    }
    
    /**
     * Finds leftmost wall X among colliding blocks (positive movement).
     */
    private float findWallXPositive(AABB playerAABB) {
        float leftmostWall = Float.MAX_VALUE;
        
        for (Vector3 blockPos : world.getBlockPositions()) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersects(blockAABB)) {
                float blockLeft = blockPos.x - BLOCK_SIZE / 2f;
                if (blockLeft < leftmostWall) {
                    leftmostWall = blockLeft;
                }
            }
        }
        
        return leftmostWall;
    }
    
    /**
     * Finds rightmost wall X among colliding blocks (negative movement).
     */
    private float findWallXNegative(AABB playerAABB) {
        float rightmostWall = -Float.MAX_VALUE;
        
        for (Vector3 blockPos : world.getBlockPositions()) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersects(blockAABB)) {
                float blockRight = blockPos.x + BLOCK_SIZE / 2f;
                if (blockRight > rightmostWall) {
                    rightmostWall = blockRight;
                }
            }
        }
        
        return rightmostWall;
    }
    
    /**
     * Finds backmost wall Z among colliding blocks (positive movement).
     */
    private float findWallZPositive(AABB playerAABB) {
        float backmostWall = Float.MAX_VALUE;
        
        for (Vector3 blockPos : world.getBlockPositions()) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersects(blockAABB)) {
                float blockBack = blockPos.z - BLOCK_SIZE / 2f;
                if (blockBack < backmostWall) {
                    backmostWall = blockBack;
                }
            }
        }
        
        return backmostWall;
    }
    
    /**
     * Finds frontmost wall Z among colliding blocks (negative movement).
     */
    private float findWallZNegative(AABB playerAABB) {
        float frontmostWall = -Float.MAX_VALUE;
        
        for (Vector3 blockPos : world.getBlockPositions()) {
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            if (playerAABB.intersects(blockAABB)) {
                float blockFront = blockPos.z + BLOCK_SIZE / 2f;
                if (blockFront > frontmostWall) {
                    frontmostWall = blockFront;
                }
            }
        }
        
        return frontmostWall;
    }
    
    /**
     * Checks for ground directly below player to prevent floating at cliff edges.
     */
    private void checkGroundBelow(Player player) {
        Vector3 pos = player.getPosition();
        Vector3 belowPos = new Vector3(pos.x, pos.y - 0.01f, pos.z);
        AABB belowAABB = new AABB(belowPos, Player.WIDTH / 2f, 0.01f, Player.WIDTH / 2f);
        
        boolean hasGroundBelow = checkCollision(belowAABB);
        player.setOnGround(hasGroundBelow);
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
