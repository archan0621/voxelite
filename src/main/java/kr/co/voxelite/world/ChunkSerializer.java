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
        file.getParentFile().mkdirs(); // 폴더 생성
        
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            
            // 헤더: 청크 좌표
            out.writeInt(chunk.getCoord().x);
            out.writeInt(chunk.getCoord().z);
            
            // 블록 수
            Collection<Chunk.BlockData> blocks = chunk.getBlocks();
            out.writeInt(blocks.size());
            
            // 각 블록 저장
            for (Chunk.BlockData block : blocks) {
                out.writeFloat(block.position.x);
                out.writeFloat(block.position.y);
                out.writeFloat(block.position.z);
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
            
            // 헤더: 청크 좌표
            int chunkX = in.readInt();
            int chunkZ = in.readInt();
            
            Chunk chunk = new Chunk(new ChunkCoord(chunkX, chunkZ));
            
            // 블록 수
            int blockCount = in.readInt();
            
            // 각 블록 로드
            for (int i = 0; i < blockCount; i++) {
                float x = in.readFloat();
                float y = in.readFloat();
                float z = in.readFloat();
                int blockType = in.readInt();
                
                Vector3 pos = new Vector3(x, y, z);
                chunk.addBlockWorld(pos, blockType);
            }
            
            // 로드된 청크는 generated = true로 설정
            chunk.markAsGenerated();
            
            return chunk;
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
