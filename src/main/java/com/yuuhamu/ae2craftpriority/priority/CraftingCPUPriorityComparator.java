package com.yuuhamu.ae2craftpriority.priority;

import java.util.Comparator;

import com.yuuhamu.ae2craftpriority.api.CraftPriorityApi;

import appeng.me.cluster.implementations.CraftingCPUCluster;

/**
 * {@code CraftingGridCacheMixin}が{@code craftingCPUClusters}の反復順序を優先度降順に固定する
 * ために使う{@link Comparator}。
 *
 * <p>元は{@code CraftingGridCacheMixin}の{@code @Inject}メソッド内の匿名クラス
 * ({@code new Comparator<CraftingCPUCluster>() { ... }})として実装していたが、匿名クラスは
 * Mixinが対象クラス({@code CraftingGridCache})へマージする際に内部クラスの再配置(リネーム)が
 * 必要になり、{@code CraftingCPUScreenMixin}の匿名{@code IconButton}サブクラスで実機起動時に
 * {@code NoClassDefFoundError}を引き起こした前例(2026-07-22発見)と同じ罠を踏む可能性がある
 * ため、Mixin対象クラスに一切マージされない独立したトップレベルクラスとして切り出した。
 * 詳細は{@code Knowledge/mixin-anonymous-class-in-injected-method-breaks-classloading.md}参照。</p>
 */
public final class CraftingCPUPriorityComparator implements Comparator<CraftingCPUCluster> {

    public static final CraftingCPUPriorityComparator INSTANCE = new CraftingCPUPriorityComparator();

    private CraftingCPUPriorityComparator() {
    }

    @Override
    public int compare(CraftingCPUCluster a, CraftingCPUCluster b) {
        return Integer.compare(CraftPriorityApi.getPriority(b), CraftPriorityApi.getPriority(a));
    }
}
