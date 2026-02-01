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
