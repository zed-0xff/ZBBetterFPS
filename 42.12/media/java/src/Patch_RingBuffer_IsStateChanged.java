package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Accessor;
import me.zed_0xff.zombie_buddy.Patch;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.Styles.Style;
import java.lang.reflect.Field;

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
public class Patch_RingBuffer_IsStateChanged {

    public static final Class<?> RB_CLASS = Accessor.findClass("zombie.core.SpriteRenderer$RingBuffer");

    public static final Field f_currentRun             = Accessor.findField(RB_CLASS, "currentRun");
    public static final Field f_currentUseAttribArray  = Accessor.findField(RB_CLASS, "currentUseAttribArray");
    public static final Field f_currentTexture0        = Accessor.findField(RB_CLASS, "currentTexture0");
    public static final Field f_currentTexture1        = Accessor.findField(RB_CLASS, "currentTexture1");
    public static final Field f_currentTexture2        = Accessor.findField(RB_CLASS, "currentTexture2");
    public static final Field f_currentStyle           = Accessor.findField(RB_CLASS, "currentStyle");

    public static final boolean ALL_FIELDS_FOUND = f_currentRun != null && f_currentUseAttribArray != null
        && f_currentTexture0 != null && f_currentTexture1 != null
        && f_currentTexture2 != null && f_currentStyle != null;

    @Patch.OnExit
    @Patch.RuntimeType
    public static void onExit(@Patch.This Object self, 
                             Object drawObj, 
                             Object prevDrawObj, 
                             Object newStyleObj, 
                             Object newTexture0Obj, 
                             Object newTexture1Obj, 
                             Object newTexture2Obj, 
                             byte newUseAttribArray,
                             @Patch.Return(readOnly = false) boolean result) {
        
        // Only attempt to optimize if the original logic decided a state change is needed
        if (!result || !ZBBetterFPS.g_OptimizeSpriteBatching || !ALL_FIELDS_FOUND) {
            return;
        }

        try {
            TextureDraw draw = (TextureDraw) drawObj;
            TextureDraw prevDraw = (TextureDraw) prevDrawObj;
            Style newStyle = (Style) newStyleObj;
            Texture newTexture0 = (Texture) newTexture0Obj;
            Texture newTexture1 = (Texture) newTexture1Obj;
            Texture newTexture2 = (Texture) newTexture2Obj;

            // Cannot merge if there's no current batch to merge into
            if (Accessor.tryGet(self, f_currentRun, null) == null) return;

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
            Byte useAttrib = Accessor.tryGet(self, f_currentUseAttribArray, (byte) 0);
            if (useAttrib == null || newUseAttribArray != useAttrib.byteValue()) return;

            // 3. Compare Texture IDs (The core optimization: merge subtextures from same atlas)
            if (getTexID(newTexture0) != getTexID(Accessor.tryGet(self, f_currentTexture0, (Texture) null))) return;
            if (getTexID(newTexture1) != getTexID(Accessor.tryGet(self, f_currentTexture1, (Texture) null))) return;
            if (getTexID(newTexture2) != getTexID(Accessor.tryGet(self, f_currentTexture2, (Texture) null))) return;

            // 4. Check Style compatibility
            Style currentStyle = Accessor.tryGet(self, f_currentStyle, (Style) null);
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

    public static int getTexID(Texture t) {
        return t == null ? -1 : t.getID();
    }
}
