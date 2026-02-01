package kr.co.voxelite.physics;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import kr.co.voxelite.world.World;

import java.util.List;

/**
 * Raycasting for block selection with face detection.
 */
public class RayCaster {
    private static final float MAX_DISTANCE = 10f;
    private static final float STEP = 0.05f;

    /**
     * Casts a ray to find the nearest block (backward compatibility).
     * @param ray ray from camera
     * @param world world containing blocks
     * @return selected block position, or null if none found
     */
    public static Vector3 raycast(Ray ray, World world) {
        RaycastHit hit = raycastWithFace(ray, world);
        return hit != null ? hit.getBlockPosition() : null;
    }
    
    /**
     * Casts a ray to find the nearest block with face information.
     * @param ray ray from camera
     * @param world world containing blocks
     * @return RaycastHit with block position and face normal, or null if none found
     */
    public static RaycastHit raycastWithFace(Ray ray, World world) {
        Vector3 current = new Vector3(ray.origin);
        Vector3 previous = new Vector3(ray.origin);
        Vector3 step = new Vector3(ray.direction).nor().scl(STEP);
        
        List<Vector3> blockPositions = world.getBlockPositions();
        
        for (float distance = 0; distance < MAX_DISTANCE; distance += STEP) {
            previous.set(current);
            current.add(step);
            
            for (Vector3 blockPos : blockPositions) {
                if (isPointInBlock(current, blockPos)) {
                    // Determine which face was hit
                    Vector3 normal = calculateHitNormal(previous, blockPos);
                    return new RaycastHit(blockPos, normal);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Checks if point is inside a 1x1x1 block.
     */
    private static boolean isPointInBlock(Vector3 point, Vector3 blockPos) {
        float halfSize = 0.5f;
        
        return point.x >= blockPos.x - halfSize && point.x <= blockPos.x + halfSize &&
               point.y >= blockPos.y - halfSize && point.y <= blockPos.y + halfSize &&
               point.z >= blockPos.z - halfSize && point.z <= blockPos.z + halfSize;
    }
    
    /**
     * Calculates which face was hit based on the entry point.
     * The entry point is just outside the block, so we calculate the direction
     * from block center to entry point and determine the closest face.
     */
    private static Vector3 calculateHitNormal(Vector3 entryPoint, Vector3 blockPos) {
        // Vector from block center to entry point
        float dx = entryPoint.x - blockPos.x;
        float dy = entryPoint.y - blockPos.y;
        float dz = entryPoint.z - blockPos.z;
        
        // Find the axis with maximum absolute value
        float absDx = Math.abs(dx);
        float absDy = Math.abs(dy);
        float absDz = Math.abs(dz);
        
        if (absDx > absDy && absDx > absDz) {
            // Hit X face
            return new Vector3(dx > 0 ? 1 : -1, 0, 0);
        } else if (absDy > absDz) {
            // Hit Y face
            return new Vector3(0, dy > 0 ? 1 : -1, 0);
        } else {
            // Hit Z face
            return new Vector3(0, 0, dz > 0 ? 1 : -1);
        }
    }
}

