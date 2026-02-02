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
     * Y축 방향 충돌 검사 - X/Z/Y 모두 겹쳐야 하며, X/Z는 충분히 겹쳐야 함
     * 
     * Y축 충돌의 정의:
     * - Y축으로 이동할 때 발생하는 충돌
     * - X/Z 투영 영역이 충분히 겹친 상태에서 Y축 침투가 발생한 경우만
     * - 벽이나 모서리 블록은 Y축 충돌이 아님
     */
    public boolean intersectsOnY(AABB other) {
        float MIN_OVERLAP = 0.01f;  // 최소 겹침 임계값

        // 1. Y축 겹침 확인 (먼저 체크하여 조기 탈출)
        boolean yOverlap = this.max.y > other.min.y && this.min.y < other.max.y;
        if (!yOverlap) {
            return false;
        }

        // 2. X/Z축이 충분히 겹쳐야 함 (실제 겹침 영역이 임계값 이상)
        float xOverlap = Math.min(this.max.x, other.max.x) - Math.max(this.min.x, other.min.x);
        float zOverlap = Math.min(this.max.z, other.max.z) - Math.max(this.min.z, other.min.z);

        // X 또는 Z가 떨어져 있거나(음수) 경계만 터치하면 Y축 충돌 아님
        return xOverlap > MIN_OVERLAP && zOverlap > MIN_OVERLAP;
    }

    /**
     * X축 방향 충돌 검사 - Y/Z/X 모두 겹쳐야 하며, Y/Z는 충분히 겹쳐야 함
     */
    public boolean intersectsOnX(AABB other) {
        float MIN_OVERLAP = 0.01f;

        // 1. X축 겹침 확인 (먼저 체크하여 조기 탈출)
        boolean xOverlap = this.max.x > other.min.x && this.min.x < other.max.x;
        if (!xOverlap) {
            return false;
        }

        // 2. Y/Z축이 충분히 겹쳐야 함
        float yOverlap = Math.min(this.max.y, other.max.y) - Math.max(this.min.y, other.min.y);
        float zOverlap = Math.min(this.max.z, other.max.z) - Math.max(this.min.z, other.min.z);

        return yOverlap > MIN_OVERLAP && zOverlap > MIN_OVERLAP;
    }

    /**
     * Z축 방향 충돌 검사 - X/Y/Z 모두 겹쳐야 하며, X/Y는 충분히 겹쳐야 함
     */
    public boolean intersectsOnZ(AABB other) {
        float MIN_OVERLAP = 0.01f;

        // 1. Z축 겹침 확인 (먼저 체크하여 조기 탈출)
        boolean zOverlap = this.max.z > other.min.z && this.min.z < other.max.z;
        if (!zOverlap) {
            return false;
        }

        // 2. X/Y축이 충분히 겹쳐야 함
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
