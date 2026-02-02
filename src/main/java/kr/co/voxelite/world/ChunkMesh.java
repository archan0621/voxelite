package kr.co.voxelite.world;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

/**
 * Unified rendering mesh for a chunk
 * - Entire chunk = 1 ModelInstance = 1 Draw Call
 * - GPU caching
 */
public class ChunkMesh {
    private Model model;                // Unified mesh (VBO)
    private ModelInstance instance;     // Rendering instance
    private boolean dirty = true;
    
    /**
     * Sets unified mesh
     */
    public void setModel(Model model) {
        // Clean up existing mesh
        if (this.model != null) {
            this.model.dispose();
        }
        
        this.model = model;
        this.instance = (model != null) ? new ModelInstance(model) : null;
        this.dirty = false;
    }
    
    /**
     * Returns rendering instance
     */
    public ModelInstance getInstance() {
        return instance;
    }
    
    /**
     * Is mesh valid?
     */
    public boolean hasInstance() {
        return instance != null;
    }
    
    /**
     * Invalidates mesh
     */
    public void markDirty() {
        this.dirty = true;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    /**
     * Clears memory
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
     * Called on dispose
     */
    public void dispose() {
        clear();
    }
}
