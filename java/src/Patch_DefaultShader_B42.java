package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.DefaultShader;
import org.lwjgl.opengl.GL20;

/**
 * This patch optimizes `DefaultShader.setChunkDepth` to reduce redundant GPU uniform updates.
 *
 * Vanilla behavior:
 * - Calls `getProgram().setValue("chunkDepth", depth)` which involves a `HashMap` lookup by string.
 * - Always sends the `depth` value to the GPU, even if it hasn't changed.
 *
 * Optimizations:
 * 1. Uniform Location Caching: The `chunkDepthLoc` (uniform location) is looked up once and cached.
 *    - Impact: Replaces slow `HashMap` lookups with a direct integer access.
 * 2. Value Caching: The `cachedChunkDepth` stores the last value sent to the GPU.
 *    - Impact: Prevents redundant `GL20.glUniform1f` calls if the `depth` value has not changed,
 *      reducing CPU-GPU overhead, especially when rendering many tiles in the same chunk.
 * 3. Skip Original Method: The patch returns `true` from `onEnter` to skip the original, less efficient method
 *    if the optimization is enabled and the uniform update has been handled or deemed unnecessary.
 */
@Patch(className = "zombie.core.DefaultShader", methodName = "setChunkDepth")
public class Patch_DefaultShader_B42 {
    public static final boolean ALL_FIELDS_FOUND = true; // for uniformity

    public static int chunkDepthLoc = -2; // -2 indicates not yet initialized, -1 indicates not found
    public static float cachedChunkDepth = Float.NaN; // Stores the last depth value sent to GPU

    @Patch.OnEnter(skipOn = true)
    public static boolean setChunkDepth(@Patch.This Object selfObj, @Patch.Argument(0) float depth) {
        if (!ZBBetterFPS.g_OptimizeDefaultShader) return false; // Run original method if optimization is disabled

        try {
            // Initialize uniform location if not already done
            if (chunkDepthLoc == -2) {
                DefaultShader self = (DefaultShader) selfObj;
                chunkDepthLoc = GL20.glGetUniformLocation(self.getProgram().getShaderID(), "chunkDepth");
            }

            // If uniform location is valid, apply optimization
            if (chunkDepthLoc != -1) {
                if (cachedChunkDepth != depth) {
                    GL20.glUniform1f(chunkDepthLoc, depth);
                    cachedChunkDepth = depth;
                }
                return true; // Skip original code
            }
            return false; // Uniform not found or other issue, run original method as fallback
        } catch (Exception e) {
            e.printStackTrace();
            return false; // On error, run original method as fallback
        }
    }
}
