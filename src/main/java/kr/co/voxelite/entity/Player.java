package kr.co.voxelite.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.physics.AABB;

/**
 * Player entity with position, velocity, and collision box.
 * Physics and movement handled by PhysicsSystem and CameraController.
 */
public class Player {
    private final Vector3 position;
    private final Vector3 velocity;
    private boolean onGround;
    private final AABB aabb;
    private boolean gravityEnabled = true;  // Extension point: gravity can be disabled by subclasses
    
    public static final float WIDTH = 0.6f;
    public static final float HEIGHT = 1.8f;
    public static final float EYE_HEIGHT = 1.62f;
    private static final float HALF_WIDTH = WIDTH / 2f;
    private static final float HALF_HEIGHT = HEIGHT / 2f;
    private static final float BLOCK_HALF_SIZE = 0.5f;
    
    public static final float GRAVITY = -20f;
    public static final float JUMP_VELOCITY = 7f;
    public static final float TERMINAL_VELOCITY = -50f;
    
    public Player(Vector3 startPosition) {
        this.position = new Vector3(startPosition);
        this.velocity = new Vector3(0, 0, 0);
        this.onGround = true;

        this.aabb = createCollisionAABB(startPosition);
    }
    
    /**
     * Updates position and syncs AABB.
     */
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        updateAABB();
    }
    
    public void setPosition(Vector3 newPosition) {
        position.set(newPosition);
        updateAABB();
    }
    
    private void updateAABB() {
        aabb.setCenter(position.x, position.y + HALF_HEIGHT, position.z);
    }
    
    /**
     * Returns eye position for camera placement.
     */
    public Vector3 getEyePosition() {
        return new Vector3(position.x, position.y + EYE_HEIGHT, position.z);
    }
    
    public Vector3 getPosition() { return position; }
    public Vector3 getVelocity() { return velocity; }
    public AABB getAABB() { return aabb; }
    public boolean isOnGround() { return onGround; }
    public boolean isGravityEnabled() { return gravityEnabled; }

    /**
     * Returns the canonical collision box for a player standing at the given feet position.
     */
    public static AABB createCollisionAABB(Vector3 playerPosition) {
        return new AABB(
            new Vector3(playerPosition.x, playerPosition.y + HALF_HEIGHT, playerPosition.z),
            HALF_WIDTH,
            HALF_HEIGHT,
            HALF_WIDTH
        );
    }

    /**
     * Checks whether the player's collision box intersects the given block cell.
     */
    public boolean collidesWithBlock(Vector3 blockPosition) {
        return blockPosition != null && aabb.intersects(createBlockAABB(blockPosition));
    }

    /**
     * Checks block intersection for an arbitrary player feet position without requiring an instance.
     */
    public static boolean collidesWithBlockAt(Vector3 playerPosition, Vector3 blockPosition) {
        return playerPosition != null
            && blockPosition != null
            && createCollisionAABB(playerPosition).intersects(createBlockAABB(blockPosition));
    }

    public void setOnGround(boolean onGround) { this.onGround = onGround; }
    
    /**
     * Extension point: allows subclasses to control gravity.
     * Used by game-specific features like fly mode, creative mode, etc.
     */
    protected void setGravityEnabled(boolean enabled) {
        this.gravityEnabled = enabled;
    }

    private static AABB createBlockAABB(Vector3 blockPosition) {
        return new AABB(new Vector3(
            MathUtils.floor(blockPosition.x),
            MathUtils.floor(blockPosition.y),
            MathUtils.floor(blockPosition.z)
        ), BLOCK_HALF_SIZE);
    }
}
