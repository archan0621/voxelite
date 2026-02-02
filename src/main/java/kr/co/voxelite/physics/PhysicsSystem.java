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
     *
     * 핵심 원칙:
     * 1. 각 축 이동 후 반드시 position → AABB 동기화
     * 2. 각 축은 독립적으로 처리
     * 3. onGround는 Y축 처리에서만 관리 (X/Z 이동 후 재확인 금지)
     */
    private void moveAndCollide(Player player, float dx, float dy, float dz) {
        // 현재 위치의 복사본 사용 (내부 참조 직접 수정 방지)
        Vector3 currentPos = new Vector3(player.getPosition());

        // === Y축 이동 및 충돌 해결 ===
        if (dy != 0) {
            currentPos.y += dy;
            player.setPosition(currentPos);  // AABB 동기화됨

            AABB aabb = player.getAABB();    // 동기화된 AABB 가져오기
            if (checkCollisionY(aabb)) {     // Y축 충돌만 검사
                if (dy > 0) {
                    // 천장 충돌
                    float ceilingY = findCeilingY(aabb);
                    currentPos.y = ceilingY - Player.HEIGHT;
                    player.setOnGround(false);  // 천장 충돌 시 명시적으로 공중 상태
                } else {
                    // 바닥 충돌 (착지)
                    float floorY = findFloorY(aabb);
                    currentPos.y = floorY;
                    player.setOnGround(true);   // Y축 하강 충돌만 착지로 인정
                }
                player.setPosition(currentPos);  // 보정된 위치로 AABB 재동기화
                player.getVelocity().y = 0;
            } else if (dy < 0) {
                // Y축 하강 중 충돌 없음 → 공중 상태
                player.setOnGround(false);
            }
            // dy > 0이고 충돌 없으면 onGround 변경 없음 (상승 중)
        }
        
        // === 절벽 가장자리 감지 (Y축 처리 직후, X/Z 이동 전) ===
        // 지상 상태에서만 체크: 절벽을 걸어서 벗어날 때 감지
        if (player.isOnGround() && dy == 0) {
            // 현재 위치에서 바닥이 있는지 확인
            if (!hasGroundDirectlyBelow(player)) {
                player.setOnGround(false);
            }
        }

        // === X축 이동 및 충돌 해결 ===
        if (dx != 0) {
            currentPos.x += dx;
            player.setPosition(currentPos);  // AABB 동기화됨

            AABB aabb = player.getAABB();    // 동기화된 AABB 가져오기
            if (checkCollisionX(aabb)) {     // X축 충돌만 검사
                if (dx > 0) {
                    float wallX = findWallXPositive(aabb);
                    currentPos.x = wallX - Player.WIDTH / 2f - COLLISION_MARGIN;
                } else {
                    float wallX = findWallXNegative(aabb);
                    currentPos.x = wallX + Player.WIDTH / 2f + COLLISION_MARGIN;
                }
                player.setPosition(currentPos);  // 보정된 위치로 AABB 재동기화
                player.getVelocity().x = 0;
            }
        }

        // === Z축 이동 및 충돌 해결 ===
        if (dz != 0) {
            currentPos.z += dz;
            player.setPosition(currentPos);  // AABB 동기화됨

            AABB aabb = player.getAABB();    // 동기화된 AABB 가져오기
            if (checkCollisionZ(aabb)) {     // Z축 충돌만 검사
                if (dz > 0) {
                    float wallZ = findWallZPositive(aabb);
                    currentPos.z = wallZ - Player.WIDTH / 2f - COLLISION_MARGIN;
                } else {
                    float wallZ = findWallZNegative(aabb);
                    currentPos.z = wallZ + Player.WIDTH / 2f + COLLISION_MARGIN;
                }
                player.setPosition(currentPos);  // 보정된 위치로 AABB 재동기화
                player.getVelocity().z = 0;
            }
        }

        // X/Z 이동 후 바닥 재확인 금지: 상태 흔들림 유발
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
     * Y축 방향 충돌만 검사 (X/Z 겹침은 무시)
     * 플레이어 AABB의 X/Z 범위 내에서 Y축으로 실제 침투가 있는 블록만 검사
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
     * X축 방향 충돌만 검사 (Y/Z 겹침은 무시)
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
     * Z축 방향 충돌만 검사 (X/Y 겹침은 무시)
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
     * Y축 충돌 블록 중 가장 높은 floor Y 반환 (착지 시 사용)
     * 축별 충돌 검사 사용 - X/Z 경계 겹침 블록은 제외
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
     * Y축 충돌 블록 중 가장 낮은 ceiling Y 반환 (점프 천장 충돌 시 사용)
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
     * X축 충돌 블록 중 가장 왼쪽(min X) 벽 반환 (오른쪽 이동 시 사용)
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
     * X축 충돌 블록 중 가장 오른쪽(max X) 벽 반환 (왼쪽 이동 시 사용)
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
     * Z축 충돌 블록 중 가장 뒤쪽(min Z) 벽 반환 (앞쪽 이동 시 사용)
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
     * Z축 충돌 블록 중 가장 앞쪽(max Z) 벽 반환 (뒤쪽 이동 시 사용)
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
     * 플레이어 발 바로 아래에 바닥이 있는지 확인 (절벽 가장자리 감지용)
     * 
     * 호출 시점: Y축 처리 직후, X/Z 이동 전
     * 목적: 절벽을 걸어서 벗어날 때 감지
     * 
     * 중요: X/Z 이동 후 호출 금지 (블록 경계에서 오판 유발)
     */
    private boolean hasGroundDirectlyBelow(Player player) {
        Vector3 pos = player.getPosition();
        float playerBottom = pos.y;
        float GROUND_THRESHOLD = 0.02f;  // 바닥으로 인정할 최대 Y 간격
        float MIN_XZ_OVERLAP = 0.1f;     // 최소 X/Z 겹침 (모서리 제외)
        
        AABB playerAABB = player.getAABB();
        
        for (Vector3 blockPos : nearbyBlocks) {
            float blockTop = blockPos.y + BLOCK_SIZE / 2f;
            
            // 1. Y축 간격 체크: 블록 상단이 플레이어 발 바로 아래에 있는지
            float yGap = playerBottom - blockTop;
            if (yGap < 0 || yGap > GROUND_THRESHOLD) {
                continue;  // 너무 멀거나 플레이어가 블록 안에 있음
            }
            
            // 2. X/Z 투영 영역 겹침 체크: 충분히 겹쳐야 바닥으로 인정
            AABB blockAABB = new AABB(blockPos, BLOCK_SIZE / 2f);
            float xOverlap = Math.min(playerAABB.getMax().x, blockAABB.getMax().x) 
                           - Math.max(playerAABB.getMin().x, blockAABB.getMin().x);
            float zOverlap = Math.min(playerAABB.getMax().z, blockAABB.getMax().z) 
                           - Math.max(playerAABB.getMin().z, blockAABB.getMin().z);
            
            if (xOverlap > MIN_XZ_OVERLAP && zOverlap > MIN_XZ_OVERLAP) {
                return true;  // 진짜 바닥 발견
            }
        }
        
        return false;  // 바닥 없음 (절벽 가장자리)
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
