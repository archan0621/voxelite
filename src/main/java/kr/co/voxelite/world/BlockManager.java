package kr.co.voxelite.world;

/**
 * Headless block metadata registry.
 */
public class BlockManager {
    /**
     * Interface for providing collision-related block properties.
     */
    public interface IBlockPropertiesProvider {
        boolean isSolid(int blockType);
    }

    /**
     * Interface for providing custom textures for each face of a block.
     */
    public interface IBlockTextureProvider {
        int getTexture(int blockType, int faceIndex);
    }

    /**
     * Interface for placing blocks into renderer passes.
     */
    public interface IBlockRenderLayerProvider {
        BlockRenderLayer getRenderLayer(int blockType);
    }

    private IBlockTextureProvider textureProvider;
    private IBlockPropertiesProvider propertiesProvider;

    public BlockManager() {
        this(null, null);
    }

    public BlockManager(IBlockTextureProvider textureProvider) {
        this(textureProvider, null);
    }

    public BlockManager(IBlockTextureProvider textureProvider, IBlockPropertiesProvider propertiesProvider) {
        this.textureProvider = textureProvider;
        this.propertiesProvider = propertiesProvider;
    }

    public void setTextureProvider(IBlockTextureProvider provider) {
        textureProvider = provider;
    }

    public void setPropertiesProvider(IBlockPropertiesProvider provider) {
        propertiesProvider = provider;
    }

    public int getTexture(int blockType, int faceIndex) {
        if (textureProvider != null) {
            return textureProvider.getTexture(blockType, faceIndex);
        }
        return blockType;
    }

    public boolean isSolid(int blockType) {
        if (blockType < 0) {
            return false;
        }
        if (propertiesProvider != null) {
            return propertiesProvider.isSolid(blockType);
        }
        return true;
    }

    public int[] getTextures(int blockType) {
        int[] textures = new int[6];
        for (int i = 0; i < textures.length; i++) {
            textures[i] = getTexture(blockType, i);
        }
        return textures;
    }

    public void dispose() {
    }
}
