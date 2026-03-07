package kr.co.voxelite.world;

/**
 * Headless block metadata registry.
 */
public class BlockManager {
    /**
     * Interface for providing custom textures for each face of a block.
     */
    public interface IBlockTextureProvider {
        int getTexture(int blockType, int faceIndex);
    }

    private IBlockTextureProvider textureProvider;

    public BlockManager() {
        this(null);
    }

    public BlockManager(IBlockTextureProvider textureProvider) {
        this.textureProvider = textureProvider;
    }

    public void setTextureProvider(IBlockTextureProvider provider) {
        textureProvider = provider;
    }

    public int getTexture(int blockType, int faceIndex) {
        if (textureProvider != null) {
            return textureProvider.getTexture(blockType, faceIndex);
        }
        return blockType;
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
