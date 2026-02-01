package kr.co.voxelite.physics;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.world.World;

/**
 * Collision detection between player AABB and 1x1x1 atomic blocks.
 */
public class CollisionDetector {
    private static final float BLOCK_HALF_SIZE = 0.5f;

    private World world;

    public CollisionDetector(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }
    
    /**
     * Checks if player AABB collides with any block.
     * @param playerFootPosition player's foot position
     * @param playerWidth player width (0.6)
     * @param playerHeight player height (1.8)
     * @return true if collision detected
     */
    public boolean checkPlayerCollision(Vector3 playerFootPosition, float playerWidth, float playerHeight) {
        float playerHalfWidth = playerWidth / 2f;
        
        float playerMinX = playerFootPosition.x - playerHalfWidth;
        float playerMaxX = playerFootPosition.x + playerHalfWidth;
        float playerMinY = playerFootPosition.y;
        float playerMaxY = playerFootPosition.y + playerHeight;
        float playerMinZ = playerFootPosition.z - playerHalfWidth;
        float playerMaxZ = playerFootPosition.z + playerHalfWidth;
        
        for (Vector3 blockPos : world.getBlockPositions()) {
            float blockMinX = blockPos.x - BLOCK_HALF_SIZE;
            float blockMaxX = blockPos.x + BLOCK_HALF_SIZE;
            float blockMinY = blockPos.y - BLOCK_HALF_SIZE;
            float blockMaxY = blockPos.y + BLOCK_HALF_SIZE;
            float blockMinZ = blockPos.z - BLOCK_HALF_SIZE;
            float blockMaxZ = blockPos.z + BLOCK_HALF_SIZE;
            
            boolean xOverlap = playerMaxX > blockMinX && playerMinX < blockMaxX;
            boolean yOverlap = playerMaxY > blockMinY && playerMinY < blockMaxY;
            boolean zOverlap = playerMaxZ > blockMinZ && playerMinZ < blockMaxZ;
            
            if (xOverlap && yOverlap && zOverlap) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * @deprecated Use checkPlayerCollision instead
     */
    @Deprecated
    public boolean checkCollision(Vector3 position) {
        return checkPlayerCollision(position, 0.01f, 0.01f);
    }
}
