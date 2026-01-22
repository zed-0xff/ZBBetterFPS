package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.PerformanceSettings;

@Patch(className = "zombie.iso.IsoGridSquare", methodName = "setBlendFunc")
public class Patch_setBlendFunc {
    public static int lastState = -1; // -1: unknown, 0: default, 1: separate

    @Patch.OnEnter
    public static void enter() {
        if (!ZBBetterFPS.g_OptimizeGridSquare) return;

        int currentState = PerformanceSettings.fboRenderChunk ? 1 : 0;
        if (currentState == lastState) {
            // If the patcher supports skipping, we'd return false here.
            // Since we don't know, we'll just let it be for now or find a way to skip.
        }
        lastState = currentState;
    }
}
