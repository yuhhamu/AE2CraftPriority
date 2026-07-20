package com.yuuhamu.ae2craftpriority.mixin.status;

import com.yuuhamu.ae2craftpriority.api.CraftPriorityApi;
import com.yuuhamu.ae2craftpriority.client.PriorityBackIconOverride;
import com.yuuhamu.ae2craftpriority.priority.CraftingCPUMenuGridAccess;
import com.yuuhamu.ae2craftpriority.priority.CraftingStatusPriorityControl;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.storage.ITerminalHost;
import appeng.core.localization.GuiText;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.PriorityMenu;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftingCPUMenu;
import appeng.menu.me.crafting.CraftingStatusMenu;

@Mixin(value = CraftingStatusMenu.class, remap = false)
public abstract class CraftingStatusMenuMixin extends CraftingCPUMenu implements CraftingStatusPriorityControl {

    private static final String ACTION_OPEN_PRIORITY = "ae2cp$openPriority";

    private CraftingStatusMenuMixin(MenuType<?> menuType, int id, Inventory ip, Object te) {
        super(menuType, id, ip, te);
    }

    @Unique
    private ICraftingCPU ae2cp$currentCpu;

    @Shadow
    private int getOrAssignCpuSerial(ICraftingCPU cpu) {
        throw new UnsupportedOperationException();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2cp$onInit(int id, Inventory ip, ITerminalHost host, CallbackInfo ci) {
        registerClientAction(ACTION_OPEN_PRIORITY, this::ae2cp$openPrioritySettings);
    }

    @Inject(method = "setCPU", at = @At("HEAD"))
    private void ae2cp$onSetCPU(ICraftingCPU c, CallbackInfo ci) {
        this.ae2cp$currentCpu = c;
    }

    @Unique
    private void ae2cp$ensureCpuSelected() {
        if (this.ae2cp$currentCpu != null) {
            return;
        }
        IGrid grid = CraftingCPUMenuGridAccess.getGridOrNull(this);
        if (grid == null) {
            return;
        }
        Set<ICraftingCPU> cpus = grid.getCraftingService().getCpus();
        if (cpus.isEmpty()) {
            return;
        }
        this.setCPU(cpus.iterator().next());
    }

    @Override
    public void ae2cp$openPrioritySettings() {
        if (isClientSide()) {

            PriorityBackIconOverride.set();
            sendClientAction(ACTION_OPEN_PRIORITY);
        } else {
            ae2cp$ensureCpuSelected();
            BlockEntity host = null;
            if (this.ae2cp$currentCpu instanceof CraftingCPUCluster cluster) {
                var it = cluster.getBlockEntities();
                if (it.hasNext()) {
                    host = it.next();
                }
            } else if (this.ae2cp$currentCpu != null) {

                host = CraftPriorityApi.getPriorityHostBlockEntity(this.ae2cp$currentCpu);
            }
            if (host != null) {

                CraftPriorityApi.prepareForPriorityEdit(this.ae2cp$currentCpu, host);
                var player = getPlayerInventory().player;
                var locator = getLocator();
                if (locator != null) {
                    PriorityReturnTarget.set(player.getUUID(), CraftingStatusMenu.TYPE, locator);
                }
                MenuOpener.open(PriorityMenu.TYPE, player, MenuLocators.forBlockEntity(host));
            }
        }
    }

    @Redirect(
            method = "createCpuList",
            at = @At(value = "INVOKE", target = "Lappeng/api/networking/crafting/ICraftingCPU;"
                    + "getName()Lnet/minecraft/network/chat/Component;"))
    private Component ae2cp$appendPriorityToName(ICraftingCPU cpu) {
        Component original = cpu.getName();
        if (!cpu.isBusy()) {
            return original;
        }
        Component base = original != null
                ? original
                : GuiText.CPUs.text().append(String.format(" #%d", this.getOrAssignCpuSerial(cpu)));
        return base.copy().append("@" + CraftPriorityApi.getPriority(cpu));
    }
}
