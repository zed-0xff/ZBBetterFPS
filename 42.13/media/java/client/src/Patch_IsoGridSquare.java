package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.PerformanceSettings;

@Patch(className = "zombie.iso.IsoGridSquare", methodName = "setBlendFunc")
public class Patch_IsoGridSquare {
    public static boolean lastState = false;

    @Patch.OnEnter(skipOn = true)
    public static boolean setBlendFunc() {
        if (ZBBetterFPS.g_OptimizeGridSquare) {
            if (lastState != PerformanceSettings.fboRenderChunk) {
                lastState = PerformanceSettings.fboRenderChunk;
                return false;
            }
            return true;
        } else {
            return false;
        }
    }
}
