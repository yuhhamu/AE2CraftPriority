package com.yuuhamu.ae2craftpriority.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.yuuhamu.ae2craftpriority.priority.PendingCraftPriority;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.me.cluster.implementations.CraftingCPUCluster;

/**
 * ジョブ提出時に{@link PendingCraftPriority}の値を実際に選ばれたCrafting CPUへ適用する。
 *
 * <p>1.16.5には1.18.2/1.20.1の{@code CraftingCpuLogic}のような、ジョブ選定と実行確定を分離した
 * 独立クラスは存在せず(AE2 1.16.5実ソースで確認済み)、両方とも{@code CraftingCPUCluster}
 * 自身のメソッドに同居している。そのため、このMixinは{@code CraftingCPUClusterMixin}と
 * 同じ対象クラス({@code CraftingCPUCluster})に適用する、独立したMixinファイルとして実装した
 * (役割の分離を保つため)。</p>
 *
 * <p>対象メソッド{@code submitJob(IGrid, ICraftingJob, IActionSource, ICraftingRequester)}
 * (実ソース817〜882行目)は成功時に2箇所、失敗時に1箇所(暗黙のreturn null)、計3箇所の
 * return文を持つが、{@code @At("RETURN")}はメソッド内の全return文を無条件に拾う設計であり、
 * 特定のreturn文をordinalで狙い撃ちするものではないため安全に扱える。</p>
 */
@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class CraftingCpuLogicMixin {

    @Inject(method = "submitJob", at = @At("RETURN"))
    private void ae2cp$onSubmitJobReturn(IGrid g, ICraftingJob job, IActionSource src,
            ICraftingRequester requestingMachine, CallbackInfoReturnable<ICraftingLink> cir) {
        Integer pending = PendingCraftPriority.get();
        if (pending == null) {
            return;
        }
        if (cir.getReturnValue() != null) {
            // このクラスタへの割り当てが成功した(戻り値のICraftingLinkが非null)場合のみ適用する。
            PriorityHolder.setPriority(this, pending.intValue());
        }
    }

    /**
     * ジョブが完了したら優先度を既定値へ戻す(次にこのCPUが新しいジョブを受け付けたときに、
     * 前回の優先度が意図せず引き継がれないようにする)。
     */
    @Inject(method = "completeJob", at = @At("TAIL"))
    private void ae2cp$onCompleteJob(CallbackInfo ci) {
        PriorityHolder.setPriority(this, PriorityHolder.DEFAULT_PRIORITY);
    }

    /**
     * ユーザーによるキャンセル、またはリンクのキャンセルによる中断でも同様に既定値へ戻す。
     */
    @Inject(method = "cancel", at = @At("TAIL"))
    private void ae2cp$onCancel(CallbackInfo ci) {
        PriorityHolder.setPriority(this, PriorityHolder.DEFAULT_PRIORITY);
    }
}
