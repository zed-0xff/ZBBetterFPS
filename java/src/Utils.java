package me.zed_0xff.zb_better_fps;

import me.zed_0xff.zombie_buddy.Reflect;

import java.lang.invoke.VarHandle;

import zombie.core.Core;
import zombie.iso.IsoCell;
import zombie.iso.IsoWorld;

public class Utils {
    static private final VarHandle vh_currentCell = Reflect.on(IsoWorld.class).getVarHandle(
        IsoCell.class, "currentCell", "CurrentCell"
    );

    public static boolean isGameStarted() {
        return IsoWorld.instance != null && vh_currentCell != null && vh_currentCell.get(IsoWorld.instance) != null;
    }

    public static boolean isDebug() {
        var core = Core.getInstance();
        return core != null && core.getDebug();
    }
}
