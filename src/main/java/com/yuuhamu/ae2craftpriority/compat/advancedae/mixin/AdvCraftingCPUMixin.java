package com.yuuhamu.ae2craftpriority.compat.advancedae.mixin;

import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import net.minecraft.nbt.CompoundTag;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AdvCraftingCPU.class, remap = false)
public abstract class AdvCraftingCPUMixin implements PriorityHolder {

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
