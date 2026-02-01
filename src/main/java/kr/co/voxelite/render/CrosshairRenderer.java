package kr.co.voxelite.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Renders a crosshair at screen center.
 */
public class CrosshairRenderer {
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera uiCamera;
    private float crosshairSize = 10f;
    private float crosshairThickness = 2f;

    public CrosshairRenderer(int screenWidth, int screenHeight) {
        shapeRenderer = new ShapeRenderer();
        uiCamera = new OrthographicCamera(screenWidth, screenHeight);
        uiCamera.setToOrtho(false, screenWidth, screenHeight);
    }

    public void render(int screenWidth, int screenHeight) {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        uiCamera.viewportWidth = screenWidth;
        uiCamera.viewportHeight = screenHeight;
        uiCamera.update();

        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);

        shapeRenderer.rect(centerX - crosshairSize / 2f, centerY - crosshairThickness / 2f,
                         crosshairSize, crosshairThickness);

        shapeRenderer.rect(centerX - crosshairThickness / 2f, centerY - crosshairSize / 2f,
                         crosshairThickness, crosshairSize);

        shapeRenderer.end();

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    public void resize(int width, int height) {
        uiCamera.viewportWidth = width;
        uiCamera.viewportHeight = height;
        uiCamera.setToOrtho(false, width, height);
        uiCamera.update();
    }

    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
