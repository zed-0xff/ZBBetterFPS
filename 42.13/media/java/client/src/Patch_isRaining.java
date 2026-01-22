package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.iso.weather.ClimateManager;

@Patch(className = "zombie.iso.objects.RainManager", methodName = "isRaining")
public class Patch_isRaining {
    @Patch.OnExit
    public static void onExit(@Patch.Return(readOnly = false) Boolean returnValue) {
        if (!ZBBetterFPS.g_OptimizeRainManager) return;
        // Avoid potential multiple Boolean.valueOf calls by using cached constants
        returnValue = ClimateManager.getInstance().isRaining() ? Boolean.TRUE : Boolean.FALSE;
    }
}
