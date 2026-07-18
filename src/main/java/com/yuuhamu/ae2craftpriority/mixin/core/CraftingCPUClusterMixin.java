package com.yuuhamu.ae2craftpriority.mixin.core;

import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.me.cluster.implementations.CraftingCPUCluster;

@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class CraftingCPUClusterMixin implements PriorityHolder {

    private static final String NBT_KEY = "ae2cp_priority";

    @Unique
    private int ae2cp$priority = PriorityHolder.DEFAULT_PRIORITY;

    @Override
    public int ae2cp$getPriority() {
        return this.ae2cp$priority;
    }

    @Override
    public void ae2cp$setPriority(int priority) {
        this.ae2cp$priority = priority;
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void ae2cp$onWriteToNBT(CompoundTag data, CallbackInfo ci) {
        data.putInt(NBT_KEY, this.ae2cp$priority);
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void ae2cp$onReadFromNBT(CompoundTag data, CallbackInfo ci) {
        this.ae2cp$priority = data.contains(NBT_KEY) ? data.getInt(NBT_KEY) : PriorityHolder.DEFAULT_PRIORITY;
    }
}
