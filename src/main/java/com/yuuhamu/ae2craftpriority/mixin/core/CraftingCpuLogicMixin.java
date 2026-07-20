package com.yuuhamu.ae2craftpriority.mixin.core;

import com.yuuhamu.ae2craftpriority.priority.PendingCraftPriority;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.me.cluster.implementations.CraftingCPUCluster;

@Mixin(value = CraftingCpuLogic.class, remap = false)
public abstract class CraftingCpuLogicMixin {

    @Shadow
    @Final
    CraftingCPUCluster cluster;

    @Inject(method = "trySubmitJob", at = @At("RETURN"))
    private void ae2cp$onTrySubmitJob(IGrid grid, ICraftingPlan plan, IActionSource src, ICraftingRequester requester,
            CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        Integer pending = PendingCraftPriority.get();
        if (pending == null) {
            return;
        }
        ICraftingSubmitResult result = cir.getReturnValue();
        if (result != null && result.successful()) {
            PriorityHolder.setPriority(this.cluster, pending);
        }
    }

    @Inject(method = "finishJob", at = @At("TAIL"))
    private void ae2cp$onFinishJob(boolean success, CallbackInfo ci) {
        PriorityHolder.setPriority(this.cluster, PriorityHolder.DEFAULT_PRIORITY);
    }
}
