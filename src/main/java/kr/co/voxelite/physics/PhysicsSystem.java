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
    private static final float COLLISION_MARGIN = 0.001f; // 충돌 여유값
    private static final int PHYSICS_CHUNK_RADIUS = 1; // 물리 계산 청크 반경 (3x3)
    
    // Fixed Timestep
    private static final float FIXED_TIMESTEP = 1f / 60f; // 60Hz 물리 업데이트
    private static final float MAX_FRAME_TIME = 0.25f; // 최대 프레임 시간 (스파이럴 방지)
    private float accumulator = 0f;
    
    // 근처 블록 캐시 (청크 이동 시에만 갱신)
    private List<Vector3> nearbyBlocks = new ArrayList<>();
    private ChunkCoord lastPhysicsChunk = null;
    private boolean cacheInvalidated = false; // 블록 변경으로 인한 캐시 무효화 플래그
    
    public PhysicsSystem(World world) {
        this.world = world;
    }
    
    /**
     * 블록 변경 시 캐시 무효화 (다음 물리 스텝에서 갱신됨)
     */
    public void invalidateCache() {
        this.cacheInvalidated = true;
    }
    
    /**
     * Updates player physics with Fixed Timestep
     */
    public void update(Player player, float delta) {
        // 스파이럴 오브 데스 방지
        if (delta > MAX_FRAME_TIME) {
            delta = MAX_FRAME_TIME;
        }
        
        accumulator += delta;
        
        // Fixed timestep으로 물리 시뮬레이션
        while (accumulator >= FIXED_TIMESTEP) {
            stepPhysics(player, FIXED_TIMESTEP);
            accumulator -= FIXED_TIMESTEP;
        }
        
        // 남은 시간은 다음 프레임으로 이월
    }
    
    /**
     * Single physics step with fixed timestep
     */
    private void stepPhysics(Player player, float dt) {
        // 근처 블록 갱신 (청크 이동 또는 캐시 무효화 시)
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
                    pos.x = wallX - Player.WIDTH / 2f - COLLISION_MARGIN;
                } else {
                    float wallX = findWallXNegative(aabb);
                    pos.x = wallX + Player.WIDTH / 2f + COLLISION_MARGIN;
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
                    pos.z = wallZ - Player.WIDTH / 2f - COLLISION_MARGIN;
                } else {
                    float wallZ = findWallZNegative(aabb);
                    pos.z = wallZ + Player.WIDTH / 2f + COLLISION_MARGIN;
                }
                player.setPosition(pos);
                player.getVelocity().z = 0;
            }
        }
        
        // Always check ground (not just when moving horizontally)
        checkGroundBelow(player);
    }
    
    /**
     * Checks if player AABB intersects any block (근처 청크만)
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
     * Finds highest floor Y among colliding blocks.
     */
    private float findFloorY(AABB playerAABB) {
        float highestFloor = -Float.MAX_VALUE;
        
        for (Vector3 blockPos : nearbyBlocks) {
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
        
        for (Vector3 blockPos : nearbyBlocks) {
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
        
        for (Vector3 blockPos : nearbyBlocks) {
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
        
        for (Vector3 blockPos : nearbyBlocks) {
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
        
        for (Vector3 blockPos : nearbyBlocks) {
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
        
        for (Vector3 blockPos : nearbyBlocks) {
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
