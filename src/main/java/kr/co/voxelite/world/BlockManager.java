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

import kr.co.voxelite.util.PerformanceLogger;

/**
 * Block creation and model management with texture support
 */
public class BlockManager {
    /**
     * Interface for providing custom textures for each face of a block
     */
    public interface IBlockTextureProvider {
        /**
         * Get texture index for a specific face of a block
         * @param blockType Block type ID
         * @param faceIndex Face index (0=front/z+, 1=back/z-, 2=left/x-, 3=right/x+, 4=top/y+, 5=bottom/y-)
         * @return Texture index in the atlas
         */
        int getTexture(int blockType, int faceIndex);
    }
    
    private Texture blockAtlas;
    private int atlasGridSize = 16; // 16x16 grid
    private float tileSize; // UV size per tile
    
    private Map<Integer, Model[]> blockModelsCache; // Cache models per block type
    private float blockSize = 0.5f;
    
    private IBlockTextureProvider textureProvider; // Custom texture provider

    public BlockManager(String atlasPath) {
        this(atlasPath, null);
    }
    
    public BlockManager(String atlasPath, IBlockTextureProvider textureProvider) {
        if (atlasPath != null) {
            this.blockAtlas = new Texture(Gdx.files.internal(atlasPath));
            // ✅ Atlas-Safe approach: UV always uses only 1 tile size, so Repeat is unnecessary
            // Use default (ClampToEdge)
        }
        this.tileSize = 1.0f / atlasGridSize;
        this.blockModelsCache = new HashMap<>();
        this.textureProvider = textureProvider;
    }
    
    public BlockManager() {
        this(null, null);
    }
    
    /**
     * Set custom texture provider
     */
    public void setTextureProvider(IBlockTextureProvider provider) {
        this.textureProvider = provider;
        // Clear cache to regenerate models with new textures
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
     * Supports different textures for each face via IBlockTextureProvider
     */
    private Model[] createBlockFaceModels(int blockType) {
        if (blockAtlas == null) {
            return createColoredBlockModels(); // Fallback to colored blocks
        }
        
        ModelBuilder builder = new ModelBuilder();
        Model[] models = new Model[6];
        
        Material material = new Material(TextureAttribute.createDiffuse(blockAtlas));
        long attributes = VertexAttributes.Usage.Position | 
                         VertexAttributes.Usage.Normal | 
                         VertexAttributes.Usage.TextureCoordinates;
        
        float s = blockSize;
        Vector3 normal = new Vector3();
        
        // Create each face with its specific texture
        for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
            int textureIndex = getTextureForFace(blockType, faceIndex);
            
            // Calculate UV coordinates for this texture
            int tileX = textureIndex % atlasGridSize;
            int tileY = textureIndex / atlasGridSize;
            float u = tileX * tileSize;
            float v = tileY * tileSize;
            float u2 = u + tileSize;
            float v2 = v + tileSize;
            
            builder.begin();
            MeshPartBuilder part = builder.part(getFaceName(faceIndex), GL20.GL_TRIANGLES, attributes, material);
            part.setUVRange(u, v, u2, v2);
            
            switch (faceIndex) {
                case 0: // Front face (z+)
                    normal.set(0, 0, 1);
                    part.rect(
                        new Vector3(-s, -s, s), new Vector3(s, -s, s),
                        new Vector3(s, s, s), new Vector3(-s, s, s),
                        normal
                    );
                    break;
                    
                case 1: // Back face (z-)
                    normal.set(0, 0, -1);
                    part.rect(
                        new Vector3(s, -s, -s), new Vector3(-s, -s, -s),
                        new Vector3(-s, s, -s), new Vector3(s, s, -s),
                        normal
                    );
                    break;
                    
                case 2: // Left face (x-)
                    normal.set(-1, 0, 0);
                    part.rect(
                        new Vector3(-s, -s, -s), new Vector3(-s, -s, s),
                        new Vector3(-s, s, s), new Vector3(-s, s, -s),
                        normal
                    );
                    break;
                    
                case 3: // Right face (x+)
                    normal.set(1, 0, 0);
                    part.rect(
                        new Vector3(s, -s, s), new Vector3(s, -s, -s),
                        new Vector3(s, s, -s), new Vector3(s, s, s),
                        normal
                    );
                    break;
                    
                case 4: // Top face (y+)
                    normal.set(0, 1, 0);
                    part.rect(
                        new Vector3(-s, s, s), new Vector3(s, s, s),
                        new Vector3(s, s, -s), new Vector3(-s, s, -s),
                        normal
                    );
                    break;
                    
                case 5: // Bottom face (y-)
                    normal.set(0, -1, 0);
                    part.rect(
                        new Vector3(-s, -s, -s), new Vector3(s, -s, -s),
                        new Vector3(s, -s, s), new Vector3(-s, -s, s),
                        normal
                    );
                    break;
            }
            
            models[faceIndex] = builder.end();
        }
        
        return models;
    }
    
