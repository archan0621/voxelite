package kr.co.voxelite.render;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import kr.co.voxelite.util.PerformanceLogger;
import kr.co.voxelite.world.World;

import java.util.List;

/**
 * Renders all blocks in the world.
 */
public class BlockRenderer {
    private ModelBatch modelBatch;
    private Environment environment;

    public BlockRenderer() {
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(
            ColorAttribute.AmbientLight,
            1f, 1f, 1f, 1f
        ));
    }

    public void render(PerspectiveCamera camera, World world) {
        long t0 = PerformanceLogger.now();
        modelBatch.begin(camera);
        // Frustum Culling: render only chunks visible on screen
        List<ModelInstance> instances = world.getAllBlockInstances(camera);
        long t1 = PerformanceLogger.now();
        for (ModelInstance instance : instances) {
            modelBatch.render(instance, environment);
        }
        modelBatch.end();
        long t2 = PerformanceLogger.now();
        if (PerformanceLogger.ENABLED && (t2 - t0 > 5 || instances.size() > 30)) {
            System.out.printf("[PERF][BlockRenderer] getInstances=%dms render=%dms drawCalls=%d%n",
                t1 - t0, t2 - t1, instances.size());
        }
    }

    public void dispose() {
        if (modelBatch != null) {
            modelBatch.dispose();
        }
    }
}
