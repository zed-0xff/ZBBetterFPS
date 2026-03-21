package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Accessor;
import me.zed_0xff.zombie_buddy.Patch;
import zombie.characters.IsoPlayer;
import zombie.core.textures.MultiTextureFBO2;

import java.lang.reflect.Field;

@Patch(className = "zombie.core.textures.MultiTextureFBO2", methodName = "update")
public final class Patch_MultiTextureFBO2 {

    public static final Field f_zoom       = Accessor.findField(MultiTextureFBO2.class, "zoom");
    public static final Field f_targetZoom = Accessor.findField(MultiTextureFBO2.class, "targetZoom");

    public static final boolean ALL_FIELDS_FOUND = f_zoom != null && f_targetZoom != null;

    public static boolean bErrorShown = false;

    @Patch.OnExit
    public static void onExit(@Patch.This MultiTextureFBO2 self) {
        if (!ZBBetterFPS.g_InstantZoom) {
            return;
        }

        if (f_zoom == null || f_targetZoom == null) {
            if (!bErrorShown) {
                bErrorShown = true;
                System.err.println("[!] Patch_MultiTextureFBO2_update: Failed to find zoom or targetZoom fields");
            }
            return;
        }

        try {
            float[] zoom       = Accessor.tryGet(self, f_zoom, null);
            float[] targetZoom = Accessor.tryGet(self, f_targetZoom, null);
            if (zoom == null || targetZoom == null) {
                return;
            }

            int playerIndex = IsoPlayer.getPlayerIndex();
            if (playerIndex < 0 || playerIndex >= zoom.length || playerIndex >= targetZoom.length) {
                return;
            }

            // Snap current zoom to target zoom for the active player.
            zoom[playerIndex] = targetZoom[playerIndex];
        } catch (Throwable e) {
            if (!bErrorShown) {
                bErrorShown = true;
                System.err.println("[!] Patch_MultiTextureFBO2_update: " + e.getMessage());
            }
        }
    }
}

