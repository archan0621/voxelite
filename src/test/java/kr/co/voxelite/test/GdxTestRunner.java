package kr.co.voxelite.test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import org.junit.jupiter.api.BeforeAll;

/**
 * Helper class to initialize LibGDX in headless mode for testing
 */
public abstract class GdxTestRunner {
    
    private static boolean initialized = false;
    
    @BeforeAll
    static void init() {
        if (!initialized) {
            HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
            config.updatesPerSecond = 60;
            new HeadlessApplication(new ApplicationAdapter() {}, config);
            
            // Wait for initialization
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            initialized = true;
        }
    }
}
