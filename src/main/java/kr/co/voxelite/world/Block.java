package kr.co.voxelite.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

/**
 * Block data class
 */
public class Block {
    private Vector3 position;
    private Color color;
    private BlockType type;

    public Block(Vector3 position, Color color, BlockType type) {
        this.position = new Vector3(position);
        this.color = color;
        this.type = type;
    }

    public Vector3 getPosition() {
        return position;
    }

    public Color getColor() {
        return color;
    }

    public BlockType getType() {
        return type;
    }

    public enum BlockType {
        GRASS, STONE, DIRT, WOOD
    }
}
