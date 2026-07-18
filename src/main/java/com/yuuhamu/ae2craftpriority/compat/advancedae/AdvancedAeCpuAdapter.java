package com.yuuhamu.ae2craftpriority.compat.advancedae;

import java.lang.reflect.Field;

import com.yuuhamu.ae2craftpriority.api.PriorityAdapter;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPUCluster;

/**
 * {@code AdvCraftingCPU}(1クラフトタスク=1ジョブに対応する仮想CPU。Quantum Computer 1台から
 * 複数生成され、並行実行されうる)から優先度を読み書きするアダプタ。
 *
 * <p>優先度の実体は {@code AdvCraftingCPU} 自身(タスク単位、{@link AdvCraftingCPUMixin} で
 * {@link PriorityHolder} 化済み)に保持している。</p>
 *
 * <p><b>{@code cluster} フィールドをMixinアクセサではなくリフレクションで読む理由</b>:
 * {@code AdvCraftingCPU} はForge起動のごく初期({@code
 * ImmediateWindowHandler$DummyProvider.updateModuleReads})で、このMod側のMixin設定が
 * 準備される前に一度クラスロードされてしまうため、通常のMixinアクセサでは適用が間に合わない
 * ({@code MixinTargetAlreadyLoadedException} や {@code ClassCastException} を引き起こす)。
 * この1フィールドの読み取りだけはリフレクションで回避している。</p>
 */
public final class AdvancedAeCpuAdapter implements PriorityAdapter {

    private static volatile Field clusterField;

    /**
     * {@code AdvCraftingCPU} が属する物理Quantum Computer本体({@code AdvCraftingCPUCluster})を
     * 取得する。優先度編集画面を開くための代表ブロックエンティティ({@link #getPriorityHostBlockEntity}）
     * を探す用途にのみ使う(優先度の実体はCPU単体側にある)。
     */
    public static AdvCraftingCPUCluster getCluster(AdvCraftingCPU cpu) {
        try {
            Field field = clusterField;
            if (field == null) {
                field = AdvCraftingCPU.class.getDeclaredField("cluster");
                field.setAccessible(true);
                clusterField = field;
            }
            return (AdvCraftingCPUCluster) field.get(cpu);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "AE2CraftPriority: AdvCraftingCPU#cluster フィールドの読み取りに失敗しました"
                            + "(AdvancedAEのバージョン更新でフィールド名/型が変わった可能性があります)", e);
        }
    }

    @Override
    public boolean supports(Object target) {
        return target instanceof AdvCraftingCPU;
    }

    @Override
    public int getPriority(Object target) {
        return PriorityHolder.getPriorityOrDefault(target);
    }

    @Override
    public void setPriority(Object target, int priority) {
        PriorityHolder.setPriority(target, priority);
    }

    @Override
    public BlockEntity getPriorityHostBlockEntity(Object target) {
        AdvCraftingCPUCluster cluster = getCluster((AdvCraftingCPU) target);
        var it = cluster.getBlockEntities();
        return it.hasNext() ? it.next() : null;
    }

    @Override
    public void prepareForPriorityEdit(Object target, BlockEntity host) {
        // AdvCraftingBlockEntity(物理Quantum Computer)は複数タスクの代表になりうるため、
        // 優先度画面を開く直前に「今どのタスクを編集中か」を覚えさせる(AdvCraftingPriorityCpuHost
        // のJavadoc参照)。
        if (host instanceof AdvCraftingPriorityCpuHost priorityHost) {
            priorityHost.ae2cp$setActivePriorityCpu((AdvCraftingCPU) target);
        }
    }
}
