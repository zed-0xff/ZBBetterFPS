package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.AdapterFactory;
import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.Field;
import me.zed_0xff.zombie_buddy.Patch.FieldRW;

import java.util.Map;

import se.krka.kahlua.vm.KahluaTable;
import zombie.iso.IsoChunkMap;
import zombie.Lua.LuaManager;

@Patch(className = "zombie.iso.IsoChunkMap", methodName = "CalcChunkWidth")
public class Patch_IsoChunkMap {
    public static int N_OK = 0, N_SKIP = 0, N_FAIL = 0;

    @Patch.OnExit
    public static void exit(
        @Field(  {"CHUNKS_PER_WIDTH",  "ChunksPerWidth"})    final int CHUNKS_PER_WIDTH,
        @FieldRW({"chunkGridWidth",    "ChunkGridWidth"})    int chunkGridWidth,
        @FieldRW({"chunkWidthInTiles", "ChunkWidthInTiles"}) int chunkWidthInTiles,
        @Patch.NameMap                                       final Map<String, String> nameMap
    ) {
        if (ZBBetterFPS.g_MaxRenderDistance == 0) {
            System.out.println("[ZBBetterFPS] using default render distance " + chunkGridWidth);
            N_SKIP++;
            return;
        }

        if (CHUNKS_PER_WIDTH <= 0 || chunkGridWidth <= 0 || chunkWidthInTiles <= 0) {
            System.err.println("[ZBBetterFPS] invalid values: ChunksPerWidth = " + CHUNKS_PER_WIDTH + ", ChunkGridWidth = " + chunkGridWidth + ", ChunkWidthInTiles = " + chunkWidthInTiles);
            N_FAIL++;
            return;
        }

        if (chunkWidthInTiles != chunkGridWidth * CHUNKS_PER_WIDTH) {
            System.err.println("[ZBBetterFPS] chunkWidthInTiles is not equal to chunkGridWidth * CHUNKS_PER_WIDTH, skipping patch");
            System.err.println("[ZBBetterFPS] ChunksPerWidth = " + CHUNKS_PER_WIDTH + ", ChunkGridWidth = " + chunkGridWidth + ", ChunkWidthInTiles = " + chunkWidthInTiles);
            N_FAIL++;
            return;
        }

        chunkGridWidth    = ZBBetterFPS.g_MaxRenderDistance;
        chunkWidthInTiles = chunkGridWidth * CHUNKS_PER_WIDTH;

        System.out.println("[ZBBetterFPS] Done: ChunkGridWidth = " + chunkGridWidth + ", ChunkWidthInTiles = " + chunkWidthInTiles);
        updateLuaCachedValues(nameMap, chunkGridWidth, chunkWidthInTiles);
        N_OK++;
    }

    public static void updateLuaCachedValues(final Map<String, String> nameMap, int chunkGridWidth, int chunkWidthInTiles) {
        var env = LuaManager.env;
        if (env == null) {
            System.err.println("[ZBBetterFPS] updateLuaCachedValues: Failed to get Lua environment");
            return;
        }

        var isoChunkMap = env.rawget("IsoChunkMap");
        if (isoChunkMap instanceof KahluaTable tbl) {
            syncField(tbl, nameMap, "chunkGridWidth",    chunkGridWidth);
            syncField(tbl, nameMap, "chunkWidthInTiles", chunkWidthInTiles);
        } else {
            N_FAIL++;
            System.err.println("[ZBBetterFPS] updateLuaCachedValues: Failed to get IsoChunkMap");
        }
    }

    public static void syncField(KahluaTable tbl, final Map<String, String> nameMap, String fieldName, int value) {
        var resolvedName = nameMap.get(fieldName);
        if (resolvedName == null) {
            N_FAIL++;
            System.err.println("[ZBBetterFPS] syncField: Failed to resolve field name for " + fieldName);
            // Logger.debug("[ZBBetterFPS] nameMap:", nameMap);
            return;
        }
        tbl.rawset(resolvedName, value);
    }
}
