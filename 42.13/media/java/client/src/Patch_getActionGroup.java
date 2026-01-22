// package me.zed_0xff.zb_better_fps;

// import me.zed_0xff.zombie_buddy.Patch;
// import zombie.characters.action.ActionGroup;
// import java.util.HashMap;
// import java.util.Map;

// @Patch(className = "zombie.characters.action.ActionGroup", methodName = "getActionGroup")
// public class Patch_getActionGroup {
//     public static final Map<String, Object> cache = new HashMap<>();

//     @Patch.RuntimeType
//     @Patch.OnExit
//     public static void onExit(@Patch.Argument(0) String name, @Patch.Return(readOnly = false) Object returnValue) {
//         if (!ZBBetterFPS.g_OptimizeIsoZombie) return;
//         if (returnValue == null) return;
        
//         // Simple cache to avoid toLowerCase() and Map.get() on the larger internal map
//         // although the gain might be small if name is already lowercased by the caller.
//         cache.putIfAbsent(name, returnValue);
//     }
// }
