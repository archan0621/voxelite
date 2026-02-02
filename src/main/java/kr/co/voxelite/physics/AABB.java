package kr.co.voxelite.physics;

import com.badlogic.gdx.math.Vector3;

/**
 * Axis-Aligned Bounding Box for collision detection.
 * Mutable structure to minimize GC overhead.
 */
public class AABB {
    private final Vector3 center;
    private float halfWidth;
    private float halfHeight;
    private float halfDepth;
    private final Vector3 min;
    private final Vector3 max;
    
    /**
     * Creates a box-shaped AABB.
     */
    public AABB(Vector3 center, float halfWidth, float halfHeight, float halfDepth) {
        this.center = new Vector3(center);
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.halfDepth = halfDepth;
        this.min = new Vector3();
        this.max = new Vector3();
        updateBounds();
    }
    
    /**
     * Creates a cube-shaped AABB.
     */
    public AABB(Vector3 center, float halfSize) {
        this(center, halfSize, halfSize, halfSize);
    }
    
    /**
     * Recalculates min/max bounds from center and half sizes.
     */
    private void updateBounds() {
        min.set(center.x - halfWidth, center.y - halfHeight, center.z - halfDepth);
        max.set(center.x + halfWidth, center.y + halfHeight, center.z + halfDepth);
    }
    
    public void setCenter(Vector3 newCenter) {
        center.set(newCenter);
        updateBounds();
    }
    
    public void setCenter(float x, float y, float z) {
        center.set(x, y, z);
        updateBounds();
    }
    
    public void setSize(float halfWidth, float halfHeight, float halfDepth) {
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.halfDepth = halfDepth;
        updateBounds();
    }
    
    /**
     * Checks intersection with another AABB.
     */
    public boolean intersects(AABB other) {
        return this.max.x > other.min.x && this.min.x < other.max.x &&
               this.max.y > other.min.y && this.min.y < other.max.y &&
               this.max.z > other.min.z && this.min.z < other.max.z;
    }

    /**
     * Y-axis direction collision detection - All X/Z/Y must overlap, and X/Z must overlap sufficiently
     * 
     * Definition of Y-axis collision:
     * - Collision that occurs when moving along Y-axis
     * - Only when Y-axis penetration occurs with sufficient X/Z projection area overlap
     * - Wall or corner blocks are not Y-axis collisions
     */
    public boolean intersectsOnY(AABB other) {
        float MIN_OVERLAP = 0.01f;  // Minimum overlap threshold

        // 1. Check Y-axis overlap (check first for early exit)
        boolean yOverlap = this.max.y > other.min.y && this.min.y < other.max.y;
        if (!yOverlap) {
            return false;
        }

        // 2. X/Z axes must overlap sufficiently (actual overlap area must be above threshold)
        float xOverlap = Math.min(this.max.x, other.max.x) - Math.max(this.min.x, other.min.x);
        float zOverlap = Math.min(this.max.z, other.max.z) - Math.max(this.min.z, other.min.z);

        // Not a Y-axis collision if X or Z are separated (negative) or only touch at boundary
        return xOverlap > MIN_OVERLAP && zOverlap > MIN_OVERLAP;
    }

    /**
     * X-axis direction collision detection - All Y/Z/X must overlap, and Y/Z must overlap sufficiently
     */
    public boolean intersectsOnX(AABB other) {
        float MIN_OVERLAP = 0.01f;

        // 1. Check X-axis overlap (check first for early exit)
        boolean xOverlap = this.max.x > other.min.x && this.min.x < other.max.x;
        if (!xOverlap) {
            return false;
        }

        // 2. Y/Z axes must overlap sufficiently
        float yOverlap = Math.min(this.max.y, other.max.y) - Math.max(this.min.y, other.min.y);
        float zOverlap = Math.min(this.max.z, other.max.z) - Math.max(this.min.z, other.min.z);

        return yOverlap > MIN_OVERLAP && zOverlap > MIN_OVERLAP;
    }

    /**
     * Z-axis direction collision detection - All X/Y/Z must overlap, and X/Y must overlap sufficiently
     */
    public boolean intersectsOnZ(AABB other) {
        float MIN_OVERLAP = 0.01f;

        // 1. Check Z-axis overlap (check first for early exit)
        boolean zOverlap = this.max.z > other.min.z && this.min.z < other.max.z;
        if (!zOverlap) {
            return false;
        }

        // 2. X/Y axes must overlap sufficiently
        float xOverlap = Math.min(this.max.x, other.max.x) - Math.max(this.min.x, other.min.x);
        float yOverlap = Math.min(this.max.y, other.max.y) - Math.max(this.min.y, other.min.y);

        return xOverlap > MIN_OVERLAP && yOverlap > MIN_OVERLAP;
    }
    
    public void offset(float dx, float dy, float dz) {
        center.add(dx, dy, dz);
        updateBounds();
    }
    
    // Getters
    public Vector3 getCenter() { return center; }
    public Vector3 getMin() { return min; }
    public Vector3 getMax() { return max; }
    public float getHalfWidth() { return halfWidth; }
    public float getHalfHeight() { return halfHeight; }
    public float getHalfDepth() { return halfDepth; }
}
