package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Accessor;
import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.Field;
import me.zed_0xff.zombie_buddy.Patch.FieldRW;

import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.Styles.Style;

/**
 * This patch optimizes the Sprite Batching pipeline by merging compatible draw calls.
 * 
 * Vanilla behavior: 
 * - Every time a sprite uses a different Java Texture object, a new batch starts.
 * - Every time a 3D model (zombie/item) is drawn, a new batch starts.
 * 
 * Optimizations:
 * 1. Subtexture Merging: Compares OpenGL Texture IDs instead of Java references.
 *    Sprites using different parts of the same atlas are now merged into one draw call.
 * 2. Model Batching: Allows consecutive 3D model draws to stay in the same batch.
 * 3. OnExit Rescue: Only intervenes if vanilla logic decided to break the batch,
 *    then "rescues" it if the underlying GPU state is actually identical.
 */
public class Patch_RingBuffer_IsStateChanged {
    public static final boolean ALL_FIELDS_FOUND = true;

    @Patch(className = "zombie.core.SpriteRenderer$RingBuffer", methodName = "isStateChanged")
    public static class Patch_B41 {
        @Patch.OnExit
        public static void onExit(
                @Field final byte currentUseAttribArray,
                @Field final Object currentRun,
                @Field final Style currentStyle,
                @Field final Texture currentTexture0,
                @Field final Texture currentTexture1,

                TextureDraw draw,
                TextureDraw prevDraw,
                Style newStyle,
                Texture newTexture0,
                Texture newTexture1,
                byte newUseAttribArray,

                @Patch.Return(readOnly = false) boolean result
        ) {
            result = unifiedPatch(
                    currentUseAttribArray, currentRun, currentStyle, currentTexture0, currentTexture1, null,
                    draw, prevDraw, newStyle, newTexture0, newTexture1, null, newUseAttribArray,
                    result
            );
        }
    }

    @Patch(className = "zombie.core.SpriteRenderer$RingBuffer", methodName = "isStateChanged")
    public static class Patch_B42 {
        @Patch.OnExit
        public static void onExit(
                @Field final byte currentUseAttribArray,
                @Field final Object currentRun,
                @Field final Style currentStyle,
                @Field final Texture currentTexture0,
                @Field final Texture currentTexture1,
                @Field final Texture currentTexture2, // added in B42

                TextureDraw draw,
                TextureDraw prevDraw,
                Style newStyle,
                Texture newTexture0,
                Texture newTexture1,
                Texture newTexture2,                  // added in B42
                byte newUseAttribArray,

                @Patch.Return(readOnly = false) boolean result
        ) {
            result = unifiedPatch(
                    currentUseAttribArray, currentRun, currentStyle, currentTexture0, currentTexture1, currentTexture2,
                    draw, prevDraw, newStyle, newTexture0, newTexture1, newTexture2, newUseAttribArray,
                    result
            );
        }
    }

    public static boolean unifiedPatch(
            final byte currentUseAttribArray,
            final Object currentRun,
            final Style currentStyle,
            final Texture currentTexture0,
            final Texture currentTexture1,
            final Texture currentTexture2,

            final TextureDraw draw,
            final TextureDraw prevDraw,
            final Style newStyle,
            final Texture newTexture0,
            final Texture newTexture1,
            final Texture newTexture2,
            final byte newUseAttribArray,

            final boolean result
    ) {
        // Only attempt to optimize if the original logic decided a state change is needed
        if (!result || !ZBBetterFPS.g_OptimizeSpriteBatching || !ALL_FIELDS_FOUND) {
            return result;
        }

        try {
            // Cannot merge if there's no current batch to merge into
            if (currentRun == null)
                return result;

            // 1. Check if merging this draw type is safe
            if (prevDraw != null) {
                // glDraw and other types (like models) use different rendering paths, don't merge them.
                if (draw.type == TextureDraw.Type.glDraw && prevDraw.type != TextureDraw.Type.glDraw) return result;
                if (draw.type != TextureDraw.Type.glDraw && prevDraw.type == TextureDraw.Type.glDraw) return result;
                
                // If both are models, we can always merge because they handle their own internal state.
                if (draw.type == TextureDraw.Type.DrawModel && prevDraw.type == TextureDraw.Type.DrawModel) {
                    return false;
                }
            }

            // 2. Check if technical state matches (vertex attributes)
            if (newUseAttribArray != currentUseAttribArray) return result;

            // 3. Compare Texture IDs (The core optimization: merge subtextures from same atlas)
            if (!isSameTexture(newTexture0, currentTexture0)) return result;
            if (!isSameTexture(newTexture1, currentTexture1)) return result;
            if (!isSameTexture(newTexture2, currentTexture2)) return result;

            // 4. Check Style compatibility
            if (newStyle != currentStyle) {
                if (currentStyle == null || newStyle == null || newStyle.getStyleID() != currentStyle.getStyleID()) {
                    return result; // Styles are fundamentally different
                }
            }

            // If we reached here, the actual OpenGL state is the same as the current batch.
            // We "rescue" the batch and tell the engine NO state change is needed.
            return false;

        } catch (Throwable t) {
            return result;
        }
    }

    public static boolean isSameTexture(Texture t1, Texture t2) {
        return (
                (t1 == null && t2 == null) ||
                (t1 == t2) ||
                (t1 != null && t2 != null && t1.getID() == t2.getID())
               );
    }
}
