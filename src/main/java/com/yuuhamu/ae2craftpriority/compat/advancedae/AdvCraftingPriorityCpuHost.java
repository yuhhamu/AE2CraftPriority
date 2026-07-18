package com.yuuhamu.ae2craftpriority.compat.advancedae;

import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;

public interface AdvCraftingPriorityCpuHost {

    void ae2cp$setActivePriorityCpu(AdvCraftingCPU cpu);

    AdvCraftingCPU ae2cp$getActivePriorityCpu();
}
