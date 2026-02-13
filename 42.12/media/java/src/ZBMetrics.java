// package me.zed_0xff.zb_better_fps;

// import me.zed_0xff.zombie_buddy.Patch;

// import java.util.concurrent.ConcurrentHashMap;
// import java.util.Map;
// import java.util.concurrent.atomic.AtomicLong;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Collections;
// import zombie.debug.DebugLog;

// public class ZBMetrics {
//     public static final Map<String, Metric> metrics = new ConcurrentHashMap<>();
//     public static long lastDumpTime = System.currentTimeMillis();

//     public static class Metric {
//         public final String name;
//         public final AtomicLong totalTimeNs = new AtomicLong(0);
//         public final AtomicLong count = new AtomicLong(0);

//         public Metric(String name) {
//             this.name = name;
//         }

//         public void add(long ns) {
//             totalTimeNs.addAndGet(ns);
//             count.incrementAndGet();
//         }

//         public void reset() {
//             totalTimeNs.set(0);
//             count.set(0);
//         }
//     }

//     public static void record(String name, long ns) {
//         if (!ZBBetterFPS.g_EnableMetrics) return;
//         metrics.computeIfAbsent(name, Metric::new).add(ns);
//     }

//     public static void update() {
//         if (!ZBBetterFPS.g_EnableMetrics) {
//             if (!metrics.isEmpty()) metrics.clear();
//             return;
//         }

//         long now = System.currentTimeMillis();
//         if (now - lastDumpTime > 5000) { // Dump every 5 seconds
//             dump();
//             lastDumpTime = now;
//         }
//     }

//     public static void dump() {
//         List<Metric> sorted = new ArrayList<>(metrics.values());
//         Collections.sort(sorted, (a, b) -> Long.compare(b.totalTimeNs.get(), a.totalTimeNs.get()));

//         DebugLog.General.println("=== ZB Better FPS Metrics (Last 5s) ===");
//         for (Metric m : sorted) {
//             long totalNs = m.totalTimeNs.get();
//             long count = m.count.get();
//             if (count == 0) continue;
            
//             double avgMs = (totalNs / (double) count) / 1_000_000.0;
//             double totalMs = totalNs / 1_000_000.0;
            
//             DebugLog.General.println(String.format("%-40s | Total: %10.2fms | Avg: %8.4fms | Count: %d", 
//                 m.name, totalMs, avgMs, count));
//             m.reset();
//         }
//         DebugLog.General.println("=======================================");
//     }

//     @Patch(className = "zombie.GameWindow", methodName = "logic")
//     public static class Patch_GameWindowLogic {
//         @Patch.OnEnter
//         public static void onEnter(@Patch.Local("startTime") long startTime) {
//             if (!ZBBetterFPS.g_EnableMetrics) return;
//             startTime = System.nanoTime();
//         }

//         @Patch.OnExit
//         public static void onExit(@Patch.Local("startTime") long startTime) {
//             if (startTime == 0) return;
//             ZBMetrics.record("GameWindow.logic", System.nanoTime() - startTime);
//             ZBMetrics.update();
//         }
//     }

//     @Patch(className = "zombie.iso.IsoCell", methodName = "update")
//     public static class Patch_IsoCellUpdate {
//         @Patch.OnEnter
//         public static void onEnter(@Patch.Local("startTime") long startTime) {
//             if (!ZBBetterFPS.g_EnableMetrics) return;
//             startTime = System.nanoTime();
//         }

//         @Patch.OnExit
//         public static void onExit(@Patch.Local("startTime") long startTime) {
//             if (startTime == 0) return;
//             ZBMetrics.record("IsoCell.update", System.nanoTime() - startTime);
//         }
//     }

//     @Patch(className = "zombie.Lua.LuaEventManager", methodName = "triggerEvent")
//     public static class Patch_LuaTriggerEvent {
//         @Patch.OnEnter
//         public static void onEnter(@Patch.Local("startTime") long startTime) {
//             if (!ZBBetterFPS.g_EnableMetrics) return;
//             startTime = System.nanoTime();
//         }

//         @Patch.OnExit
//         public static void onExit(@Patch.Local("startTime") long startTime, @Patch.Argument(0) String name) {
//             if (startTime == 0) return;
//             ZBMetrics.record("Lua Event: " + name, System.nanoTime() - startTime);
//         }
//     }

//     @Patch(className = "zombie.characters.IsoZombie", methodName = "update")
//     public static class Patch_IsoZombieUpdate {
//         @Patch.OnEnter
//         public static void onEnter(@Patch.Local("startTime") long startTime) {
//             if (!ZBBetterFPS.g_EnableMetrics) return;
//             startTime = System.nanoTime();
//         }

//         @Patch.OnExit
//         public static void onExit(@Patch.Local("startTime") long startTime) {
//             if (startTime == 0) return;
//             ZBMetrics.record("IsoZombie.update", System.nanoTime() - startTime);
//         }
//     }

//     @Patch(className = "zombie.inventory.InventoryItem", methodName = "update")
//     public static class Patch_InventoryItemUpdate {
//         @Patch.OnEnter
//         public static void onEnter(@Patch.Local("startTime") long startTime) {
//             if (!ZBBetterFPS.g_EnableMetrics) return;
//             startTime = System.nanoTime();
//         }

//         @Patch.OnExit
//         public static void onExit(@Patch.Local("startTime") long startTime) {
//             if (startTime == 0) return;
//             ZBMetrics.record("InventoryItem.update", System.nanoTime() - startTime);
//         }
//     }