    /**
     * Get texture index for a specific face of a block
     * Can be overridden by providing IBlockTextureProvider
     */
    protected int getTextureForFace(int blockType, int faceIndex) {
        if (textureProvider != null) {
            return textureProvider.getTexture(blockType, faceIndex);
        }
        // Default: all faces use blockType as texture index
        return blockType;
    }
    
    /**
     * Get face name for debugging
     */
    private String getFaceName(int faceIndex) {
        switch (faceIndex) {
            case 0: return "front";
            case 1: return "back";
            case 2: return "left";
            case 3: return "right";
            case 4: return "top";
            case 5: return "bottom";
            default: return "unknown";
        }
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
     * Create model instances for visible faces only (Face Culling)
     * @param blockPosition Block position
     * @param blockType Block type
     * @param visibleFaces Visibility array [front, back, left, right, top, bottom]
     */
    public List<ModelInstance> createVisibleFaces(Vector3 blockPosition, int blockType, boolean[] visibleFaces) {
        if (visibleFaces == null || visibleFaces.length != 6) {
            return createBlockInstances(blockPosition, blockType); // Fallback
        }
        
        Model[] models = getBlockModels(blockType);
        List<ModelInstance> instances = new ArrayList<>();
        
        // 0: Front (z+)
        if (visibleFaces[0]) {
            ModelInstance face = new ModelInstance(models[0]);
            face.transform.setToTranslation(blockPosition.x, blockPosition.y, blockPosition.z);
            instances.add(face);
        }
        
        // 1: Back (z-)
        if (visibleFaces[1]) {
            ModelInstance face = new ModelInstance(models[1]);
            face.transform.setToTranslation(blockPosition.x, blockPosition.y, blockPosition.z);
            instances.add(face);
        }
        
        // 2: Left (x-)
        if (visibleFaces[2]) {
            ModelInstance face = new ModelInstance(models[2]);
            face.transform.setToTranslation(blockPosition.x, blockPosition.y, blockPosition.z);
            instances.add(face);
        }
        
        // 3: Right (x+)
        if (visibleFaces[3]) {
            ModelInstance face = new ModelInstance(models[3]);
            face.transform.setToTranslation(blockPosition.x, blockPosition.y, blockPosition.z);
            instances.add(face);
        }
        
        // 4: Top (y+)
        if (visibleFaces[4]) {
            ModelInstance face = new ModelInstance(models[4]);
            face.transform.setToTranslation(blockPosition.x, blockPosition.y, blockPosition.z);
            instances.add(face);
        }
        
        // 5: Bottom (y-)
        if (visibleFaces[5]) {
            ModelInstance face = new ModelInstance(models[5]);
            face.transform.setToTranslation(blockPosition.x, blockPosition.y, blockPosition.z);
            instances.add(face);
        }
        
        return instances;
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
    
    /**
     * Create entire chunk as one unified mesh (Greedy Meshing applied!)
     * @param blocks Block list
     * @param visibleFacesMap Visibility map for each block
     * @return Unified Model (1 Draw Call + 10~20x face count reduction)
     */
    public Model createChunkMesh(List<BlockData> blocks, Map<Vector3, boolean[]> visibleFacesMap) {
        if (blocks == null || blocks.isEmpty()) {
            return null;
        }
        
        long t0 = PerformanceLogger.now();
        // Apply Greedy Meshing algorithm
        List<GreedyMeshBuilder.MergedQuad> mergedQuads = 
            GreedyMeshBuilder.buildGreedyMesh(blocks, visibleFacesMap);
        long t1 = PerformanceLogger.now();
        
        if (mergedQuads.isEmpty()) {
            return null;
        }
        
        // Convert merged faces to actual mesh
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        
        Material material = (blockAtlas != null) 
            ? new Material(TextureAttribute.createDiffuse(blockAtlas))
            : new Material();
        
        long attributes = VertexAttributes.Usage.Position 
                        | VertexAttributes.Usage.Normal 
                        | VertexAttributes.Usage.TextureCoordinates;
        
        MeshPartBuilder meshBuilder = builder.part(
            "chunk", 
            GL20.GL_TRIANGLES, 
            attributes, 
            material
        );
        
        Vector3 normal = new Vector3();
        float s = blockSize;
        
        // Render each merged face
        for (GreedyMeshBuilder.MergedQuad quad : mergedQuads) {
            Vector3 origin = quad.origin;
            int width = quad.width;
            int height = quad.height;
            int blockType = quad.blockType;
            int direction = quad.direction;
            
            // Get texture for this specific face
            int textureIndex = getTextureForFace(blockType, direction);
            
            // UV calculation (determine atlas tile position by texture index)
            int tileX = textureIndex % atlasGridSize;
            int tileY = textureIndex / atlasGridSize;
            float u = tileX * tileSize;
            float v = tileY * tileSize;
            
            // Create large face according to direction (apply repeating UV)
            createMergedFaceWithRepeatingUV(meshBuilder, origin, width, height, direction, s, normal, u, v, tileSize);
        }
        
        Model model = builder.end();
        long t2 = PerformanceLogger.now();
        if (PerformanceLogger.ENABLED && (t2 - t0) > 10) {
            System.out.printf("[PERF][BlockManager] createChunkMesh: greedy=%dms mesh=%dms quads=%d blocks=%d%n",
                t1 - t0, t2 - t1, mergedQuads.size(), blocks.size());
        }
        return model;
    }
    
    /**
     * Split merged large face into small 1×1 quads (Atlas-Safe)
     * 
     * Core: width × height merge → width × height individual quads
     * Each quad uses exactly 1 tile size UV (follows atlas rules)
     * 
     * Planes by direction:
     * - Front/Back (0,1): XY plane → width=X, height=Y
     * - Left/Right (2,3): ZY plane → width=Z, height=Y
     * - Top/Bottom (4,5): XZ plane → width=X, height=Z
     */
    private void createMergedFaceWithRepeatingUV(MeshPartBuilder meshBuilder, Vector3 origin, 
                                                 int width, int height, int direction, 
                                                 float s, Vector3 normal, 
                                                 float u, float v, float tileSize) {
        // Block size = 2s (±s from center)
        float blockSize = 2 * s;
        
        // ✅ Create individual quad for each 1×1 block area
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // Calculate offset by direction
                float offsetX = 0, offsetY = 0, offsetZ = 0;
                
                switch (direction) {
                    case 0: // Front (z+) - XY plane
                    case 1: // Back (z-) - XY plane
                        offsetX = i * blockSize;  // width = X direction
                        offsetY = j * blockSize;  // height = Y direction
                        break;
                        
                    case 2: // Left (x-) - ZY plane
                    case 3: // Right (x+) - ZY plane
                        offsetZ = i * blockSize;  // width = Z direction
                        offsetY = j * blockSize;   // height = Y direction
                        break;
                        
                    case 4: // Top (y+) - XZ plane
                    case 5: // Bottom (y-) - XZ plane
                        offsetX = i * blockSize;   // width = X direction
                        offsetZ = j * blockSize;   // height = Z direction
                        break;
                }
                
                // ✅ UV always uses only 1 tile size (within atlas boundary)
                float localU = u;
                float localV = v;
                float localU2 = u + tileSize;
                float localV2 = v + tileSize;
                
                // Create 1×1 quad by direction
                createSingleBlockQuad(meshBuilder, origin, offsetX, offsetY, offsetZ, 
                                    direction, s, normal, localU, localV, localU2, localV2);
            }
        }
    }
    
