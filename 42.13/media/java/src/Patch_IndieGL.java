package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Patch;

/**
 * This patch optimizes the `IndieGL` class to suppress redundant OpenGL state changes.
 *
 * Vanilla behavior:
 * - `IndieGL` methods (like `glBlendFuncSeparate`) are called thousands of times per frame.
 * - Each call passes through the `GLState` system, which performs its own checks, but 
 *   even the overhead of these checks and object manipulation adds up in hot loops.
 *
 * Optimizations:
 * 1. Low-Level Caching: Caches the last values for blend functions, alpha tests, 
 *    and depth masks directly in the `IndieGL` patch.
 *    - Impact: Skips the entire `GLState` machinery if the requested state is already active.
 *      This provides a significant performance boost in terrain rendering and sprite batching.
 */
public class Patch_IndieGL {

    public static int lastBlendA, lastBlendB, lastBlendC, lastBlendD;
    public static int lastAlphaFunc;
    public static float lastAlphaRef = -1.0f;
    public static int lastDepthFunc = -1;
    public static int lastDepthMask = -1; // 0=false, 1=true
    public static boolean blendFirst = true;

    @Patch(className = "zombie.IndieGL", methodName = "glBlendFuncSeparate")
    public static class glBlendFuncSeparate {
        @Patch.OnEnter(skipOn = true)
        public static boolean onEnter(int a, int b, int c, int d) {
            if (!ZBBetterFPS.g_OptimizeIndieGL) return false;
            if (!blendFirst && a == lastBlendA && b == lastBlendB && c == lastBlendC && d == lastBlendD) {
                return true; // Skip redundant state change
            }
            lastBlendA = a; lastBlendB = b; lastBlendC = c; lastBlendD = d;
            blendFirst = false;
            return false; // Run original code
        }
    }

    @Patch(className = "zombie.IndieGL", methodName = "glBlendFunc")
    public static class glBlendFunc {
        @Patch.OnEnter(skipOn = true)
        public static boolean onEnter(int a, int b) {
            if (!ZBBetterFPS.g_OptimizeIndieGL) return false;
            if (!blendFirst && a == lastBlendA && b == lastBlendB && a == lastBlendC && b == lastBlendD) {
                return true;
            }
            lastBlendA = a; lastBlendB = b; lastBlendC = a; lastBlendD = b;
            blendFirst = false;
            return false;
        }
    }

    @Patch(className = "zombie.IndieGL", methodName = "glAlphaFunc")
    public static class glAlphaFunc {
        @Patch.OnEnter(skipOn = true)
        public static boolean onEnter(int func, float ref) {
            if (!ZBBetterFPS.g_OptimizeIndieGL) return false;
            if (func == lastAlphaFunc && ref == lastAlphaRef) {
                return true;
            }
            lastAlphaFunc = func;
            lastAlphaRef = ref;
            return false;
        }
    }

    @Patch(className = "zombie.IndieGL", methodName = "glDepthFunc")
    public static class glDepthFunc {
        @Patch.OnEnter(skipOn = true)
        public static boolean onEnter(int func) {
            if (!ZBBetterFPS.g_OptimizeIndieGL) return false;
            if (func == lastDepthFunc) {
                return true;
            }
            lastDepthFunc = func;
            return false;
        }
    }

    @Patch(className = "zombie.IndieGL", methodName = "glDepthMask")
    public static class glDepthMask {
        @Patch.OnEnter(skipOn = true)
        public static boolean onEnter(boolean mask) {
            if (!ZBBetterFPS.g_OptimizeIndieGL) return false;
            int m = mask ? 1 : 0;
            if (m == lastDepthMask) {
                return true;
            }
            lastDepthMask = m;
            return false;
        }
    }
}
