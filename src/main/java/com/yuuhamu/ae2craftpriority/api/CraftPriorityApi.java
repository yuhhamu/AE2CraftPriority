package com.yuuhamu.ae2craftpriority.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CraftPriorityApi {

    private static final List<PriorityAdapter> ADAPTERS = new CopyOnWriteArrayList<>();

    private CraftPriorityApi() {
    }

    public static int getPriority(Object target) {
        if (target instanceof PriorityHolder holder) {
            return holder.ae2cp$getPriority();
        }
        for (PriorityAdapter adapter : ADAPTERS) {
            if (adapter.supports(target)) {
                return adapter.getPriority(target);
            }
        }
        return PriorityHolder.DEFAULT_PRIORITY;
    }

    public static void setPriority(Object target, int priority) {
        if (target instanceof PriorityHolder holder) {
            holder.ae2cp$setPriority(priority);
            return;
        }
        for (PriorityAdapter adapter : ADAPTERS) {
            if (adapter.supports(target)) {
                adapter.setPriority(target, priority);
                return;
            }
        }
    }

    public static boolean isSupported(Object target) {
        if (target instanceof PriorityHolder) {
            return true;
        }
        for (PriorityAdapter adapter : ADAPTERS) {
            if (adapter.supports(target)) {
                return true;
            }
        }
        return false;
    }

    public static BlockEntity getPriorityHostBlockEntity(Object target) {
        for (PriorityAdapter adapter : ADAPTERS) {
            if (adapter.supports(target)) {
                return adapter.getPriorityHostBlockEntity(target);
            }
        }
        return null;
    }

    public static void prepareForPriorityEdit(Object target, BlockEntity host) {
        for (PriorityAdapter adapter : ADAPTERS) {
            if (adapter.supports(target)) {
                adapter.prepareForPriorityEdit(target, host);
                return;
            }
        }
    }

    public static void registerAdapter(PriorityAdapter adapter) {
        ADAPTERS.add(adapter);
    }
}
