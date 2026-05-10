package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.Field;
import me.zed_0xff.zombie_buddy.Patch.FieldRW;
import me.zed_0xff.zombie_buddy.Patch.MemberHandle;
import me.zed_0xff.zombie_buddy.Patch.This;

import java.lang.invoke.VarHandle;

import org.lwjgl.opengl.GL20;
import zombie.core.SpriteRenderer;
import zombie.core.VBO.GLVertexBufferObject;

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
    public static int N_OK = 0, N_SKIP = 0, N_FAIL = 0;

    // to be able to construct and reference instances of private StateRun class
    @Patch.TypeAlias("zombie.core.SpriteRenderer$RingBuffer$StateRun")
    public static class StateRun {
        public StateRun(SpriteRenderer.RingBuffer ringBuffer) {
        }
    }

    // @MemberHandle(name = "VERTEX_SIZE", className = "zombie.core.SpriteRenderer", type = int.class)
    // public static VarHandle vh_VERTEX_SIZE;

    @Patch.OnEnter(skipOn = true)
    public static boolean create(
            @MemberHandle(name = "VERTEX_SIZE", className = "zombie.core.SpriteRenderer", type = int.class) final VarHandle vh_VERTEX_SIZE,
            @This SpriteRenderer.RingBuffer self,
            @FieldRW long                   bufferSize,
            @FieldRW long                   bufferSizeInVertices,
            @FieldRW GLVertexBufferObject[] ibo,
            @FieldRW long                   indexBufferSize,
            @FieldRW ShortBuffer[]          indices,
            @FieldRW ByteBuffer[]           indicesBytes,
            @FieldRW int                    numBuffers,
            @FieldRW StateRun[]             stateRun,
            @FieldRW GLVertexBufferObject[] vbo,
            @FieldRW FloatBuffer[]          vertices,
            @FieldRW ByteBuffer[]           verticesBytes
        ) {
        if (!ZBBetterFPS.g_OptimizeRingBuffer){
            N_SKIP++;
            return false;
        }

        try {
            final int VERTEX_SIZE = (int) vh_VERTEX_SIZE.get();
            System.out.println("[ZBBetterFPS] Patching SpriteRenderer.RingBuffer: VERTEX_SIZE = " + VERTEX_SIZE);

            for (int i = 0; i <= 4; i++) GL20.glEnableVertexAttribArray(i);

            long verticesCount = bufferSize / VERTEX_SIZE;

            bufferSize           = 1048576; // 1MB
            numBuffers           = Utils.isDebug() ? 256 : 128;
            bufferSizeInVertices = verticesCount;
            indexBufferSize      = verticesCount * 3;
            vertices             = new FloatBuffer[numBuffers];
            verticesBytes        = new ByteBuffer[numBuffers];
            indices              = new ShortBuffer[numBuffers];
            indicesBytes         = new ByteBuffer[numBuffers];

            // Expand StateRun pool to 20,000
            stateRun = new StateRun[20000];
            for (int i = 0; i < 20000; i++)
                stateRun[i] = new StateRun(self);

            // Initialize optimized VBOs
            vbo = new GLVertexBufferObject[numBuffers];
            ibo = new GLVertexBufferObject[numBuffers];
            for (int i = 0; i < numBuffers; i++) {
                vbo[i] = new GLVertexBufferObject(bufferSize, GLVertexBufferObject.funcs.GL_ARRAY_BUFFER(), GLVertexBufferObject.funcs.GL_STREAM_DRAW());
                vbo[i].create();

                ibo[i] = new GLVertexBufferObject(verticesCount * 3, GLVertexBufferObject.funcs.GL_ELEMENT_ARRAY_BUFFER(), GLVertexBufferObject.funcs.GL_STREAM_DRAW());
                ibo[i].create();
            }

            N_OK++;
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            N_FAIL++;
            return false; // Fallback to vanilla create()
        }
    }
}
