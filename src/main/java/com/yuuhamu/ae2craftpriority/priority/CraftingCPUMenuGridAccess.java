package com.yuuhamu.ae2craftpriority.priority;

import appeng.api.networking.IGrid;

public interface CraftingCPUMenuGridAccess {

    IGrid ae2cp$getGrid();

    static IGrid getGridOrNull(Object menu) {
        if (menu instanceof CraftingCPUMenuGridAccess access) {
            return access.ae2cp$getGrid();
        }
        return null;
    }
}
