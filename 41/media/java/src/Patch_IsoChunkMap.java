package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Accessor;
import me.zed_0xff.zombie_buddy.Patch;

import java.lang.reflect.Field;

import zombie.iso.IsoChunkMap;

@Patch(className = "zombie.iso.IsoChunkMap", methodName = "CalcChunkWidth")
public class Patch_IsoChunkMap {
    public static final Field f_ChunksPerWidth    = Accessor.findField(IsoChunkMap.class, "ChunksPerWidth",    "CHUNKS_PER_WIDTH");
    public static final Field f_ChunkWidthInTiles = Accessor.findField(IsoChunkMap.class, "ChunkWidthInTiles", "chunkWidthInTiles");
    public static final Field f_ChunkGridWidth    = Accessor.findField(IsoChunkMap.class, "ChunkGridWidth",    "chunkGridWidth");

    public static int getChunksPerWidth() {
        return Accessor.tryGet(null, f_ChunksPerWidth, -1);
    }

    @Patch.OnExit
    public static void exit() {
        int chunksPerWidth = Accessor.tryGet(null, f_ChunksPerWidth, -1);
        int chunkGridWidth = Accessor.tryGet(null, f_ChunkGridWidth, -1);

        if (ZBBetterFPS.g_MaxRenderDistance == 0) {
            System.out.println("[ZBBetterFPS] using default render distance " + chunkGridWidth);
            return;
        }

        int chunkWidthInTiles = Accessor.tryGet(null, f_ChunkWidthInTiles, -1);
        if (chunksPerWidth == -1 || chunkGridWidth == -1 || chunkWidthInTiles == -1) {
            System.err.println("[ZBBetterFPS] Failed to get field values, skipping patch");
            System.err.println("[ZBBetterFPS] ChunksPerWidth = " + chunksPerWidth + ", ChunkGridWidth = " + chunkGridWidth + ", ChunkWidthInTiles = " + chunkWidthInTiles);
            return;
        }

        if (chunkWidthInTiles != chunkGridWidth * chunksPerWidth) {
            System.err.println("[ZBBetterFPS] chunkWidthInTiles is not equal to chunkGridWidth * chunksPerWidth, skipping patch");
            System.err.println("[ZBBetterFPS] ChunksPerWidth = " + chunksPerWidth + ", ChunkGridWidth = " + chunkGridWidth + ", ChunkWidthInTiles = " + chunkWidthInTiles);
            return;
        }

        if (!Accessor.trySet(null, f_ChunkGridWidth, ZBBetterFPS.g_MaxRenderDistance)) {
            System.err.println("[ZBBetterFPS] Failed to set ChunkGridWidth");
            return;
        }

        if (!Accessor.trySet(null, f_ChunkWidthInTiles, ZBBetterFPS.g_MaxRenderDistance * chunksPerWidth)) {
            System.err.println("[ZBBetterFPS] Failed to set ChunkWidthInTiles");
            return;
        }

        System.out.println("[ZBBetterFPS] Done: ChunkGridWidth = " + Accessor.tryGet(null, f_ChunkGridWidth, -1) + ", ChunkWidthInTiles = " + Accessor.tryGet(null, f_ChunkWidthInTiles, -1));
    }
}
