package kr.co.voxelite.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Block creation and model management with texture support
 */
public class BlockManager {
    private Texture blockAtlas;
    private int atlasGridSize = 16; // 16x16 grid
    private float tileSize; // UV size per tile
    
    private Map<Integer, Model[]> blockModelsCache; // Cache models per block type
    private float blockSize = 0.5f;

    public BlockManager(String atlasPath) {
        if (atlasPath != null) {
            this.blockAtlas = new Texture(Gdx.files.internal(atlasPath));
        }
        this.tileSize = 1.0f / atlasGridSize;
        this.blockModelsCache = new HashMap<>();
    }
    
    public BlockManager() {
        this(null);
    }

    /**
     * Get or create models for a specific block type
     */
    private Model[] getBlockModels(int blockType) {
        if (blockModelsCache.containsKey(blockType)) {
            return blockModelsCache.get(blockType);
        }
        
        Model[] models = createBlockFaceModels(blockType);
        blockModelsCache.put(blockType, models);
        return models;
    }
    
    /**
     * Create face models with texture for a specific block type
     */
    private Model[] createBlockFaceModels(int blockType) {
        if (blockAtlas == null) {
            return createColoredBlockModels(); // Fallback to colored blocks
        }
        
        ModelBuilder builder = new ModelBuilder();
        Model[] models = new Model[6];
        
        // Calculate UV coordinates for this block type
        int tileX = blockType % atlasGridSize;
        int tileY = blockType / atlasGridSize;
        float u = tileX * tileSize;
        float v = tileY * tileSize;
        float u2 = u + tileSize;
        float v2 = v + tileSize;
        
        System.out.println("[BlockManager] Block type " + blockType + 
            " UV: (" + u + "," + v + ") to (" + u2 + "," + v2 + 
            ") | Tile: (" + tileX + "," + tileY + ")");
        
        Material material = new Material(TextureAttribute.createDiffuse(blockAtlas));
        long attributes = VertexAttributes.Usage.Position | 
                         VertexAttributes.Usage.Normal | 
                         VertexAttributes.Usage.TextureCoordinates;
        
        float s = blockSize;
        Vector3 normal = new Vector3();
        
        // Front face (z+)
        builder.begin();
        MeshPartBuilder part = builder.part("front", GL20.GL_TRIANGLES, attributes, material);
        normal.set(0, 0, 1);
        part.setUVRange(u, v, u2, v2);
        part.rect(
            new Vector3(-s, -s, s), new Vector3(s, -s, s),
            new Vector3(s, s, s), new Vector3(-s, s, s),
            normal
        );
        models[0] = builder.end();
        
        // Back face (z-)
        builder.begin();
        part = builder.part("back", GL20.GL_TRIANGLES, attributes, material);
        normal.set(0, 0, -1);
        part.setUVRange(u, v, u2, v2);
        part.rect(
            new Vector3(s, -s, -s), new Vector3(-s, -s, -s),
            new Vector3(-s, s, -s), new Vector3(s, s, -s),
            normal
        );
        models[1] = builder.end();
        
        // Left face (x-)
        builder.begin();
        part = builder.part("left", GL20.GL_TRIANGLES, attributes, material);
        normal.set(-1, 0, 0);
        part.setUVRange(u, v, u2, v2);
        part.rect(
            new Vector3(-s, -s, -s), new Vector3(-s, -s, s),
            new Vector3(-s, s, s), new Vector3(-s, s, -s),
            normal
        );
        models[2] = builder.end();
        
        // Right face (x+)
        builder.begin();
        part = builder.part("right", GL20.GL_TRIANGLES, attributes, material);
        normal.set(1, 0, 0);
        part.setUVRange(u, v, u2, v2);
        part.rect(
            new Vector3(s, -s, s), new Vector3(s, -s, -s),
            new Vector3(s, s, -s), new Vector3(s, s, s),
            normal
        );
        models[3] = builder.end();
        
        // Top face (y+)
        builder.begin();
        part = builder.part("top", GL20.GL_TRIANGLES, attributes, material);
        normal.set(0, 1, 0);
        part.setUVRange(u, v, u2, v2);
        part.rect(
            new Vector3(-s, s, s), new Vector3(s, s, s),
            new Vector3(s, s, -s), new Vector3(-s, s, -s),
            normal
        );
        models[4] = builder.end();
        
        // Bottom face (y-)
        builder.begin();
        part = builder.part("bottom", GL20.GL_TRIANGLES, attributes, material);
        normal.set(0, -1, 0);
        part.setUVRange(u, v, u2, v2);
        part.rect(
            new Vector3(-s, -s, -s), new Vector3(s, -s, -s),
            new Vector3(s, -s, s), new Vector3(-s, -s, s),
            normal
        );
        models[5] = builder.end();
        
        return models;
    }
    
