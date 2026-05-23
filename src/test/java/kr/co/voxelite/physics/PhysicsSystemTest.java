package kr.co.voxelite.physics;

import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.entity.Player;
import kr.co.voxelite.world.BlockManager;
import kr.co.voxelite.world.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhysicsSystemTest {
    private PhysicsSystem physics;
    private World world;
    private Player player;

    @BeforeEach
    void setUp() {
        world = new World(new BlockManager());
        physics = new PhysicsSystem(world);
        player = new Player(new Vector3(0, 2, 0));
    }

    @Test
    void update_PlayerInAir_ShouldApplyGravity() {
        player.setOnGround(false);
        float initialY = player.getPosition().y;

        physics.update(player, 0.2f);

        assertTrue(player.getPosition().y < initialY);
        assertTrue(player.getVelocity().y < 0);
    }

    @Test
    void update_PlayerFallsOntoBlock_ShouldLand() {
        world.addBlock(new Vector3(0, 0, 0), 0);
        player.setPosition(0, 2, 0);
        player.setOnGround(false);
        player.getVelocity().y = -5f;

        for (int i = 0; i < 20; i++) {
            physics.update(player, 0.1f);
        }

        assertTrue(player.isOnGround());
        assertEquals(0f, player.getVelocity().y, 0.1f);
    }

    @Test
    void tryJump_OnGround_ShouldSetJumpVelocity() {
        world.addBlock(new Vector3(0, 0, 0), 0);
        player.setPosition(0, 1, 0);
        player.setOnGround(true);

        physics.tryJump(player);

        assertEquals(Player.JUMP_VELOCITY, player.getVelocity().y, 0.001f);
        assertFalse(player.isOnGround());
    }

    @Test
    void update_PlayerRunsIntoPositiveXWall_ShouldClampAgainstBlockFace() {
        world.addBlock(new Vector3(1, 0, 0), 0);
        player.setPosition(0.1f, 0f, 0f);
        player.setOnGround(true);
        player.getVelocity().set(12f, 0f, 0f);

        physics.update(player, 1f / 60f);

        assertEquals(0f, player.getVelocity().x, 0.001f);
        assertEquals(0.5f - Player.WIDTH / 2f - 0.001f, player.getPosition().x, 0.001f);
    }

    @Test
    void update_PlayerFallsThroughNonSolidBlock_ShouldKeepFalling() {
        BlockManager blockManager = new BlockManager();
        blockManager.setPropertiesProvider(blockType -> blockType != 6);
        world = new World(blockManager);
        physics = new PhysicsSystem(world);

        world.addBlock(new Vector3(0, 0, 0), 6);
        player.setPosition(0, 2, 0);
        player.setOnGround(false);
        player.getVelocity().y = -5f;

        for (int i = 0; i < 20; i++) {
            physics.update(player, 0.1f);
        }

        assertFalse(player.isOnGround());
        assertTrue(player.getPosition().y < 0.5f);
    }
}
