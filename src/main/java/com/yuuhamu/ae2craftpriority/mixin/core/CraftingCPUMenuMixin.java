package com.yuuhamu.ae2craftpriority.mixin.core;

import com.yuuhamu.ae2craftpriority.priority.CraftingCPUMenuGridAccess;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.networking.IGrid;
import appeng.menu.me.crafting.CraftingCPUMenu;

@Mixin(value = CraftingCPUMenu.class, remap = false)
public abstract class CraftingCPUMenuMixin implements CraftingCPUMenuGridAccess {

    @Shadow
    IGrid getGrid() {
        throw new UnsupportedOperationException();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2cp$onInit(MenuType<?> menuType, int id, Inventory ip, Object te, CallbackInfo ci) {
        if (!ip.player.level().isClientSide()) {
            PriorityReturnTarget.clear(ip.player.getUUID());
        }
    }

    @Unique
    @Override
    public IGrid ae2cp$getGrid() {
        return this.getGrid();
    }
}
