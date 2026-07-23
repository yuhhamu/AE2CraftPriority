package com.yuuhamu.ae2craftpriority.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.container.me.crafting.CraftingCPURecord;

/**
 * {@code CraftingCPURecord}のpackage-privateメソッド{@code getCpu()}(1.16.5実ソースで確認済み)
 * への読み取りアクセサ。{@code CraftingStatusContainerMixin}が別パッケージから
 * {@code onCPUSelectionChanged}のコールバック引数として渡された{@code CraftingCPURecord}から
 * 追跡中の{@link ICraftingCPU}を取り出すために使う。
 */
@Mixin(value = CraftingCPURecord.class, remap = false)
public interface CraftingCPURecordAccessor {

    @Invoker("getCpu")
    ICraftingCPU ae2cp$invokeGetCpu();
}
