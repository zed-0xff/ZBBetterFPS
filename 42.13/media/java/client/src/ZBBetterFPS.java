package me.zed_0xff.zb_better_fps;

public class ZBBetterFPS {
    public static int g_MaxRenderDistance = 0; // 0 = default value

    public static boolean g_OptimizeIndieGL = false;
    public static boolean g_OptimizeSpriteBatching = false;
    public static boolean g_OptimizeRingBuffer = false;
    public static boolean g_OptimizeDefaultShader = false;
    public static boolean g_Optimize3DModels = false;
    public static boolean g_OptimizeIsoMovingObject = false;
    public static boolean g_OptimizeMainLoop = false;
    public static boolean g_EnableMetrics = false;

    public static void setMaxRenderDistance(int distance) {
        g_MaxRenderDistance = (short) distance;
    }

    public static void setOptimizeIndieGL(boolean b) { g_OptimizeIndieGL = b; }
    public static void setOptimizeSpriteBatching(boolean b) { g_OptimizeSpriteBatching = b; }
    public static void setOptimizeRingBuffer(boolean b) { g_OptimizeRingBuffer = b; }
    public static void setOptimizeDefaultShader(boolean b) { g_OptimizeDefaultShader = b; }
    public static void setOptimize3DModels(boolean b) { g_Optimize3DModels = b; }
    public static void setOptimizeIsoMovingObject(boolean b) { g_OptimizeIsoMovingObject = b; }
    public static void setOptimizeMainLoop(boolean b) { g_OptimizeMainLoop = b; }
    public static void setEnableMetrics(boolean b) { g_EnableMetrics = b; }
}
