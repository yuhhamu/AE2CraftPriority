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

@Mixin(value = AdvCraftingCPUCluster.class, remap = false)
public abstract class AdvCraftingCPUClusterMixin {

    @Inject(method = "getActiveCPUs", at = @At("RETURN"), cancellable = true)
    private void ae2cp$sortActiveCpusByPriority(CallbackInfoReturnable<List<AdvCraftingCPU>> cir) {
        List<AdvCraftingCPU> sorted = new ArrayList<>(cir.getReturnValue());
        sorted.sort(Comparator.<AdvCraftingCPU>comparingInt(PriorityHolder::getPriorityOrDefault).reversed());
        cir.setReturnValue(sorted);
    }
}
