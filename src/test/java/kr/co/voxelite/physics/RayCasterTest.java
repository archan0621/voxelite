package kr.co.voxelite.physics;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import kr.co.voxelite.world.BlockManager;
import kr.co.voxelite.world.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RayCasterTest {
    private World world;

    @BeforeEach
    void setUp() {
        world = new World(new BlockManager());
    }

    @Test
    void raycast_ShouldFindNearestBlock() {
        world.addBlock(new Vector3(0, 0, 3), 0);
        world.addBlock(new Vector3(0, 0, 6), 0);
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, 1));

        Vector3 result = RayCaster.raycast(ray, world);

        assertNotNull(result);
        assertEquals(3f, result.z, 0.1f);
    }

    @Test
    void raycast_NoBlockInPath_ShouldReturnNull() {
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, 1));

        assertNull(RayCaster.raycast(ray, world));
    }

    @Test
    void raycastWithFace_ShouldReturnHitAndNormal() {
        world.addBlock(new Vector3(0, 0, 5), 0);
        Ray ray = new Ray(new Vector3(0, 0, 0), new Vector3(0, 0, 1));

        RaycastHit hit = RayCaster.raycastWithFace(ray, world);

        assertNotNull(hit);
        assertNotNull(hit.getBlockPosition());
        assertNotNull(hit.getNormal());
    }
}
