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
