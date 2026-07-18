package com.yuuhamu.ae2craftpriority.mixin.core;

import com.yuuhamu.ae2craftpriority.priority.CraftingPriorityHostMarker;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.helpers.IPriorityHost;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftingCPUMenu;

@Mixin(value = CraftingBlockEntity.class, remap = false)
public abstract class CraftingBlockEntityMixin implements IPriorityHost, CraftingPriorityHostMarker {

    @Override
    public int getPriority() {
        return PriorityHolder.getPriorityOrDefault(ae2cp$self().getCluster());
    }

    @Override
    public void setPriority(int newValue) {
        PriorityHolder.setPriority(ae2cp$self().getCluster(), newValue);
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        var target = PriorityReturnTarget.take(player.getUUID());
        if (target != null && MenuOpener.returnTo(target.menuType(), player, target.locator())) {
            return;
        }
        MenuOpener.returnTo(CraftingCPUMenu.TYPE, player, MenuLocators.forBlockEntity(ae2cp$self()));
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(ae2cp$self().getUnitBlock());
    }

    @Unique
    private CraftingBlockEntity ae2cp$self() {
        return (CraftingBlockEntity) (Object) this;
    }
}
