package me.zed_0xff.zb_better_fps;

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

    public static Field currentRunField;
    public static Field currentUseAttribArrayField;
    public static Field currentTexture0Field;
    public static Field currentTexture1Field;
    public static Field currentTexture2Field;
    public static Field currentStyleField;
    public static boolean reflectionInitialized = false;

    static {
        try {
            Class<?> rbClass = Class.forName("zombie.core.SpriteRenderer$RingBuffer");
            currentRunField = rbClass.getDeclaredField("currentRun");
            currentRunField.setAccessible(true);
            currentUseAttribArrayField = rbClass.getDeclaredField("currentUseAttribArray");
            currentUseAttribArrayField.setAccessible(true);
            currentTexture0Field = rbClass.getDeclaredField("currentTexture0");
            currentTexture0Field.setAccessible(true);
            currentTexture1Field = rbClass.getDeclaredField("currentTexture1");
            currentTexture1Field.setAccessible(true);
            currentTexture2Field = rbClass.getDeclaredField("currentTexture2");
            currentTexture2Field.setAccessible(true);
            currentStyleField = rbClass.getDeclaredField("currentStyle");
            currentStyleField.setAccessible(true);
            reflectionInitialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
        if (!result || !ZBBetterFPS.g_OptimizeSpriteBatching || !reflectionInitialized) {
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
            if (currentRunField.get(self) == null) return;

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
            if (newUseAttribArray != currentUseAttribArrayField.getByte(self)) return;

            // 3. Compare Texture IDs (The core optimization: merge subtextures from same atlas)
            if (getTexID(newTexture0) != getTexID((Texture) currentTexture0Field.get(self))) return;
            if (getTexID(newTexture1) != getTexID((Texture) currentTexture1Field.get(self))) return;
            if (getTexID(newTexture2) != getTexID((Texture) currentTexture2Field.get(self))) return;

            // 4. Check Style compatibility
            Style currentStyle = (Style) currentStyleField.get(self);
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
