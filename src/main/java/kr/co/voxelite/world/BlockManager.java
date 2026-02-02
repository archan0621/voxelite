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
            // ✅ Atlas-Safe 방식: UV는 항상 1타일 크기만 사용하므로 Repeat 불필요
            // 기본값 (ClampToEdge) 사용
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
     * Create model instances for visible faces only (Face Culling)
     * @param blockPosition 블록 위치
     * @param blockType 블록 타입
     * @param visibleFaces 가시성 배열 [front, back, left, right, top, bottom]
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
     * 청크 전체를 1개 통합 메시로 생성 (Greedy Meshing 적용!)
     * @param blocks 블록 리스트
     * @param visibleFacesMap 각 블록의 가시성 맵
     * @return 통합 Model (1 Draw Call + 면 개수 10~20배 감소)
     */
    public Model createChunkMesh(List<BlockData> blocks, Map<Vector3, boolean[]> visibleFacesMap) {
        if (blocks == null || blocks.isEmpty()) {
            return null;
        }
        
        // Greedy Meshing 알고리즘 적용
        List<GreedyMeshBuilder.MergedQuad> mergedQuads = 
            GreedyMeshBuilder.buildGreedyMesh(blocks, visibleFacesMap);
        
        if (mergedQuads.isEmpty()) {
            return null;
        }
        
        // 병합된 면들을 실제 메시로 변환
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
        
        // 병합된 각 면을 렌더링
        for (GreedyMeshBuilder.MergedQuad quad : mergedQuads) {
            Vector3 origin = quad.origin;
            int width = quad.width;
            int height = quad.height;
            int blockType = quad.blockType;
            int direction = quad.direction;
            
            // UV 계산 (블록 타입에 따라 아틀라스 타일 위치 결정)
            int tileX = blockType % atlasGridSize;
            int tileY = blockType / atlasGridSize;
            float u = tileX * tileSize;
            float v = tileY * tileSize;
            
            // 방향에 따라 큰 면 생성 (UV 반복 적용)
            createMergedFaceWithRepeatingUV(meshBuilder, origin, width, height, direction, s, normal, u, v, tileSize);
        }
        
        return builder.end();
    }
    
    /**
     * 병합된 큰 면을 작은 1×1 쿼드들로 분할 생성 (Atlas-Safe)
     * 
     * 핵심: width × height 병합 → width × height 개의 개별 쿼드
     * 각 쿼드는 정확히 1타일 크기의 UV만 사용 (아틀라스 규칙 준수)
     * 
     * 방향별 평면:
     * - Front/Back (0,1): XY 평면 → width=X, height=Y
     * - Left/Right (2,3): ZY 평면 → width=Z, height=Y
     * - Top/Bottom (4,5): XZ 평면 → width=X, height=Z
     */
    private void createMergedFaceWithRepeatingUV(MeshPartBuilder meshBuilder, Vector3 origin, 
                                                 int width, int height, int direction, 
                                                 float s, Vector3 normal, 
                                                 float u, float v, float tileSize) {
        // 블록 크기 = 2s (중심에서 ±s)
        float blockSize = 2 * s;
        
        // ✅ 각 1×1 블록 영역마다 개별 쿼드 생성
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // 방향에 따른 오프셋 계산
                float offsetX = 0, offsetY = 0, offsetZ = 0;
                
                switch (direction) {
                    case 0: // Front (z+) - XY 평면
                    case 1: // Back (z-) - XY 평면
                        offsetX = i * blockSize;  // width = X 방향
                        offsetY = j * blockSize;  // height = Y 방향
                        break;
                        
                    case 2: // Left (x-) - ZY 평면
                    case 3: // Right (x+) - ZY 평면
                        offsetZ = i * blockSize;  // width = Z 방향
                        offsetY = j * blockSize;   // height = Y 방향
                        break;
                        
                    case 4: // Top (y+) - XZ 평면
                    case 5: // Bottom (y-) - XZ 평면
                        offsetX = i * blockSize;   // width = X 방향
                        offsetZ = j * blockSize;   // height = Z 방향
                        break;
                }
                
                // ✅ UV는 항상 1타일 크기만 (아틀라스 경계 안)
                float localU = u;
                float localV = v;
                float localU2 = u + tileSize;
                float localV2 = v + tileSize;
                
                // 방향별 1×1 쿼드 생성
                createSingleBlockQuad(meshBuilder, origin, offsetX, offsetY, offsetZ, 
                                    direction, s, normal, localU, localV, localU2, localV2);
            }
        }
    }
    
    /**
     * 단일 1×1 블록 크기의 쿼드 생성 (아틀라스 안전)
     */
    private void createSingleBlockQuad(MeshPartBuilder meshBuilder, Vector3 origin,
                                      float offsetX, float offsetY, float offsetZ,
                                      int direction, float s, Vector3 normal,
                                      float u, float v, float u2, float v2) {
        float x = origin.x;
        float y = origin.y;
        float z = origin.z;
        
        // 블록 크기
        float blockSize = 2 * s;
        
        // ✅ 1×1 블록 크기 쿼드 (오프셋 적용)
        meshBuilder.setUVRange(u, v, u2, v2);
        
        switch (direction) {
            case 0: // Front (z+) - XY 평면
                normal.set(0, 0, 1);
                meshBuilder.rect(
                    new Vector3(x - s + offsetX, y - s + offsetY, z + s),
                    new Vector3(x + s + offsetX, y - s + offsetY, z + s),
                    new Vector3(x + s + offsetX, y + s + offsetY, z + s),
                    new Vector3(x - s + offsetX, y + s + offsetY, z + s),
                    normal
                );
                break;
                
            case 1: // Back (z-) - XY 평면
                normal.set(0, 0, -1);
                meshBuilder.rect(
                    new Vector3(x + s + offsetX, y - s + offsetY, z - s),
                    new Vector3(x - s + offsetX, y - s + offsetY, z - s),
                    new Vector3(x - s + offsetX, y + s + offsetY, z - s),
                    new Vector3(x + s + offsetX, y + s + offsetY, z - s),
                    normal
                );
                break;
                
            case 2: // Left (x-) - ZY 평면
                normal.set(-1, 0, 0);
                meshBuilder.rect(
                    new Vector3(x - s, y - s + offsetY, z - s + offsetZ),
                    new Vector3(x - s, y - s + offsetY, z + s + offsetZ),
                    new Vector3(x - s, y + s + offsetY, z + s + offsetZ),
                    new Vector3(x - s, y + s + offsetY, z - s + offsetZ),
                    normal
                );
                break;
                
            case 3: // Right (x+) - ZY 평면
                normal.set(1, 0, 0);
                meshBuilder.rect(
                    new Vector3(x + s, y - s + offsetY, z + s + offsetZ),
                    new Vector3(x + s, y - s + offsetY, z - s + offsetZ),
                    new Vector3(x + s, y + s + offsetY, z - s + offsetZ),
                    new Vector3(x + s, y + s + offsetY, z + s + offsetZ),
                    normal
                );
                break;
                
            case 4: // Top (y+) - XZ 평면
                normal.set(0, 1, 0);
                meshBuilder.rect(
                    new Vector3(x - s + offsetX, y + s, z + s + offsetZ),
                    new Vector3(x + s + offsetX, y + s, z + s + offsetZ),
                    new Vector3(x + s + offsetX, y + s, z - s + offsetZ),
                    new Vector3(x - s + offsetX, y + s, z - s + offsetZ),
                    normal
                );
                break;
                
            case 5: // Bottom (y-) - XZ 평면
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
