package kr.co.voxelite.world;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

/**
 * Chunk의 통합 렌더링 메시
 * - 청크 전체 = 1개 ModelInstance = 1 Draw Call
 * - GPU 캐싱
 */
public class ChunkMesh {
    private Model model;                // 통합 메시 (VBO)
    private ModelInstance instance;     // 렌더링 인스턴스
    private boolean dirty = true;
    
    /**
     * 통합 메시 설정
     */
    public void setModel(Model model) {
        // 기존 메시 정리
        if (this.model != null) {
            this.model.dispose();
        }
        
        this.model = model;
        this.instance = (model != null) ? new ModelInstance(model) : null;
        this.dirty = false;
    }
    
    /**
     * 렌더링용 인스턴스 반환
     */
    public ModelInstance getInstance() {
        return instance;
    }
    
    /**
     * 메시가 유효한가?
     */
    public boolean hasInstance() {
        return instance != null;
    }
    
    /**
     * 메시 무효화
     */
    public void markDirty() {
        this.dirty = true;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    /**
     * 메모리 정리
     */
    public void clear() {
        if (model != null) {
            model.dispose();
            model = null;
        }
        instance = null;
        dirty = true;
    }
    
    /**
     * Dispose 시 호출
     */
    public void dispose() {
        clear();
    }
}
