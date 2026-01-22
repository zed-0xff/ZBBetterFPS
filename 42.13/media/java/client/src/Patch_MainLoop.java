package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Patch;
import org.lwjglx.opengl.Display;
import zombie.GameTime;
import zombie.network.GameClient;
import zombie.network.GameServer;

public class Patch_MainLoop {

    @Patch(className = "zombie.GameWindow", methodName = "mainThreadStep")
    public static class GameWindowPatch {
        @Patch.OnExit
        public static void onExit() {
            if (!ZBBetterFPS.g_OptimizeMainLoop) return;

            try {
                // If the game is in background or paused, it spins at 100% CPU on a single core
                // because of Thread.yield() in MainThread.mainLoop.
                // We introduce a small sleep to release CPU time.
                
                boolean isPaused = GameTime.isGamePaused();
                boolean isFocused = Display.isActive();
                
                // Don't throttle if we are a server or in MP, might cause lag? 
                // Actually, MainThread is client-side in this context (zombie.GameWindow).
                
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
    public static class RenderStepPatch {
        @Patch.OnExit
        public static void onExit() {
            if (!ZBBetterFPS.g_OptimizeMainLoop) return;

            try {
                // RenderThread also spins on Thread.yield().
                if (!Display.isActive()) {
                    Thread.sleep(32); // ~30 FPS in background
                }
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }
}
