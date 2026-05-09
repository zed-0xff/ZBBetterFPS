package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Accessor;
import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.FieldRW;
import me.zed_0xff.zombie_buddy.Patch.StaticFieldAlias;
import me.zed_0xff.zombie_buddy.Patch.This;

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
    public static final boolean ALL_FIELDS_FOUND = true;

    @Patch.TypeAlias("zombie.core.SpriteRenderer$RingBuffer$StateRun")
    static class StateRun {
        StateRun(SpriteRenderer.RingBuffer ringBuffer) {
        }
    }

    @Patch.StaticFieldAlias(className = "zombie.core.SpriteRenderer")
    static int VERTEX_SIZE;

    @Patch.OnEnter(skipOn = true)
    public static boolean create(
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
        if (!ZBBetterFPS.g_OptimizeRingBuffer) return false;
        if (!ALL_FIELDS_FOUND) return false;

        try {
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

            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false; // Fallback to vanilla create()
        }
    }
}
