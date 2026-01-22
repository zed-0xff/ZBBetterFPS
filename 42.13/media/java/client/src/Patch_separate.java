// package me.zed_0xff.zb_better_fps;

// import me.zed_0xff.zombie_buddy.Patch;

// @Patch(className = "zombie.iso.IsoMovingObject", methodName = "separate")
// public class Patch_separate {
//     @Patch.OnEnter
//     public static void onEnter(@Patch.This Object self) {
//         if (!ZBBetterFPS.g_OptimizeIsoMovingObject) return;
//         // separate() is very heavy, but without mid-method patching it's hard to optimize.
//     }
// }
