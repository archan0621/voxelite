package kr.co.voxelite.world;

/**
 * Chunk의 생명주기 상태
 */
public enum ChunkState {
    EMPTY,      // 존재만 함
    GENERATED,  // 블록 생성됨
    MESHED,     // 메시 생성됨
    UPLOADED,   // GPU 업로드 완료 (필요시)
    ACTIVE      // 렌더/물리 사용 중
}
