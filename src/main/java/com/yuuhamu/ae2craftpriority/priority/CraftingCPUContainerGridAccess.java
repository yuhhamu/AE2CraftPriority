package com.yuuhamu.ae2craftpriority.priority;

import appeng.api.networking.IGrid;

/**
 * {@code CraftingCPUContainer#getGrid()}はpackage-private(appeng.container.me.crafting限定)のため、
 * 別パッケージの{@code CraftingStatusContainerMixin}から直接{@code @Shadow}できない。
 * {@code CraftingCPUContainerMixin}がこのduck interfaceを実装して橋渡しする。
 */
public interface CraftingCPUContainerGridAccess {

    IGrid ae2cp$getGrid();

    static IGrid getGridOrNull(Object container) {
        if (container instanceof CraftingCPUContainerGridAccess) {
            return ((CraftingCPUContainerGridAccess) container).ae2cp$getGrid();
        }
        return null;
    }
}
