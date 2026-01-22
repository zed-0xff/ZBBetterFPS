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

        if (IsoChunkMap.chunkWidthInTiles != IsoChunkMap.chunkGridWidth * 8) {
            System.err.println("[ZBBetterFPS] chunkWidthInTiles is not equal to chunkGridWidth * 8, skipping patch");
            return;
        }

        System.out.println("[ZBBetterFPS] overriding max render distance with " + ZBBetterFPS.g_MaxRenderDistance);

        IsoChunkMap.chunkGridWidth = ZBBetterFPS.g_MaxRenderDistance;
        IsoChunkMap.chunkWidthInTiles = IsoChunkMap.chunkGridWidth * 8;
    }
}
