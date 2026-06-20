package kr.co.voxelite.util;

/**
 * Performance logging utility for lag debugging.
 * Enable with -Dopencraft.perf=true or OPENCRAFT_PERF_LOGS=true.
 */
public final class PerformanceLogger {
    public static final boolean ENABLED = Boolean.getBoolean("opencraft.perf")
        || Boolean.parseBoolean(System.getenv().getOrDefault("OPENCRAFT_PERF_LOGS", "false"));
    
    /** Log interval: log every N frames (1 = every frame). */
    public static final int LOG_INTERVAL = Math.max(1, Integer.getInteger("opencraft.perf.interval", 300));
    public static final int SLOW_FRAME_MS = Math.max(1, Integer.getInteger("opencraft.perf.slowFrameMs", 25));
    public static final int SLOW_RENDER_MS = Math.max(1, Integer.getInteger("opencraft.perf.slowRenderMs", 12));
    public static final int SLOW_COLLECT_MS = Math.max(1, Integer.getInteger("opencraft.perf.slowCollectMs", 6));
    public static final int SLOW_MESH_PROCESS_MS = Math.max(1, Integer.getInteger("opencraft.perf.slowMeshProcessMs", 16));
    public static final int SLOW_MESH_PREPARE_MS = Math.max(1, Integer.getInteger("opencraft.perf.slowMeshPrepareMs", 20));
    public static final int SLOW_MESH_BUILD_MS = Math.max(1, Integer.getInteger("opencraft.perf.slowMeshBuildMs", 16));
    public static final int SLOW_CHUNK_PROCESS_MS = Math.max(1, Integer.getInteger("opencraft.perf.slowChunkProcessMs", 10));
    
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

    public static boolean shouldLogInterval() {
        return ENABLED && frameCount % LOG_INTERVAL == 0;
    }

    public static boolean shouldLogSlow(long elapsedMs, int thresholdMs) {
        return ENABLED && elapsedMs >= thresholdMs;
    }

    public static boolean shouldLogSlowOrInterval(long elapsedMs, int thresholdMs) {
        return ENABLED && (elapsedMs >= thresholdMs || shouldLogInterval());
    }
    
    public static long now() {
        return System.currentTimeMillis();
    }
    
    public static long nowNanos() {
        return System.nanoTime();
    }
}
