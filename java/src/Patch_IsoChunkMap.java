package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.annotations.Patch;
import me.zed_0xff.zombie_buddy.annotations.Patch.Field;
import me.zed_0xff.zombie_buddy.Logger;

import java.util.HashMap;
import java.util.Map;

import se.krka.kahlua.vm.KahluaTable;
import zombie.iso.IsoChunkMap;
import zombie.Lua.LuaManager;

@Patch(className = "zombie.iso.IsoChunkMap", methodName = "CalcChunkWidth")
public class Patch_IsoChunkMap {
    public static int N_OK = 0, N_SKIP = 0, N_FAIL = 0;

    static final Logger.Instance _logger = Logger.get("ZBBetterFPS", Logger.DEBUG);

    @Patch.NameMap
    public static Map<String, String> _nameMap = new HashMap<>();

    @Patch.OnExit
    public static void exit(
        @Field({"CHUNKS_PER_WIDTH",  "ChunksPerWidth"})    final int CHUNKS_PER_WIDTH,
        @Field({"chunkGridWidth",    "ChunkGridWidth"})    int chunkGridWidth,
        @Field({"chunkWidthInTiles", "ChunkWidthInTiles"}) int chunkWidthInTiles
    ) {
        _logger.debug("CalcChunkWidth: CHUNKS_PER_WIDTH", CHUNKS_PER_WIDTH, "chunkGridWidth", chunkGridWidth, "chunkWidthInTiles", chunkWidthInTiles, "g_MaxRenderDistance", ZBBetterFPS.g_MaxRenderDistance);

        if (ZBBetterFPS.g_MaxRenderDistance == 0) {
            _logger.info("using default render distance", chunkGridWidth);
            N_SKIP++;
            return;
        }

        if (CHUNKS_PER_WIDTH <= 0 || chunkGridWidth <= 0 || chunkWidthInTiles <= 0) {
            _logger.error("invalid values: ChunksPerWidth =", CHUNKS_PER_WIDTH, ", ChunkGridWidth =", chunkGridWidth, ", ChunkWidthInTiles =", chunkWidthInTiles);
            N_FAIL++;
            return;
        }

        if (chunkWidthInTiles != chunkGridWidth * CHUNKS_PER_WIDTH) {
            _logger.error("chunkWidthInTiles is not equal to chunkGridWidth * CHUNKS_PER_WIDTH, skipping patch");
            _logger.error("ChunksPerWidth =", CHUNKS_PER_WIDTH, ", ChunkGridWidth =", chunkGridWidth, ", ChunkWidthInTiles =", chunkWidthInTiles);
            N_FAIL++;
            return;
        }

        chunkGridWidth    = ZBBetterFPS.g_MaxRenderDistance;
        chunkWidthInTiles = chunkGridWidth * CHUNKS_PER_WIDTH;

        _logger.info("Done: ChunkGridWidth =", chunkGridWidth, ", ChunkWidthInTiles =", chunkWidthInTiles);
        updateLuaCachedValues(chunkGridWidth, chunkWidthInTiles);
        N_OK++;
    }

    public static void updateLuaCachedValues(int chunkGridWidth, int chunkWidthInTiles) {
        var env = LuaManager.env;
        if (env == null) {
            _logger.error("updateLuaCachedValues: Failed to get Lua environment");
            return;
        }

        var isoChunkMap = env.rawget("IsoChunkMap");
        if (isoChunkMap instanceof KahluaTable tbl) {
            syncField(tbl, _nameMap, "chunkGridWidth",    chunkGridWidth);
            syncField(tbl, _nameMap, "chunkWidthInTiles", chunkWidthInTiles);
        } else {
            N_FAIL++;
            _logger.error("updateLuaCachedValues: Failed to get IsoChunkMap");
        }
    }

    public static void syncField(KahluaTable tbl, final Map<String, String> nameMap, String fieldName, int value) {
        var resolvedName = nameMap.get(fieldName);
        if (resolvedName == null) {
            N_FAIL++;
            _logger.error("syncField: Failed to resolve field name for", fieldName);
            _logger.debug("[ZBBetterFPS] nameMap:", nameMap);
            return;
        }
        tbl.rawset(resolvedName, value);
    }
}
