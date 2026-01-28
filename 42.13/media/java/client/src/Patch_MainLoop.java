package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Patch;
import org.lwjglx.opengl.Display;
import zombie.GameTime;
import zombie.iso.IsoWorld;

public class Patch_MainLoop {

    @Patch(className = "zombie.GameWindow", methodName = "mainThreadStep")
    public static class GameWindowPatch {
        @Patch.OnExit
        public static void onExit() {
            if (!ZBBetterFPS.g_OptimizeMainLoop) return;

            try {
                // Only throttle main thread when paused AND unfocused together
                // This avoids the false positive from Display.isActive() in fullscreen/borderless
                boolean isPaused = GameTime.isGamePaused();
                boolean isFocused = Display.isActive();
                
                if (!isFocused && isPaused) {
                    Thread.sleep(32); // ~30 FPS when minimized and paused
                }
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    @Patch(className = "zombie.core.opengl.RenderThread", methodName = "renderStep")
    public static class RenderThreadStepPatch {
        @Patch.OnExit
        public static void onExit() {
            if (!ZBBetterFPS.g_OptimizeMainLoop) return;

            try {
                boolean isFocused = Display.isActive();
                boolean isPaused = GameTime.isGamePaused();
                
                // Only throttle render thread when truly in background (unfocused AND paused)
                // Display.isActive() can return false incorrectly in borderless/fullscreen modes
                if (!isFocused && isPaused) {
                    Thread.sleep(16); // ~60 FPS when minimized and paused
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
            if (!ZBBetterFPS.g_OptimizeMainLoop) return;

            // Only throttle if the game world is loaded
            if (IsoWorld.instance == null || IsoWorld.instance.currentCell == null) return;

            try {
                boolean isFocused = Display.isActive();
                boolean isPaused = GameTime.isGamePaused();
                
                // Only throttle lighting thread when truly in background
                if (!isFocused && isPaused) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }
}
