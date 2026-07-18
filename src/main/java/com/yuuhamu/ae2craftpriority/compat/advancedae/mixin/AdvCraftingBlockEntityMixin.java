package com.yuuhamu.ae2craftpriority.compat.advancedae.mixin;

import com.yuuhamu.ae2craftpriority.compat.advancedae.AdvCraftingPriorityCpuHost;
import com.yuuhamu.ae2craftpriority.compat.advancedae.AdvancedAeMenuTypes;
import com.yuuhamu.ae2craftpriority.priority.CraftingPriorityHostMarker;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.common.entities.AdvCraftingBlockEntity;
import net.pedroksl.advanced_ae.gui.quantumcomputer.QuantumComputerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import appeng.helpers.IPriorityHost;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftingCPUMenu;

@Mixin(value = AdvCraftingBlockEntity.class, remap = false)
public abstract class AdvCraftingBlockEntityMixin
        implements IPriorityHost, CraftingPriorityHostMarker, AdvCraftingPriorityCpuHost {

    @Unique
    private AdvCraftingCPU ae2cp$activePriorityCpu;

    @Override
    public void ae2cp$setActivePriorityCpu(AdvCraftingCPU cpu) {
        this.ae2cp$activePriorityCpu = cpu;
    }

    @Override
    public AdvCraftingCPU ae2cp$getActivePriorityCpu() {
        return this.ae2cp$activePriorityCpu;
    }

    @Override
    public int getPriority() {
        return PriorityHolder.getPriorityOrDefault(this.ae2cp$activePriorityCpu);
    }

    @Override
    public void setPriority(int newValue) {
        if (this.ae2cp$activePriorityCpu != null) {
            PriorityHolder.setPriority(this.ae2cp$activePriorityCpu, newValue);
        }
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        var target = PriorityReturnTarget.take(player.getUUID());
        if (target != null && MenuOpener.returnTo(target.menuType(), player, target.locator())) {
            return;
        }

        MenuType<QuantumComputerMenu> quantumType = AdvancedAeMenuTypes.quantumComputerMenuTypeOrNull();
        if (quantumType != null
                && MenuOpener.returnTo(quantumType, player, MenuLocators.forBlockEntity(ae2cp$self()))) {
            return;
        }

        MenuOpener.returnTo(CraftingCPUMenu.TYPE, player, MenuLocators.forBlockEntity(ae2cp$self()));
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(ae2cp$self().getUnitBlock());
    }

    @Unique
    private AdvCraftingBlockEntity ae2cp$self() {
        return (AdvCraftingBlockEntity) (Object) this;
    }
}
