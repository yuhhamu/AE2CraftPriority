package com.yuuhamu.ae2craftpriority.mixin.core;

import java.util.Comparator;
import java.util.Set;

import com.mojang.logging.LogUtils;
import com.yuuhamu.ae2craftpriority.api.CraftPriorityApi;
import com.yuuhamu.ae2craftpriority.priority.PriorityOrderedHashSet;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;

@Mixin(value = CraftingService.class, remap = false)
public abstract class CraftingServiceMixin {

    @Unique
    private static final Logger ae2cp$LOGGER = LogUtils.getLogger();

    @Unique
    private static boolean ae2cp$loggedOnce = false;

    @Shadow
    @Final
    @Mutable
    private Set<CraftingCPUCluster> craftingCPUClusters;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2cp$onInit(CallbackInfo ci) {
        this.craftingCPUClusters = new PriorityOrderedHashSet<>(
                Comparator.<CraftingCPUCluster>comparingInt(CraftPriorityApi::getPriority).reversed());
        if (!ae2cp$loggedOnce) {
            ae2cp$loggedOnce = true;
            ae2cp$LOGGER.info("AE2CraftPriority: craftingCPUClusters を優先度順Setに差し替えました");
        }
    }
}