    /**
     * Create single 1×1 block size quad (atlas safe)
     */
    private void createSingleBlockQuad(MeshPartBuilder meshBuilder, Vector3 origin,
                                      float offsetX, float offsetY, float offsetZ,
                                      int direction, float s, Vector3 normal,
                                      float u, float v, float u2, float v2) {
        float x = origin.x;
        float y = origin.y;
        float z = origin.z;
        
        // Block size
        float blockSize = 2 * s;
        
        // ✅ 1×1 block size quad (apply offset)
        meshBuilder.setUVRange(u, v, u2, v2);
        
        switch (direction) {
            case 0: // Front (z+) - XY plane
                normal.set(0, 0, 1);
                meshBuilder.rect(
                    new Vector3(x - s + offsetX, y - s + offsetY, z + s),
                    new Vector3(x + s + offsetX, y - s + offsetY, z + s),
                    new Vector3(x + s + offsetX, y + s + offsetY, z + s),
                    new Vector3(x - s + offsetX, y + s + offsetY, z + s),
                    normal
                );
                break;
                
            case 1: // Back (z-) - XY plane
                normal.set(0, 0, -1);
                meshBuilder.rect(
                    new Vector3(x + s + offsetX, y - s + offsetY, z - s),
                    new Vector3(x - s + offsetX, y - s + offsetY, z - s),
                    new Vector3(x - s + offsetX, y + s + offsetY, z - s),
                    new Vector3(x + s + offsetX, y + s + offsetY, z - s),
                    normal
                );
                break;
                
            case 2: // Left (x-) - ZY plane
                normal.set(-1, 0, 0);
                meshBuilder.rect(
                    new Vector3(x - s, y - s + offsetY, z - s + offsetZ),
                    new Vector3(x - s, y - s + offsetY, z + s + offsetZ),
                    new Vector3(x - s, y + s + offsetY, z + s + offsetZ),
                    new Vector3(x - s, y + s + offsetY, z - s + offsetZ),
                    normal
                );
                break;
                
            case 3: // Right (x+) - ZY plane
                normal.set(1, 0, 0);
                meshBuilder.rect(
                    new Vector3(x + s, y - s + offsetY, z + s + offsetZ),
                    new Vector3(x + s, y - s + offsetY, z - s + offsetZ),
                    new Vector3(x + s, y + s + offsetY, z - s + offsetZ),
                    new Vector3(x + s, y + s + offsetY, z + s + offsetZ),
                    normal
                );
                break;
                
            case 4: // Top (y+) - XZ plane
                normal.set(0, 1, 0);
                meshBuilder.rect(
                    new Vector3(x - s + offsetX, y + s, z + s + offsetZ),
                    new Vector3(x + s + offsetX, y + s, z + s + offsetZ),
                    new Vector3(x + s + offsetX, y + s, z - s + offsetZ),
                    new Vector3(x - s + offsetX, y + s, z - s + offsetZ),
                    normal
                );
                break;
                
            case 5: // Bottom (y-) - XZ plane
                normal.set(0, -1, 0);
                meshBuilder.rect(
                    new Vector3(x - s + offsetX, y - s, z - s + offsetZ),
                    new Vector3(x + s + offsetX, y - s, z - s + offsetZ),
                    new Vector3(x + s + offsetX, y - s, z + s + offsetZ),
                    new Vector3(x - s + offsetX, y - s, z + s + offsetZ),
                    normal
                );
                break;
        }
    }
    
    /**
     * BlockData helper class
     */
    public static class BlockData {
        public final Vector3 position;
        public final int blockType;
        
        public BlockData(Vector3 position, int blockType) {
            this.position = position;
            this.blockType = blockType;
        }
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
