package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.iso.IsoChunkMap;

@Patch(className = "zombie.iso.IsoChunkMap", methodName = "CalcChunkWidth")
public class Patch_IsoChunkMap {
    @Patch.OnExit
    public static void exit() {
        if (ZBBetterFPS.g_MaxRenderDistance == 0) {
            return;
        }

        // 41.78 uses PascalCase static fields
        if (IsoChunkMap.ChunkWidthInTiles != IsoChunkMap.ChunkGridWidth * 8) {
            System.err.println("[ZBBetterFPS] ChunkWidthInTiles is not equal to ChunkGridWidth * 8, skipping patch");
            return;
        }

        System.out.println("[ZBBetterFPS] overriding max render distance with " + ZBBetterFPS.g_MaxRenderDistance);

        IsoChunkMap.ChunkGridWidth = ZBBetterFPS.g_MaxRenderDistance;
        IsoChunkMap.ChunkWidthInTiles = IsoChunkMap.ChunkGridWidth * 8;
    }
}
