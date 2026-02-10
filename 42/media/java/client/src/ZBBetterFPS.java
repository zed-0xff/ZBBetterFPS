package me.zed_0xff.zb_better_fps;

import zombie.iso.IsoChunkMap;

import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Exposer;

@Exposer.LuaClass
public class ZBBetterFPS {
    public static int g_MaxRenderDistance = 0; // 0 = default value

    public static void setMaxRenderDistance(int distance) {
        g_MaxRenderDistance = (short) distance;
    }

    @Patch(className = "zombie.iso.IsoChunkMap", methodName = "CalcChunkWidth")
    public static class Patch_setMaxRenderDistance {
        @Patch.OnExit
        public static void exit() {
            if (g_MaxRenderDistance == 0) {
                return;
            }

            if (IsoChunkMap.ChunkWidthInTiles != IsoChunkMap.ChunkGridWidth * 8) {
                System.err.println("[ZBBetterFPS] ChunkWidthInTiles is not equal to ChunkGridWidth * 8, skipping patch");
                return;
            }

            System.out.println("[ZBBetterFPS] overriding max render distance with " + g_MaxRenderDistance);

            IsoChunkMap.ChunkGridWidth = g_MaxRenderDistance;
            IsoChunkMap.ChunkWidthInTiles = IsoChunkMap.ChunkGridWidth * 8;
        }
    }
}

