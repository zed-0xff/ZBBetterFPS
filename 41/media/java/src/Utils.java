package me.zed_0xff.zb_better_fps;

import zombie.iso.IsoWorld;

public class Utils {
    /** 41.78 uses CurrentCell (PascalCase). */
    public static boolean isGameStarted() {
        return IsoWorld.instance != null && IsoWorld.instance.CurrentCell != null;
    }
}
