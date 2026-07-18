package com.yuuhamu.ae2craftpriority.api;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface PriorityAdapter {

    boolean supports(Object target);

    int getPriority(Object target);

    void setPriority(Object target, int priority);

    default BlockEntity getPriorityHostBlockEntity(Object target) {
        return null;
    }

    default void prepareForPriorityEdit(Object target, BlockEntity host) {
    }
}
