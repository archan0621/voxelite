package kr.co.voxelite.util;

/**
 * Performance logging utility for lag debugging.
 * Set ENABLED = false to disable all performance logs.
 */
public final class PerformanceLogger {
    /** Set to false to disable all performance logs */
    public static final boolean ENABLED = false;
    
    /** Log interval: log every N frames (1 = every frame) */
    public static final int LOG_INTERVAL = 60;
    
    private static int frameCount = 0;
    
    public static void log(String tag, String message, long timeMs) {
        if (ENABLED && frameCount % LOG_INTERVAL == 0) {
            System.out.printf("[PERF][%s] %s: %d ms%n", tag, message, timeMs);
        }
    }
    
    public static void log(String tag, String message) {
        if (ENABLED && frameCount % LOG_INTERVAL == 0) {
            System.out.printf("[PERF][%s] %s%n", tag, message);
        }
    }
    
    public static void logEveryFrame(String tag, String message, long timeMs) {
        if (ENABLED) {
            System.out.printf("[PERF][%s] %s: %d ms%n", tag, message, timeMs);
        }
    }
    
    /** @return frame count after increment */
    public static int tickFrame() {
        return ++frameCount;
    }
    
    public static long now() {
        return System.currentTimeMillis();
    }
    
    public static long nowNanos() {
        return System.nanoTime();
    }
}