//     @Patch(className = "zombie.core.SpriteRenderer", methodName = "postRender")
//     public static class Patch_SpriteRendererPostRender {
//         @Patch.OnEnter
//         public static void onEnter(@Patch.Local("startTime") long startTime) {
//             if (!ZBBetterFPS.g_EnableMetrics) return;
//             startTime = System.nanoTime();
//         }

//         @Patch.OnExit
//         public static void onExit(@Patch.Local("startTime") long startTime) {
//             if (startTime == 0) return;
//             ZBMetrics.record("SpriteRenderer.postRender", System.nanoTime() - startTime);
//         }
//     }

//     @Patch(className = "zombie.iso.IsoMovingObject", methodName = "separate")
//     public static class Patch_IsoMovingObjectSeparate {
//         @Patch.OnEnter
//         public static void onEnter(@Patch.Local("startTime") long startTime) {
//             if (!ZBBetterFPS.g_EnableMetrics) return;
//             startTime = System.nanoTime();
//         }

//         @Patch.OnExit
//         public static void onExit(@Patch.Local("startTime") long startTime) {
//             if (startTime == 0) return;
//             ZBMetrics.record("IsoMovingObject.separate", System.nanoTime() - startTime);
//         }
//     }

//     @Patch(className = "zombie.core.physics.WorldSimulation", methodName = "updatePhysic")
//     public static class Patch_PhysicsUpdate {
//         @Patch.OnEnter
//         public static void onEnter(@Patch.Local("startTime") long startTime) {
//             if (!ZBBetterFPS.g_EnableMetrics) return;
//             startTime = System.nanoTime();
//         }

//         @Patch.OnExit
//         public static void onExit(@Patch.Local("startTime") long startTime) {
//             if (startTime == 0) return;
//             ZBMetrics.record("Physics (Bullet) Update", System.nanoTime() - startTime);
//         }
//     }

//     @Patch(className = "zombie.core.opengl.RenderThread", methodName = "renderStep")
//     public static class Patch_RenderThreadStep {
//         @Patch.OnEnter
//         public static void onEnter(@Patch.Local("startTime") long startTime) {
//             if (!ZBBetterFPS.g_EnableMetrics) return;
//             startTime = System.nanoTime();
//         }

//         @Patch.OnExit
//         public static void onExit(@Patch.Local("startTime") long startTime) {
//             if (startTime == 0) return;
//             ZBMetrics.record("RenderThread.renderStep", System.nanoTime() - startTime);
//         }
//     }

//     @Patch(className = "org.lwjglx.opengl.Display", methodName = "update")
//     public static class Patch_DisplayUpdate {
//         @Patch.OnEnter
//         public static void onEnter(@Patch.Local("startTime") long startTime) {
//             if (!ZBBetterFPS.g_EnableMetrics) return;
//             startTime = System.nanoTime();
//         }

//         @Patch.OnExit
//         public static void onExit(@Patch.Local("startTime") long startTime) {
//             if (startTime == 0) return;
//             ZBMetrics.record("Display.update", System.nanoTime() - startTime);
//         }
//     }

//     @Patch(className = "zombie.core.SpriteRenderer$RingBuffer", methodName = "render")
//     public static class Patch_RingBufferRender {
//         @Patch.OnEnter
//         public static void onEnter(@Patch.Local("startTime") long startTime) {
//             if (!ZBBetterFPS.g_EnableMetrics) return;
//             startTime = System.nanoTime();
//         }

//         @Patch.OnExit
//         public static void onExit(@Patch.Local("startTime") long startTime, @Patch.This Object self) {
//             if (startTime == 0) return;
//             long duration = System.nanoTime() - startTime;
//             ZBMetrics.record("RingBuffer.render", duration);
            
//             try {
//                 java.lang.reflect.Field numRunsField = self.getClass().getDeclaredField("numRuns");
//                 numRunsField.setAccessible(true);
//                 int numRuns = numRunsField.getInt(self);
//                 ZBMetrics.record("RingBuffer batches per call", numRuns);
//             } catch (Exception e) {}
//         }
//     }

//     @Patch(className = "zombie.core.SpriteRenderer", methodName = "buildDrawBuffer")
//     public static class Patch_SpriteRendererBuildDrawBuffer {
//         @Patch.OnEnter
//         public static void onEnter(@Patch.Local("startTime") long startTime) {
//             if (!ZBBetterFPS.g_EnableMetrics) return;
//             startTime = System.nanoTime();
//         }

//         @Patch.OnExit
//         public static void onExit(@Patch.Local("startTime") long startTime) {
//             if (startTime == 0) return;
//             ZBMetrics.record("SpriteRenderer.buildDrawBuffer", System.nanoTime() - startTime);
//         }
//     }

//     @Patch(className = "zombie.core.skinnedmodel.model.VertexBufferObject", methodName = "setModelViewProjection")
//     public static class Patch_VBOSetModelViewProjection {
//         @Patch.OnEnter
//         public static void onEnter(@Patch.Local("startTime") long startTime) {
//             if (!ZBBetterFPS.g_EnableMetrics) return;
//             startTime = System.nanoTime();
//         }

//         @Patch.OnExit
//         public static void onExit(@Patch.Local("startTime") long startTime) {
//             if (startTime == 0) return;
//             ZBMetrics.record("VBO.setModelViewProjection", System.nanoTime() - startTime);
//         }
//     }
// }
