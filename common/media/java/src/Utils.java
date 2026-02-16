package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Accessor;

import zombie.iso.IsoWorld;

import java.lang.reflect.Field;

public class Utils {
    public static final Field f_currentCell = Accessor.findField(IsoWorld.class,
        "currentCell",
        "CurrentCell"
    );

    public static boolean isGameStarted() {
        return IsoWorld.instance != null && Accessor.tryGet(IsoWorld.instance, f_currentCell, null) != null;
    }
}
