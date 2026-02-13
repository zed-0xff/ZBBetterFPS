package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Exposer;

@Exposer.LuaClass
public class ZBBetterFPS {
    // g_LowerCPUMode values
    public static final int CPU_MODE_PAUSED_OR_BG  = 1; // when paused OR background
    public static final int CPU_MODE_PAUSED_AND_BG = 2; // when paused AND background (default)
    public static final int CPU_MODE_ALWAYS         = 3; // always
    public static final int CPU_MODE_NEVER          = 4; // never

    public static int g_MaxRenderDistance = 0; // 0 = default value

    public static boolean g_OptimizeIndieGL = false;
    public static boolean g_OptimizeSpriteBatching = false;
    public static boolean g_OptimizeRingBuffer = false;
    public static boolean g_OptimizeDefaultShader = false;
    public static boolean g_Optimize3DModels = false;
    public static boolean g_OptimizeIsoMovingObject = false;
    public static int g_LowerCPUMode = CPU_MODE_PAUSED_AND_BG;
    public static boolean g_EnableMetrics = false;

    public static void setMaxRenderDistance(int distance) {
        g_MaxRenderDistance = distance;
    }

    public static void setOptimizeIndieGL(boolean b) { g_OptimizeIndieGL = b; }
    public static void setOptimizeSpriteBatching(boolean b) { g_OptimizeSpriteBatching = b; }
    public static void setOptimizeRingBuffer(boolean b) { g_OptimizeRingBuffer = b; }
    public static void setOptimizeDefaultShader(boolean b) { g_OptimizeDefaultShader = b; }
    public static void setOptimize3DModels(boolean b) { g_Optimize3DModels = b; }
    public static void setOptimizeIsoMovingObject(boolean b) { g_OptimizeIsoMovingObject = b; }
    public static void setLowerCPUMode(int mode) { g_LowerCPUMode = mode; }
    public static void setEnableMetrics(boolean b) { g_EnableMetrics = b; }
}
