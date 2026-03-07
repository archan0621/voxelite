package kr.co.voxelite.physics;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import kr.co.voxelite.world.World;

/**
 * DDA (Digital Differential Analyzer) raycasting for block selection.
 * Steps one block at a time along the ray - max ~10 checks instead of millions.
 */
public class RayCaster {
    private static final float MAX_DISTANCE = 10f;

    /**
     * Casts a ray to find the nearest block (backward compatibility).
     */
    public static Vector3 raycast(Ray ray, World world) {
        RaycastHit hit = raycastWithFace(ray, world);
        return hit != null ? hit.getBlockPosition() : null;
    }

    /**
     * DDA raycast: step one block at a time, check only current voxel.
     * Max checks ≈ 5 ~ 10 blocks (not millions).
     */
    public static RaycastHit raycastWithFace(Ray ray, World world) {
        // Blocks are centered on integer coordinates, so shift into a grid where
        // each voxel occupies [n, n+1) before running standard DDA.
        Vector3 origin = new Vector3(ray.origin).add(0.5f, 0.5f, 0.5f);
        Vector3 dir = ray.direction.cpy().nor();

        int vx = (int) Math.floor(origin.x);
        int vy = (int) Math.floor(origin.y);
        int vz = (int) Math.floor(origin.z);

        int stepX = dir.x > 0 ? 1 : (dir.x < 0 ? -1 : 0);
        int stepY = dir.y > 0 ? 1 : (dir.y < 0 ? -1 : 0);
        int stepZ = dir.z > 0 ? 1 : (dir.z < 0 ? -1 : 0);

        float tDeltaX = (stepX != 0 && dir.x != 0) ? Math.abs(1f / dir.x) : Float.POSITIVE_INFINITY;
        float tDeltaY = (stepY != 0 && dir.y != 0) ? Math.abs(1f / dir.y) : Float.POSITIVE_INFINITY;
        float tDeltaZ = (stepZ != 0 && dir.z != 0) ? Math.abs(1f / dir.z) : Float.POSITIVE_INFINITY;

        float tMaxX = (stepX != 0) ? (stepX > 0 ? (vx + 1 - origin.x) / dir.x : (vx - origin.x) / dir.x) : Float.POSITIVE_INFINITY;
        float tMaxY = (stepY != 0) ? (stepY > 0 ? (vy + 1 - origin.y) / dir.y : (vy - origin.y) / dir.y) : Float.POSITIVE_INFINITY;
        float tMaxZ = (stepZ != 0) ? (stepZ > 0 ? (vz + 1 - origin.z) / dir.z : (vz - origin.z) / dir.z) : Float.POSITIVE_INFINITY;

        int prevVx = vx, prevVy = vy, prevVz = vz;
        float t = 0;

        while (t < MAX_DISTANCE) {
            if (world.hasBlockAt(vx, vy, vz)) {
                Vector3 normal = calculateHitNormal(prevVx, prevVy, prevVz, vx, vy, vz);
                return new RaycastHit(new Vector3(vx, vy, vz), normal);
            }

            prevVx = vx;
            prevVy = vy;
            prevVz = vz;

            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                t = tMaxX;
                tMaxX += tDeltaX;
                vx += stepX;
            } else if (tMaxY <= tMaxZ) {
                t = tMaxY;
                tMaxY += tDeltaY;
                vy += stepY;
            } else {
                t = tMaxZ;
                tMaxZ += tDeltaZ;
                vz += stepZ;
            }
        }

        return null;
    }

    /**
     * Hit normal = direction from hit voxel toward ray origin (face normal pointing outward).
     */
    private static Vector3 calculateHitNormal(int prevVx, int prevVy, int prevVz, int vx, int vy, int vz) {
        int dx = prevVx - vx;
        int dy = prevVy - vy;
        int dz = prevVz - vz;

        if (dx != 0) return new Vector3(dx > 0 ? 1 : -1, 0, 0);
        if (dy != 0) return new Vector3(0, dy > 0 ? 1 : -1, 0);
        if (dz != 0) return new Vector3(0, 0, dz > 0 ? 1 : -1);

        return new Vector3(0, 1, 0);
    }
}
