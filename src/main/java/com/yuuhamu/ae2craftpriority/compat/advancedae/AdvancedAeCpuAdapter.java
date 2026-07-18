package com.yuuhamu.ae2craftpriority.compat.advancedae;

import java.lang.reflect.Field;

import com.yuuhamu.ae2craftpriority.api.PriorityAdapter;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPUCluster;

public final class AdvancedAeCpuAdapter implements PriorityAdapter {

    private static volatile Field clusterField;

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

        if (host instanceof AdvCraftingPriorityCpuHost priorityHost) {
            priorityHost.ae2cp$setActivePriorityCpu((AdvCraftingCPU) target);
        }
    }
}
