package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Patch;
import org.lwjglx.opengl.Display;
import zombie.GameTime;
import zombie.core.PerformanceSettings;

public class Patch_MainLoop_B42 {
    public static final boolean ALL_FIELDS_FOUND = true; // for uniformity

    // Frame timing for "always" mode
    public static long renderFrameStart = 0;
    public static long mainFrameStart = 0;

    // Returns true if game is paused AND in background (for longer sleeps)
    // In "always" mode, only check if paused (ignore window focus)
    public static boolean isFullyInactive() {
        if (ZBBetterFPS.g_LowerCPUMode == ZBBetterFPS.CPU_MODE_ALWAYS) {
            return !Utils.isGameStarted() || GameTime.isGamePaused();
        }
        return GameTime.isGamePaused() && !Display.isActive();
    }

    public static boolean shouldThrottle() {
        int mode = ZBBetterFPS.g_LowerCPUMode;
        if (mode == ZBBetterFPS.CPU_MODE_NEVER)  return false;
        if (mode == ZBBetterFPS.CPU_MODE_ALWAYS) return true;
        
        boolean isFocused = Display.isActive();
        boolean isPaused = GameTime.isGamePaused();
        
        if (mode == ZBBetterFPS.CPU_MODE_PAUSED_OR_BG) {
            // when paused OR background (more aggressive)
            return isPaused || !isFocused;
        } else if (mode == ZBBetterFPS.CPU_MODE_PAUSED_AND_BG) {
            // when paused AND background (conservative, default)
            return isPaused && !isFocused;
        }
        return false;
    }

    // Calculate safe sleep time based on target FPS and elapsed frame time
    public static long calcSleepTime(long frameStartNanos) {
        int framerate = PerformanceSettings.instance.getFramerate();
        if (framerate <= 1) return 1; // uncapped, minimal sleep
        
        long targetFrameTimeNanos = 1_000_000_000L / framerate;
        long elapsedNanos = System.nanoTime() - frameStartNanos;
        long remainingNanos = targetFrameTimeNanos - elapsedNanos;
        
        // Convert to ms, leave 2ms buffer for scheduling variance
        long sleepMs = (remainingNanos / 1_000_000L) - 2;
        return Math.max(1, sleepMs);
    }

    @Patch(className = "zombie.GameWindow", methodName = "mainThreadStep")
    public static class GameWindowPatch {
        @Patch.OnEnter
        public static void onEnter() {
            if (ZBBetterFPS.g_LowerCPUMode == ZBBetterFPS.CPU_MODE_ALWAYS) {
                mainFrameStart = System.nanoTime();
            }
        }

        @Patch.OnExit
        public static void onExit() {
            if (!shouldThrottle()) return;

            try {
                if (isFullyInactive()) {
                    Thread.sleep(32);
                } else if (ZBBetterFPS.g_LowerCPUMode == ZBBetterFPS.CPU_MODE_ALWAYS && mainFrameStart > 0) {
                    Thread.sleep(calcSleepTime(mainFrameStart));
                } else {
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    @Patch(className = "zombie.core.opengl.RenderThread", methodName = "renderStep")
    public static class RenderThreadStepPatch {
        @Patch.OnEnter
        public static void onEnter() {
            if (ZBBetterFPS.g_LowerCPUMode == ZBBetterFPS.CPU_MODE_ALWAYS) {
                renderFrameStart = System.nanoTime();
            }
        }

        @Patch.OnExit
        public static void onExit() {
            if (!shouldThrottle()) return;

            try {
                if (isFullyInactive()) {
                    Thread.sleep(16);
                } else if (ZBBetterFPS.g_LowerCPUMode == ZBBetterFPS.CPU_MODE_ALWAYS && renderFrameStart > 0) {
                    Thread.sleep(calcSleepTime(renderFrameStart));
                } else {
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    @Patch(className = "zombie.iso.LightingThread", methodName = "runInner")
    public static class LightingThreadPatch {
        @Patch.OnExit
        public static void onExit() {
            if (!shouldThrottle()) return;

            // Only throttle if the game world is loaded
            if (!Utils.isGameStarted()) return;

            try {
                Thread.sleep(isFullyInactive() ? 100 : 1);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }
}
