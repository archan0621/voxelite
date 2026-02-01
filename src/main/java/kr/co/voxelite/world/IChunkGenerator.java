package kr.co.voxelite.world;

/**
 * 청크 생성 인터페이스
 * 라이브러리는 인터페이스만 제공, 구현은 애플리케이션이 주입
 */
public interface IChunkGenerator {
    /**
     * 청크 생성
     * @param chunk 생성할 청크
     * @param blockType 기본 블록 타입
     */
    void generateChunk(Chunk chunk, int blockType);
}
