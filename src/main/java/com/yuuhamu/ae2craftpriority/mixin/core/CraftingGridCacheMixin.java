package com.yuuhamu.ae2craftpriority.mixin.core;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.yuuhamu.ae2craftpriority.priority.CraftingCPUPriorityComparator;
import com.yuuhamu.ae2craftpriority.priority.PriorityOrderedHashSet;

import appeng.api.networking.IGrid;
import appeng.me.cache.CraftingGridCache;
import appeng.me.cluster.implementations.CraftingCPUCluster;

/**
 * {@code CraftingGridCache#onUpdateTick()}が{@code craftingCPUClusters}を反復する順序を、
 * 優先度降順に固定する。
 *
 * <p>1.16.5の{@code craftingCPUClusters}は1.18.2/1.20.1の{@code CraftingService}と異なり、
 * {@code updateCPUClusters()}(実ソース270行目付近)が毎回{@code clear()}してから{@code add()}し
 * 直す設計だが、フィールド自体のインスタンスは差し替えられない(常に同一オブジェクトを
 * clear/addするだけ)ため、コンストラクタで一度差し替えてしまえば以降もそのまま機能する。</p>
 *
 * <p>差し替えは{@code craftingCPUClusters}というフィールド名を直接{@code @Shadow}して行い、
 * コンストラクタ内の"何番目の{@code new HashSet()}呼び出しか"という位置(ordinal)には
 * 依存しない。{@code CraftingGridCache}のコンストラクタは他に{@code craftingProviders}
 * ({@code Set<ICraftingProvider>})・{@code emitableItems}({@code Set<IAEItemStack>})も
 * HashSetで初期化しており、ordinal依存の{@code @Redirect}は事故の元になるため避けた。</p>
 */
@Mixin(value = CraftingGridCache.class, remap = false)
public abstract class CraftingGridCacheMixin {

    @Unique
    private static final Logger ae2cp$LOGGER = LogManager.getLogger();

    @Unique
    private static boolean ae2cp$loggedOnce = false;

    @Shadow
    @Final
    @Mutable
    private Set<CraftingCPUCluster> craftingCPUClusters;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2cp$onInit(IGrid grid, CallbackInfo ci) {
        this.craftingCPUClusters = new PriorityOrderedHashSet<CraftingCPUCluster>(
                CraftingCPUPriorityComparator.INSTANCE);
        if (!ae2cp$loggedOnce) {
            ae2cp$loggedOnce = true;
            ae2cp$LOGGER.info("AE2CraftPriority: craftingCPUClusters を優先度順Setに差し替えました");
        }
    }
}
