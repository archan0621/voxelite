package kr.co.voxelite.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import kr.co.voxelite.world.Chunk;
import kr.co.voxelite.world.ChunkCoord;

/**
 * HUD Renderer - Display player information
 */
public class HudRenderer {
    private SpriteBatch batch;
    private BitmapFont font;
    
    public HudRenderer() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.5f);
    }
    
    /**
     * Render player position and chunk coordinates
     */
    public void render(Vector3 playerPos, int screenWidth, int screenHeight) {
        // World coordinates → Chunk coordinates conversion
        ChunkCoord chunkCoord = ChunkCoord.fromWorldPos(playerPos.x, playerPos.z, Chunk.CHUNK_SIZE);
        
        // Block coordinates (integer)
        int blockX = (int) Math.floor(playerPos.x);
        int blockY = (int) Math.floor(playerPos.y);
        int blockZ = (int) Math.floor(playerPos.z);
        
        batch.begin();
        
        // Display information on top left
        float x = 10;
        float y = screenHeight - 20;
        
        font.draw(batch, String.format("Position: (%d, %d, %d)", blockX, blockY, blockZ), x, y);
        y -= 25;
        font.draw(batch, String.format("Chunk: (%d, %d)", chunkCoord.x, chunkCoord.z), x, y);
        y -= 25;
        font.draw(batch, String.format("FPS: %d", Gdx.graphics.getFramesPerSecond()), x, y);
        
        batch.end();
    }
    
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
    }
}
