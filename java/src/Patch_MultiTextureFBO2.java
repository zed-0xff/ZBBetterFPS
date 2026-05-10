package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.Field;

import zombie.characters.IsoPlayer;
import zombie.core.textures.MultiTextureFBO2;

@Patch(className = "zombie.core.textures.MultiTextureFBO2", methodName = "update")
public final class Patch_MultiTextureFBO2 {
    public static int N_OK = 0, N_SKIP = 0, N_FAIL = 0;

    public static boolean bErrorShown = false;

    @Patch.OnExit
    public static void onExit(
            @Field final float[] zoom,
            @Field final float[] targetZoom,

            @Patch.This MultiTextureFBO2 self
    ) {
        if (!ZBBetterFPS.g_InstantZoom) {
            N_SKIP++;
            return;
        }

        if (zoom == null || targetZoom == null) {
            if (!bErrorShown) {
                bErrorShown = true;
                System.err.println("[!] Patch_MultiTextureFBO2_update: Failed to find zoom or targetZoom fields");
            }
            N_FAIL++;
            return;
        }

        try {
            int playerIndex = IsoPlayer.getPlayerIndex();
            if (playerIndex < 0 || playerIndex >= zoom.length || playerIndex >= targetZoom.length) {
                N_SKIP++;
                return;
            }

            // Snap current zoom to target zoom for the active player.
            zoom[playerIndex] = targetZoom[playerIndex];
            N_OK++;
        } catch (Throwable t) {
            N_FAIL++;
            if (!bErrorShown) {
                bErrorShown = true;
                System.err.println("[!] Patch_MultiTextureFBO2_update: " + t.getMessage());
            }
        }
    }
}

