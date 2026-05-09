package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Accessor;
import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.Field;
import me.zed_0xff.zombie_buddy.Patch.FieldRW;

import java.util.HashMap;

import se.krka.kahlua.vm.KahluaTable;
import zombie.iso.IsoChunkMap;
import zombie.Lua.LuaManager;

@Patch(className = "zombie.iso.IsoChunkMap", methodName = "CalcChunkWidth")
public class Patch_IsoChunkMap {
    public static final boolean ALL_FIELDS_FOUND = true;

    static final HashMap<String, String> ZB_RESOLVED_FIELDS = new HashMap<>();

    @Patch.StaticFieldAlias(  {"CHUNKS_PER_WIDTH", "ChunksPerWidth"})     static int CHUNKS_PER_WIDTH;
    @Patch.StaticFieldAliasRW({"chunkGridWidth", "ChunkGridWidth"})       static int chunkGridWidth;
    @Patch.StaticFieldAliasRW({"chunkWidthInTiles", "ChunkWidthInTiles"}) static int chunkWidthInTiles;

    public static int getChunksPerWidth() {
        return CHUNKS_PER_WIDTH;
    }

    @Patch.OnExit
    public static void exit() {
        if (ZBBetterFPS.g_MaxRenderDistance == 0) {
            System.out.println("[ZBBetterFPS] using default render distance " + chunkGridWidth);
            return;
        }

        if (CHUNKS_PER_WIDTH <= 0 || chunkGridWidth <= 0 || chunkWidthInTiles <= 0) {
            System.err.println("[ZBBetterFPS] invalid values: ChunksPerWidth = " + CHUNKS_PER_WIDTH + ", ChunkGridWidth = " + chunkGridWidth + ", ChunkWidthInTiles = " + chunkWidthInTiles);
            return;
        }

        if (chunkWidthInTiles != chunkGridWidth * CHUNKS_PER_WIDTH) {
            System.err.println("[ZBBetterFPS] chunkWidthInTiles is not equal to chunkGridWidth * CHUNKS_PER_WIDTH, skipping patch");
            System.err.println("[ZBBetterFPS] ChunksPerWidth = " + CHUNKS_PER_WIDTH + ", ChunkGridWidth = " + chunkGridWidth + ", ChunkWidthInTiles = " + chunkWidthInTiles);
            return;
        }

        chunkGridWidth    = ZBBetterFPS.g_MaxRenderDistance;
        chunkWidthInTiles = chunkGridWidth * CHUNKS_PER_WIDTH;

        System.out.println("[ZBBetterFPS] Done: ChunkGridWidth = " + chunkGridWidth + ", ChunkWidthInTiles = " + chunkWidthInTiles);
        updateLuaCachedValues(chunkGridWidth, chunkWidthInTiles);
    }

    public static void updateLuaCachedValues(int chunkGridWidth, int chunkWidthInTiles) {
        var env = LuaManager.env;
        if (env == null) {
            System.err.println("[ZBBetterFPS] updateLuaCachedValues: Failed to get Lua environment");
            return;
        }

        var isoChunkMap = env.rawget("IsoChunkMap");
        if (isoChunkMap instanceof KahluaTable tbl) {
            syncField(tbl, "chunkGridWidth",    chunkGridWidth);
            syncField(tbl, "chunkWidthInTiles", chunkWidthInTiles);
        } else {
            System.err.println("[ZBBetterFPS] updateLuaCachedValues: Failed to get IsoChunkMap");
        }
    }

    public static void syncField(KahluaTable tbl, String fieldName, int value) {
        var resolvedName = ZB_RESOLVED_FIELDS.get(fieldName);
        if (resolvedName == null) {
            System.err.println("[ZBBetterFPS] syncField: Failed to resolve field name for " + fieldName);
            return;
        }
        tbl.rawset(resolvedName, value);
    }
}
