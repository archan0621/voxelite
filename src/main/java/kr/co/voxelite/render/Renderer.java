package kr.co.voxelite.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.camera.FPSCamera;
import kr.co.voxelite.world.World;

/**
 * Main renderer coordinating all rendering components.
 */
public class Renderer {
    private BlockRenderer blockRenderer;
    private CrosshairRenderer crosshairRenderer;
    private BlockOutlineRenderer blockOutlineRenderer;
    private HudRenderer hudRenderer;

    public Renderer(int screenWidth, int screenHeight) {
        blockRenderer = new BlockRenderer();
        crosshairRenderer = new CrosshairRenderer(screenWidth, screenHeight);
        blockOutlineRenderer = new BlockOutlineRenderer();
        hudRenderer = new HudRenderer();
    }

    public void render(FPSCamera fpsCamera, World world, int logicalWidth, int logicalHeight, Vector3 selectedBlock, Vector3 playerPos) {
        int backBufferWidth = Gdx.graphics.getBackBufferWidth();
        int backBufferHeight = Gdx.graphics.getBackBufferHeight();
        Gdx.gl.glViewport(0, 0, backBufferWidth, backBufferHeight);

        Gdx.gl.glClearColor(0.87f, 0.95f, 1.0f, 1f); // Sky blue background
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        fpsCamera.resize(logicalWidth, logicalHeight);

        blockRenderer.render(fpsCamera.getCamera(), world);

        if (selectedBlock != null) {
            blockOutlineRenderer.render(fpsCamera.getCamera(), selectedBlock);
        }

        crosshairRenderer.render(logicalWidth, logicalHeight);
        
        // HUD rendering (2D, last)
        hudRenderer.render(playerPos, logicalWidth, logicalHeight);
    }

    public void resize(int width, int height) {
        crosshairRenderer.resize(width, height);
    }

    public void dispose() {
        if (blockRenderer != null) blockRenderer.dispose();
        if (crosshairRenderer != null) crosshairRenderer.dispose();
        if (blockOutlineRenderer != null) blockOutlineRenderer.dispose();
        if (hudRenderer != null) hudRenderer.dispose();
    }
}
