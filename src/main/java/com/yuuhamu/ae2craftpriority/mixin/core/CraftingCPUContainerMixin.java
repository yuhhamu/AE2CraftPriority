package com.yuuhamu.ae2craftpriority.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.yuuhamu.ae2craftpriority.priority.CraftingCPUContainerGridAccess;

import appeng.api.networking.IGrid;
import appeng.container.me.crafting.CraftingCPUContainer;

/**
 * {@code CraftingCPUContainer#getGrid()}はpackage-private({@code appeng.container.me.crafting}
 * 限定)であり、{@code CraftingStatusContainer}(⑥)は継承しているだけで自身では宣言していない
 * (1.16.5実ソースで確認済み)ため、別パッケージの{@code CraftingStatusContainerMixin}から
 * 直接{@code @Shadow}できない(1.20.1版で発生した{@code CraftingCPUMenuMixin}新設と同一の罠)。
 *
 * <p>このMixinは{@code CraftingCPUContainer}自身(宣言クラス)に適用し、{@code getGrid()}を
 * duck interface({@link CraftingCPUContainerGridAccess})経由で公開する橋渡し役を担う。</p>
 */
@Mixin(value = CraftingCPUContainer.class, remap = false)
public abstract class CraftingCPUContainerMixin implements CraftingCPUContainerGridAccess {

    @Shadow
    abstract IGrid getGrid();

    @Override
    public IGrid ae2cp$getGrid() {
        return this.getGrid();
    }
}
