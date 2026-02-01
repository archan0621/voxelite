package kr.co.voxelite.physics;

import com.badlogic.gdx.math.Vector3;

/**
 * Result of a raycast, including the block hit and the face normal
 */
public class RaycastHit {
    private final Vector3 blockPosition;
    private final Vector3 normal;
    
    public RaycastHit(Vector3 blockPosition, Vector3 normal) {
        this.blockPosition = new Vector3(blockPosition);
        this.normal = new Vector3(normal);
    }
    
    public Vector3 getBlockPosition() {
        return blockPosition;
    }
    
    public Vector3 getNormal() {
        return normal;
    }
    
    /**
     * Calculate the position to place a new block adjacent to the hit face
     */
    public Vector3 getPlacementPosition() {
        return new Vector3(
            blockPosition.x + normal.x,
            blockPosition.y + normal.y,
            blockPosition.z + normal.z
        );
    }
}
