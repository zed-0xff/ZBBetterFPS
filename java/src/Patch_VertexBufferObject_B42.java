package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.AdapterFactory;
import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.Field;
import me.zed_0xff.zombie_buddy.Patch.Method;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import org.joml.Matrix4f;
import zombie.core.Core;
import zombie.core.opengl.ShaderProgram;

/**
 * This patch optimizes the constant matrix updates for 3D shaders.
 * 
 * Vanilla behavior: 
 * - For every 3D object draw, it performs a String-based HashMap lookup for "ModelViewProjection".
 * - It then calls glUniformMatrix4fv regardless of whether the matrix changed.
 * - This can happen 100,000+ times per second.
 * 
 * Optimizations:
 * 1. Zero-Lookup Linking: Links the uniform address once per shader ID and caches it.
 * 2. High-Performance Matrix Comparison: Uses direct float-by-float comparison instead of 
 *    the standard Matrix4f.equals() to avoid extra function call overhead.
 * 3. State Caching: Skips the GPU update if the projection/view matrices haven't changed.
 */
@Patch(className = "zombie.core.skinnedmodel.model.VertexBufferObject", methodName = "setModelViewProjection")
public class Patch_VertexBufferObject_B42 {
    public static int N_OK = 0, N_SKIP = 0, N_FAIL = 0;

    public static class ShaderState {
        public int uLoc = -2;
        public Matrix4f spMV;
        public Matrix4f spPRJ;
    }

    public static final ShaderState[] shaderCache = new ShaderState[1024];

    // @Patch.Adapter(zombie.core.opengl.ShaderProgram.class)
    public interface ShaderProgramAdapter extends AdapterFactory.ClassAdapter<ShaderProgram> {
        @Field({"modelView", "ModelView"})   RO<Matrix4f> modelView();
        @Field({"projection", "Projection"}) RO<Matrix4f> projection();

        @Method
        public void setTransformMatrix(int uLoc, Matrix4f matrix);
    }

    @Patch.OnEnter(skipOn = true)
    public static boolean setModelViewProjection(@Patch.Argument(0) ShaderProgram shaderProgram) {
        if (!ZBBetterFPS.g_Optimize3DModels){
            N_SKIP++;
            return false;
        }

        ShaderProgramAdapter adp = AdapterFactory.create(shaderProgram, ShaderProgramAdapter.class);
        if (adp == null){
            N_FAIL++;
            return false;
        }

        try {
            if (shaderProgram == null || !shaderProgram.isCompiled()) {
                N_FAIL++;
                return true;
            }

            int shaderId = shaderProgram.getShaderID();
            if (shaderId < 0 || shaderId >= shaderCache.length) return false;

            ShaderState state = shaderCache[shaderId];
            if (state == null) {
                state = new ShaderState();
                // Get uniform once and cache it
                ShaderProgram.Uniform u = shaderProgram.getUniform("ModelViewProjection", 35676, false);
                state.uLoc  = (u == null) ? -1 : u.loc;
                state.spMV  = adp.modelView().get();
                state.spPRJ = adp.projection().get();
                shaderCache[shaderId] = state;
            }

            if (state.uLoc == -1) return true;
            if (state.spMV == null || state.spPRJ == null) return true;

            Matrix4f PRJ;
            Matrix4f MV;
            if (Core.getInstance().modelViewMatrixStack.isEmpty()) {
                MV = L_Patch.getIdentityMatrix();
                PRJ = L_Patch.getIdentityMatrix();
            } else {
                PRJ = Core.getInstance().projectionMatrixStack.peek();
                MV = Core.getInstance().modelViewMatrixStack.peek();
            }

            // High-speed comparison check
            if (matrixEquals(MV, state.spMV) && matrixEquals(PRJ, state.spPRJ)) {
                return true; // Matrix is already on the GPU, skip update
            }

            state.spMV.set(MV);
            state.spPRJ.set(PRJ);
            
            Matrix4f tmp = L_Patch.getTmpMatrix();
            tmp.set(PRJ);
            tmp.mul(MV);

            // Directly call the internal method using the linked loc
            adp.setTransformMatrix(state.uLoc, tmp);

            N_OK++;
            return true;
        } catch (Exception e) {
            N_FAIL++;
            return false;
        }
    }

    /**
     * Direct float-by-float comparison for maximum performance in hot loops.
     */
    public static boolean matrixEquals(Matrix4f a, Matrix4f b) {
        if (a == b) return true;
        return a.m00() == b.m00() && a.m01() == b.m01() && a.m02() == b.m02() && a.m03() == b.m03() &&
               a.m10() == b.m10() && a.m11() == b.m11() && a.m12() == b.m12() && a.m13() == b.m13() &&
               a.m20() == b.m20() && a.m21() == b.m21() && a.m22() == b.m22() && a.m23() == b.m23() &&
               a.m30() == b.m30() && a.m31() == b.m31() && a.m32() == b.m32() && a.m33() == b.m33();
    }

    public static class L_Patch {
        public static final ThreadLocal<Matrix4f> tmpMatrix = ThreadLocal.withInitial(Matrix4f::new);
        public static final ThreadLocal<Matrix4f> identityMatrix = ThreadLocal.withInitial(() -> new Matrix4f().identity());
        public static Matrix4f getTmpMatrix() { return tmpMatrix.get(); }
        public static Matrix4f getIdentityMatrix() { return identityMatrix.get(); }
    }
}
