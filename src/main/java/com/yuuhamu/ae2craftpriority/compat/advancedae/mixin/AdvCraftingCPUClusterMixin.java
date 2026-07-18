package com.yuuhamu.ae2craftpriority.compat.advancedae.mixin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPUCluster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AdvancedAE(Quantum Computer)で、優先度が実際のスケジューリングへ効くようにする本体。
 *
 * <p>{@code getActiveCPUs()} の戻り値({@code List<AdvCraftingCPU>}、タスク単位)を優先度降順に
 * 並べ替えて返す。AdvancedAE自身のクラフト実行ループ(tick処理・{@code getCpus()}・
 * {@code insertIntoCpus()} 等)は全て {@code cluster.getActiveCPUs()} をこの1箇所経由で呼んでいる
 * ため、ここを差し替えるだけで「同一Quantum Computer上で複数タスクが共有Pattern Providerを
 * 取り合う際、優先度の高いタスクが先にtickされて機械を確保する」というバニラCPUと同じ効果を
 * 実現できる。</p>
 *
 * <p>このMixinはAdvancedAE導入時のみ適用される別Mixin設定({@code ae2craftpriority-advancedae.mixins.json}、
 * {@code AE2CraftPriorityMod} が {@code ModList} 確認後に登録)に属するため、AdvancedAE
 * 未導入環境では一切ロードされない。</p>
 */
@Mixin(value = AdvCraftingCPUCluster.class, remap = false)
public abstract class AdvCraftingCPUClusterMixin {

    @Inject(method = "getActiveCPUs", at = @At("RETURN"), cancellable = true)
    private void ae2cp$sortActiveCpusByPriority(CallbackInfoReturnable<List<AdvCraftingCPU>> cir) {
        List<AdvCraftingCPU> sorted = new ArrayList<>(cir.getReturnValue());
        sorted.sort(Comparator.<AdvCraftingCPU>comparingInt(PriorityHolder::getPriorityOrDefault).reversed());
        cir.setReturnValue(sorted);
    }
}
