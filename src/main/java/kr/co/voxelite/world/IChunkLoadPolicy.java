package kr.co.voxelite.world;

/**
 * 청크 로드 정책 인터페이스
 * 라이브러리는 인터페이스만 제공, 구현은 애플리케이션이 주입
 */
public interface IChunkLoadPolicy {
    /**
     * 메모리에 로드해야 하는가?
     */
    boolean shouldLoadToMemory(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ);
    
    /**
     * 사전 생성해야 하는가?
     */
    boolean shouldPregenerate(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ);
    
    /**
     * 최대 로드 청크 수
     */
    int getMaxLoadedChunks();
}