    /**
     * Fallback: Create colored block models (no texture)
     */
    private Model[] createColoredBlockModels() {
        ModelBuilder builder = new ModelBuilder();
        Model[] models = new Model[6];
        
        float s = blockSize;
        float t = 0.01f;
        
        models[0] = builder.createBox(1f, 1f, t, new Material(), 
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        models[1] = builder.createBox(1f, 1f, t, new Material(), 
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        models[2] = builder.createBox(t, 1f, 1f, new Material(), 
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        models[3] = builder.createBox(t, 1f, 1f, new Material(), 
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        models[4] = builder.createBox(1f, t, 1f, new Material(), 
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        models[5] = builder.createBox(1f, t, 1f, new Material(), 
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        
        return models;
    }

    /**
     * Create model instances for a block at the specified position with block type
     */
    public List<ModelInstance> createBlockInstances(Vector3 blockPosition, int blockType) {
        Model[] models = getBlockModels(blockType);
        List<ModelInstance> instances = new ArrayList<>();

        // Front (z+)
        ModelInstance frontFace = new ModelInstance(models[0]);
        frontFace.transform.setToTranslation(blockPosition.x, blockPosition.y, blockPosition.z);
        instances.add(frontFace);

        // Back (z-)
        ModelInstance backFace = new ModelInstance(models[1]);
        backFace.transform.setToTranslation(blockPosition.x, blockPosition.y, blockPosition.z);
        instances.add(backFace);

        // Left (x-)
        ModelInstance leftFace = new ModelInstance(models[2]);
        leftFace.transform.setToTranslation(blockPosition.x, blockPosition.y, blockPosition.z);
        instances.add(leftFace);

        // Right (x+)
        ModelInstance rightFace = new ModelInstance(models[3]);
        rightFace.transform.setToTranslation(blockPosition.x, blockPosition.y, blockPosition.z);
        instances.add(rightFace);

        // Top (y+)
        ModelInstance topFace = new ModelInstance(models[4]);
        topFace.transform.setToTranslation(blockPosition.x, blockPosition.y, blockPosition.z);
        instances.add(topFace);

        // Bottom (y-)
        ModelInstance bottomFace = new ModelInstance(models[5]);
        bottomFace.transform.setToTranslation(blockPosition.x, blockPosition.y, blockPosition.z);
        instances.add(bottomFace);

        return instances;
    }
    
    /**
     * Legacy method for backward compatibility (default blockType = 0)
     */
    public List<ModelInstance> createBlockInstances(Vector3 blockPosition) {
        return createBlockInstances(blockPosition, 0);
    }
    
    /**
     * Create a single face instance (for optimized chunk rendering)
     * @param blockPosition World position of the block
     * @param blockType Block type
     * @param faceIndex Face index (0=front, 1=back, 2=left, 3=right, 4=top, 5=bottom)
     * @return ModelInstance for the face
     */
    public ModelInstance createSingleFace(Vector3 blockPosition, int blockType, int faceIndex) {
        if (faceIndex < 0 || faceIndex > 5) {
            return null;
        }
        
        Model[] models = getBlockModels(blockType);
        ModelInstance instance = new ModelInstance(models[faceIndex]);
        instance.transform.setToTranslation(blockPosition.x, blockPosition.y, blockPosition.z);
        return instance;
    }

    public void dispose() {
        if (blockAtlas != null) {
            blockAtlas.dispose();
        }
        
        if (blockModelsCache != null) {
            for (Model[] models : blockModelsCache.values()) {
                if (models != null) {
                    for (Model model : models) {
                        if (model != null) model.dispose();
                    }
                }
            }
            blockModelsCache.clear();
        }
    }
}
