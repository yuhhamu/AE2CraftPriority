package com.yuuhamu.ae2craftpriority.mixin.status;

import net.minecraft.client.gui.IHasContainer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.yuuhamu.ae2craftpriority.priority.CraftingStatusPriorityControl;

import appeng.client.gui.me.crafting.CraftingStatusScreen;
import appeng.container.me.crafting.CraftingStatusContainer;

/**
 * 端末の「Crafting Status」タブ(README方法3)のCPU選択ボタンの表示を、
 * 「Crafting CPU: #1」から「CPU: #1@{優先度}」形式に変更する。
 *
 * <p>優先度の値は{@code CraftingStatusContainerMixin}が追加した{@code @GuiSync}フィールド
 * ({@link CraftingStatusPriorityControl#ae2cp$getPriority()}経由)から取得する。この値は
 * AE2純正の{@code DataSynchronization}機構によりサーバーから自動同期されているため、
 * クライアント側で追加のパケット処理は不要。</p>
 *
 * <p>{@code getNextCpuButtonLabel()}はAE2自身が新規宣言する private メソッド(vanillaの
 * オーバーライドではない)であり、{@code CraftingStatusScreen}自身が直接宣言しているため、
 * {@code remap = false}のままHEADで丸ごと差し替えても
 * {@code InvalidMixinException}にはならない(実績のある{@code onCPUSelectionChanged}と同型)。</p>
 */
@Mixin(value = CraftingStatusScreen.class, remap = false)
public abstract class CraftingStatusScreenMixin {

    @Inject(method = "getNextCpuButtonLabel", at = @At("HEAD"), cancellable = true)
    private void ae2cp$overrideCpuButtonLabel(CallbackInfoReturnable<ITextComponent> cir) {
        @SuppressWarnings("unchecked")
        final CraftingStatusContainer container = ((IHasContainer<CraftingStatusContainer>) (Object) this)
                .getContainer();
        if (container.noCPU) {
            // 「クラフトジョブがありません」等、バニラのメッセージをそのまま使う。
            return;
        }

        final ITextComponent name = container.cpuName != null ? container.cpuName : new StringTextComponent("");
        final int priority = ((CraftingStatusPriorityControl) container).ae2cp$getPriority();
        cir.setReturnValue(new StringTextComponent("CPU: " + name.getString() + "@" + priority));
    }
}
