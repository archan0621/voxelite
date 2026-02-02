package kr.co.voxelite.world;

import com.badlogic.gdx.math.Vector3;

import java.io.*;
import java.util.Collection;

/**
 * Serializes and deserializes chunks to/from disk
 */
public class ChunkSerializer {
    
    /**
     * Save chunk to file
     */
    public static void saveChunk(Chunk chunk, File file) throws IOException {
        file.getParentFile().mkdirs(); // Create folder
        
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            
            // Header: chunk coordinates
            out.writeInt(chunk.getCoord().x);
            out.writeInt(chunk.getCoord().z);
            
            // Block count
            Collection<Chunk.BlockData> blocks = chunk.getBlocks();
            out.writeInt(blocks.size());
            
            // Save each block
            // ✅ Modified: Use BlockPos
            for (Chunk.BlockData block : blocks) {
                out.writeInt(block.pos.x());
                out.writeInt(block.pos.y());
                out.writeInt(block.pos.z());
                out.writeInt(block.blockType);
            }
        }
    }
    
    /**
     * Load chunk from file
     */
    public static Chunk loadChunk(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            
            // Header: chunk coordinates
            int chunkX = in.readInt();
            int chunkZ = in.readInt();
            
            Chunk chunk = new Chunk(new ChunkCoord(chunkX, chunkZ));
            
            // Block count
            int blockCount = in.readInt();

            System.out.println(
            "[CHUNK_LOAD] coord=(" + chunkX + "," + chunkZ + ")" +
            " blocks=" + blockCount +
            " file=" + file.getName()
            );
            
            // Load each block
            // ✅ Modified: Load as BlockPos
            for (int i = 0; i < blockCount; i++) {
                int localX = in.readInt();
                int blockY = in.readInt();
                int localZ = in.readInt();
                int blockType = in.readInt();
                
                chunk.addBlockLocal(localX, blockY, localZ, blockType);
            }
            
            // Set loaded chunks as generated = true
            chunk.markAsGenerated();
            
            return chunk;
        }
    }
    
    /**
     * Load chunk data into existing chunk object
     * ✅ Fill data into existing object (prevent placeholder replacement)
     */
    public static void loadInto(Chunk chunk, File file) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            
            // Header: chunk coordinates (for verification)
            int chunkX = in.readInt();
            int chunkZ = in.readInt();
            
            // Verify coordinate match
            if (chunk.getCoord().x != chunkX || chunk.getCoord().z != chunkZ) {
                throw new IOException("Chunk coordinate mismatch: expected " + chunk.getCoord() 
                    + ", got (" + chunkX + ", " + chunkZ + ")");
            }
            
            // Block count
            int blockCount = in.readInt();
            
            System.out.println(
                "[CHUNK_LOAD_INTO] coord=" + chunk.getCoord() +
                " blocks=" + blockCount +
                " file=" + file.getName()
            );
            
            // Load each block
            for (int i = 0; i < blockCount; i++) {
                int localX = in.readInt();
                int blockY = in.readInt();
                int localZ = in.readInt();
                int blockType = in.readInt();
                
                chunk.addBlockLocal(localX, blockY, localZ, blockType);
            }
            
            // Set loaded chunks as generated = true
            chunk.markAsGenerated();
        }
    }
    
    /**
     * Get chunk file path
     */
    public static File getChunkFile(String worldPath, ChunkCoord coord) {
        return new File(worldPath, String.format("chunks/chunk_%d_%d.dat", coord.x, coord.z));
    }
    
    /**
     * Check if chunk file exists
     */
    public static boolean chunkFileExists(String worldPath, ChunkCoord coord) {
        return getChunkFile(worldPath, coord).exists();
    }
}
