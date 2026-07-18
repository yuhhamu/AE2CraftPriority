package com.yuuhamu.ae2craftpriority.compat.advancedae.mixin;

import com.yuuhamu.ae2craftpriority.api.CraftPriorityApi;
import com.yuuhamu.ae2craftpriority.client.PriorityBackIconOverride;
import com.yuuhamu.ae2craftpriority.compat.advancedae.AdvancedAeMenuTypes;
import com.yuuhamu.ae2craftpriority.priority.CraftingStatusPriorityControl;
import com.yuuhamu.ae2craftpriority.priority.PriorityReturnTarget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.gui.quantumcomputer.QuantumComputerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.core.localization.GuiText;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.PriorityMenu;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftingCPUMenu;

/**
 * {@code QuantumComputerMenu}(Quantum Computerの個別CPU画面)は、{@code CraftingStatusMenu}
 * (クラフト状況のCPU一覧)と同じく1画面の中で複数タスク({@code AdvCraftingCPU})を切り替えられる
 * ({@code selectCpu}/{@code setCPU}、AdvancedAE自身の実装)。そのため優先度ボタンを素通しで
 * AE2バニラの {@code SwitchGuisPacket} に任せると、画面が今どのタスクを選択中かという情報が
 * 失われ、常にブロック位置だけで優先度画面が開いてしまう。
 *
 * <p>{@code CraftingStatusMenuMixin} と同じ {@link CraftingStatusPriorityControl} を実装し、
 * 画面を開く直前に選択中のタスクを {@link com.yuuhamu.ae2craftpriority.compat.advancedae.AdvCraftingPriorityCpuHost}
 * 経由でブロックエンティティへ渡す。</p>
 */
@Mixin(value = QuantumComputerMenu.class, remap = false)
public abstract class QuantumComputerMenuMixin extends CraftingCPUMenu implements CraftingStatusPriorityControl {

    private static final String ACTION_OPEN_PRIORITY = "ae2cp$openPriorityQc";

    private QuantumComputerMenuMixin(MenuType<?> menuType, int id, Inventory ip, Object te) {
        super(menuType, id, ip, te);
    }

    @Unique
    private ICraftingCPU ae2cp$currentCpu;

    @Shadow
    private int getOrAssignCpuSerial(ICraftingCPU cpu) {
        throw new UnsupportedOperationException();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ae2cp$onInit(CallbackInfo ci) {
        @SuppressWarnings("unchecked")
        MenuType<QuantumComputerMenu> type = (MenuType<QuantumComputerMenu>) this.getType();
        AdvancedAeMenuTypes.capture(type);
        registerClientAction(ACTION_OPEN_PRIORITY, this::ae2cp$openPrioritySettings);
    }

    @Inject(method = "setCPU", at = @At("HEAD"))
    private void ae2cp$onSetCPU(ICraftingCPU c, CallbackInfo ci) {
        this.ae2cp$currentCpu = c;
    }

    @Override
    public void ae2cp$openPrioritySettings() {
        if (isClientSide()) {
            // Quantum Computer個別画面から開いた場合、戻るタブは「個別CPU画面」のままが正しい
            // (CraftingStatusMenuとは異なり「クラフト状況」表示にはしない)。
            PriorityBackIconOverride.clear();
            sendClientAction(ACTION_OPEN_PRIORITY);
        } else {
            if (this.ae2cp$currentCpu == null) {
                return;
            }
            BlockEntity host = CraftPriorityApi.getPriorityHostBlockEntity(this.ae2cp$currentCpu);
            if (host == null) {
                return;
            }
            // このブロック(物理Quantum Computer本体)は複数タスクの代表になりうるため、
            // 今まさに編集しようとしているタスクを覚えさせる。
            CraftPriorityApi.prepareForPriorityEdit(this.ae2cp$currentCpu, host);
            var player = getPlayerInventory().player;
            var locator = getLocator();
            if (locator != null) {
                MenuType<QuantumComputerMenu> quantumType = AdvancedAeMenuTypes.quantumComputerMenuTypeOrNull();
                if (quantumType != null) {
                    PriorityReturnTarget.set(player.getUUID(), quantumType, locator);
                }
            }
            MenuOpener.open(PriorityMenu.TYPE, player, MenuLocators.forBlockEntity(host));
        }
    }

    @Redirect(
            method = "createCpuList",
            at = @At(value = "INVOKE", target = "Lnet/pedroksl/advanced_ae/common/cluster/AdvCraftingCPU;"
                    + "getName()Lnet/minecraft/network/chat/Component;"))
    private Component ae2cp$appendPriorityToName(AdvCraftingCPU cpu) {
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
