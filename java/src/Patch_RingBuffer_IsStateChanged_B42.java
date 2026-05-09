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
@Patch(className = "zombie.core.SpriteRenderer$RingBuffer", methodName = "isStateChanged")
public class Patch_RingBuffer_IsStateChanged_B42 {
    public static final boolean ALL_FIELDS_FOUND = true;

    @Patch.OnExit
    public static void onExit(
            @Field byte currentUseAttribArray,
            @Field Object currentRun,
            @Field Style currentStyle,
            @Field Texture currentTexture0,
            @Field Texture currentTexture1,
            @Field Texture currentTexture2,

            @Patch.This Object self, 
            TextureDraw draw,
            TextureDraw prevDraw,
            Style newStyle,
            Texture newTexture0,
            Texture newTexture1,
            Texture newTexture2,
            byte newUseAttribArray,
            @Patch.Return(readOnly = false) boolean result
        ) {
        
        // Only attempt to optimize if the original logic decided a state change is needed
        if (!result || !ZBBetterFPS.g_OptimizeSpriteBatching || !ALL_FIELDS_FOUND) {
            return;
        }

        try {
            // Cannot merge if there's no current batch to merge into
            if (currentRun == null) return;

            // 1. Check if merging this draw type is safe
            if (prevDraw != null) {
                // glDraw and other types (like models) use different rendering paths, don't merge them.
                if (draw.type == TextureDraw.Type.glDraw && prevDraw.type != TextureDraw.Type.glDraw) return;
                if (draw.type != TextureDraw.Type.glDraw && prevDraw.type == TextureDraw.Type.glDraw) return;
                
                // If both are models, we can always merge because they handle their own internal state.
                if (draw.type == TextureDraw.Type.DrawModel && prevDraw.type == TextureDraw.Type.DrawModel) {
                    result = false;
                    return;
                }
            }

            // 2. Check if technical state matches (vertex attributes)
            if (newUseAttribArray != currentUseAttribArray) return;

            // 3. Compare Texture IDs (The core optimization: merge subtextures from same atlas)
            if (!isSameTexture(newTexture0, currentTexture0)) return;
            if (!isSameTexture(newTexture1, currentTexture1)) return;
            if (!isSameTexture(newTexture2, currentTexture2)) return;

            // 4. Check Style compatibility
            if (newStyle != currentStyle) {
                if (currentStyle == null || newStyle.getStyleID() != currentStyle.getStyleID()) {
                    return; // Styles are fundamentally different
                }
            }

            // If we reached here, the actual OpenGL state is the same as the current batch.
            // We "rescue" the batch and tell the engine NO state change is needed.
            result = false;

        } catch (Exception e) {
            // result stays true (vanilla behavior), which is safe.
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
