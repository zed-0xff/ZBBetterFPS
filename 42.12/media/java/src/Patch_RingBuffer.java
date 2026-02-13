package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Patch;
import org.lwjgl.opengl.GL20;
import zombie.core.Core;
import zombie.core.VBO.GLVertexBufferObject;
import java.lang.reflect.*;
import java.nio.*;

/**
 * This patch optimizes the SpriteRenderer's RingBuffer by increasing its capacity.
 * 
 * Vanilla behavior:
 * - Uses small 64KB/256KB buffers which flush to the GPU very frequently.
 * - Limited to 5,000 "State Runs" (batches) per frame.
 * 
 * Optimizations:
 * 1. Larger Buffers: Increases batch size to 1MB. This allows many more sprites to be
 *    packed into a single GPU upload, significantly reducing CPU-to-GPU overhead.
 * 2. Expanded Batch Pool: Increases the StateRun array to 20,000 entries. This prevents
 *    buffer overruns in extremely dense scenes (many zombies/items).
 * 
 * Note: Requires game restart to apply.
 */
@Patch(className = "zombie.core.SpriteRenderer$RingBuffer", methodName = "create")
public class Patch_RingBuffer {

    @Patch.OnEnter(skipOn = true)
    public static boolean create(@Patch.This Object self) {
        if (!ZBBetterFPS.g_OptimizeRingBuffer) return false;

        try {
            for (int i = 0; i <= 4; i++) GL20.glEnableVertexAttribArray(i);

            long bufferSize = 1048576; // 1MB
            int numBuffers = Core.bDebug ? 256 : 128;
            long verticesCount = bufferSize / 36;

            setFields(self, 
                "bufferSize", bufferSize,
                "numBuffers", numBuffers,
                "bufferSizeInVertices", verticesCount,
                "indexBufferSize", verticesCount * 3
            );

            setFields(self,
                "vertices", new FloatBuffer[numBuffers],
                "verticesBytes", new ByteBuffer[numBuffers],
                "indices", new ShortBuffer[numBuffers],
                "indicesBytes", new ByteBuffer[numBuffers]
            );

            // Expand StateRun pool to 20,000
            Class<?> rbClass = self.getClass();
            Class<?> srClass = Class.forName(rbClass.getName() + "$StateRun");
            Constructor<?> ctor = srClass.getDeclaredConstructor(rbClass);
            ctor.setAccessible(true);
            
            Object stateRuns = Array.newInstance(srClass, 20000);
            for (int n = 0; n < 20000; n++) Array.set(stateRuns, n, ctor.newInstance(self));
            setField(self, "stateRun", stateRuns);

            // Initialize optimized VBOs
            GLVertexBufferObject[] vbos = new GLVertexBufferObject[numBuffers];
            GLVertexBufferObject[] ibos = new GLVertexBufferObject[numBuffers];
            for (int i = 0; i < numBuffers; i++) {
                vbos[i] = new GLVertexBufferObject(bufferSize, GLVertexBufferObject.funcs.GL_ARRAY_BUFFER(), GLVertexBufferObject.funcs.GL_STREAM_DRAW());
                vbos[i].create();
                ibos[i] = new GLVertexBufferObject(verticesCount * 3, GLVertexBufferObject.funcs.GL_ELEMENT_ARRAY_BUFFER(), GLVertexBufferObject.funcs.GL_STREAM_DRAW());
                ibos[i].create();
            }
            setFields(self, "vbo", vbos, "ibo", ibos);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Fallback to vanilla create()
        }
    }

    public static void setField(Object obj, String name, Object val) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, val);
    }

    public static void setFields(Object obj, Object... pairs) throws Exception {
        for (int i = 0; i < pairs.length; i += 2) setField(obj, (String)pairs[i], pairs[i+1]);
    }
}
