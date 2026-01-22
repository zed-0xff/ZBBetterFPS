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
                boolean isPaused = GameTime.isGamePaused();
                boolean isFocused = Display.isActive();
                
                if (!isFocused) {
                    Thread.sleep(32); // ~30 FPS in background
                } else if (isPaused) {
                    Thread.sleep(16); // ~60 FPS when paused
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
                if (!Display.isActive()) {
                    Thread.sleep(32); // ~30 FPS in background
                } else if (GameTime.isGamePaused()) {
                    Thread.sleep(8); // ~120 FPS max when paused
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
                if (!Display.isActive()) {
                    Thread.sleep(100); 
                } else if (GameTime.isGamePaused()) {
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }
}
