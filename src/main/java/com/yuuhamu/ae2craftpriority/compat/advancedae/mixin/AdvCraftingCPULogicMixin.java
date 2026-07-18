package com.yuuhamu.ae2craftpriority.compat.advancedae.mixin;

import com.yuuhamu.ae2craftpriority.priority.PendingCraftPriority;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.common.logic.AdvCraftingCPULogic;

/**
 * ジョブ提出時、事前に選択された優先度({@link PendingCraftPriority})を新しく作られる
 * {@code AdvCraftingCPU}(=このジョブ専用の仮想CPU)自身に適用する。
 *
 * <p>優先度の保持場所は {@code AdvCraftingCPUCluster}(物理本体、複数ジョブが共有)ではなく
 * {@code AdvCraftingCPU}(ジョブ単体、{@link AdvCraftingCPUMixin} 参照)。
 * {@code AdvCraftingCPU} はジョブ提出のたびに新規生成され、ジョブ完了時にオブジェクトごと
 * 破棄されるため、バニラ向け {@code CraftingCpuLogicMixin} が行っている
 * 「ジョブ完了時に優先度をデフォルトへ手動リセットする」処理はここでは不要
 * (次のジョブは常に優先度0の新しい {@code AdvCraftingCPU} から始まる)。</p>
 */
@Mixin(value = AdvCraftingCPULogic.class, remap = false)
public abstract class AdvCraftingCPULogicMixin {

    @Shadow
    @Final
    AdvCraftingCPU cpu;

    @Inject(method = "trySubmitJob", at = @At("RETURN"))
    private void ae2cp$onTrySubmitJob(IGrid grid, ICraftingPlan plan, IActionSource src, ICraftingRequester requester,
            CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        Integer pending = PendingCraftPriority.get();
        if (pending == null) {
            return;
        }
        ICraftingSubmitResult result = cir.getReturnValue();
        if (result != null && result.successful()) {
            PriorityHolder.setPriority(this.cpu, pending);
        }
    }
}
