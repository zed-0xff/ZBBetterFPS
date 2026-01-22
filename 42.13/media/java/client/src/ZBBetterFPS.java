package me.zed_0xff.zb_better_fps;

public class ZBBetterFPS {
    public static int g_MaxRenderDistance = 0; // 0 = default value

    public static boolean g_OptimizeGridSquare = false;
    public static boolean g_OptimizeInventoryItem = false;
    public static boolean g_OptimizeIsoMovingObject = false;

    public static void setMaxRenderDistance(int distance) {
        g_MaxRenderDistance = (short) distance;
    }

    public static void setOptimizeGridSquare(boolean b) { g_OptimizeGridSquare = b; }
    public static void setOptimizeInventoryItem(boolean b) { g_OptimizeInventoryItem = b; }
    public static void setOptimizeIsoMovingObject(boolean b) { g_OptimizeIsoMovingObject = b; }
    }
