package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Accessor;
import me.zed_0xff.zombie_buddy.Patch;

import org.lwjgl.opengl.GL20;
import zombie.core.SpriteRenderer;
import zombie.core.VBO.GLVertexBufferObject;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

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

    public static final Class<?> RB_CLASS = Accessor.findClass("zombie.core.SpriteRenderer$RingBuffer");

    public static final Field f_bufferSize             = Accessor.findField(RB_CLASS, "bufferSize");
    public static final Field f_numBuffers             = Accessor.findField(RB_CLASS, "numBuffers");
    public static final Field f_bufferSizeInVertices   = Accessor.findField(RB_CLASS, "bufferSizeInVertices");
    public static final Field f_indexBufferSize        = Accessor.findField(RB_CLASS, "indexBufferSize");
    public static final Field f_vertices               = Accessor.findField(RB_CLASS, "vertices");
    public static final Field f_verticesBytes          = Accessor.findField(RB_CLASS, "verticesBytes");
    public static final Field f_indices                = Accessor.findField(RB_CLASS, "indices");
    public static final Field f_indicesBytes           = Accessor.findField(RB_CLASS, "indicesBytes");
    public static final Field f_stateRun               = Accessor.findField(RB_CLASS, "stateRun");
    public static final Field f_vbo                    = Accessor.findField(RB_CLASS, "vbo");
    public static final Field f_ibo                    = Accessor.findField(RB_CLASS, "ibo");

    public static final int VERTEX_SIZE = Accessor.tryGet(SpriteRenderer.class, "VERTEX_SIZE", -1);

    public static final boolean ALL_FIELDS_FOUND =
        f_bufferSize      != null && f_numBuffers   != null && f_bufferSizeInVertices != null &&
        f_indexBufferSize != null && f_vertices     != null && f_verticesBytes        != null &&
        f_indices         != null && f_indicesBytes != null && f_stateRun             != null &&
        f_vbo             != null && f_ibo          != null &&
        VERTEX_SIZE       != -1;

    @Patch.OnEnter(skipOn = true)
    public static boolean create(@Patch.This Object self) {
        if (!ZBBetterFPS.g_OptimizeRingBuffer) return false;
        if (!ALL_FIELDS_FOUND) return false;

        try {
            for (int i = 0; i <= 4; i++) GL20.glEnableVertexAttribArray(i);

            long bufferSize = 1048576; // 1MB
            int numBuffers = Utils.isDebug() ? 256 : 128;
            long verticesCount = bufferSize / VERTEX_SIZE;

            setFields(self,
                f_bufferSize, bufferSize,
                f_numBuffers, numBuffers,
                f_bufferSizeInVertices, verticesCount,
                f_indexBufferSize, verticesCount * 3,
                f_vertices, new FloatBuffer[numBuffers],
                f_verticesBytes, new ByteBuffer[numBuffers],
                f_indices, new ShortBuffer[numBuffers],
                f_indicesBytes, new ByteBuffer[numBuffers]
            );

            // Expand StateRun pool to 20,000
            Class<?> rbClass = self.getClass();
            Class<?> srClass = Class.forName(rbClass.getName() + "$StateRun");
            Constructor<?> ctor = srClass.getDeclaredConstructor(rbClass);
            ctor.setAccessible(true);

            Object stateRuns = Array.newInstance(srClass, 20000);
            for (int n = 0; n < 20000; n++) Array.set(stateRuns, n, ctor.newInstance(self));
            if (!Accessor.trySet(self, f_stateRun, stateRuns)) throw new RuntimeException("set stateRun");

            // Initialize optimized VBOs
            GLVertexBufferObject[] vbos = new GLVertexBufferObject[numBuffers];
            GLVertexBufferObject[] ibos = new GLVertexBufferObject[numBuffers];
            for (int i = 0; i < numBuffers; i++) {
                vbos[i] = new GLVertexBufferObject(bufferSize, GLVertexBufferObject.funcs.GL_ARRAY_BUFFER(), GLVertexBufferObject.funcs.GL_STREAM_DRAW());
                vbos[i].create();
                ibos[i] = new GLVertexBufferObject(verticesCount * 3, GLVertexBufferObject.funcs.GL_ELEMENT_ARRAY_BUFFER(), GLVertexBufferObject.funcs.GL_STREAM_DRAW());
                ibos[i].create();
            }
            setFields(self, f_vbo, vbos, f_ibo, ibos);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Fallback to vanilla create()
        }
    }

    public static void setFields(Object obj, Object... pairs) throws Exception {
        for (int i = 0; i < pairs.length; i += 2) {
            if (!Accessor.trySet(obj, (Field) pairs[i], pairs[i + 1])) {
                throw new RuntimeException("set failed");
            }
        }
    }
}
