package com.yuuhamu.ae2craftpriority.mixin.status;

import com.yuuhamu.ae2craftpriority.client.PriorityBackIconOverride;
import com.yuuhamu.ae2craftpriority.priority.CraftingStatusPriorityControl;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.client.gui.Icon;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.SwitchGuisPacket;
import appeng.menu.implementations.PriorityMenu;
import appeng.menu.me.crafting.CraftingCPUMenu;

@Mixin(value = CraftingCPUScreen.class, remap = false)
public abstract class CraftingCPUScreenMixin extends AbstractContainerScreen<CraftingCPUMenu> {

    private CraftingCPUScreenMixin(CraftingCPUMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Shadow
    @Final
    private Button cancel;

    @Unique
    private IconButton ae2cp$priorityButton;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ae2cp$onInit(CraftingCPUMenu menu, Inventory playerInventory, Component title, ScreenStyle style,
            CallbackInfo ci) {
        this.ae2cp$priorityButton = new IconButton(btn -> ae2cp$openPriority()) {
            @Override
            protected Icon getIcon() {
                return Icon.WRENCH;
            }
        };
        this.ae2cp$priorityButton.setMessage(GuiText.Priority.text());
    }

    @Unique
    private void ae2cp$openPriority() {
        if (this.menu instanceof CraftingStatusPriorityControl control) {
            // 戻るタブの表示切り替え(クラフト状況 / 個別CPU画面)は各実装(
            // CraftingStatusMenuMixin / QuantumComputerMenuMixin)が自分自身の責務として行う。
            control.ae2cp$openPrioritySettings();
        } else {
            // 個別CPU画面: 戻るタブはCPUブロックのままが正しいためフラグはクリア
            PriorityBackIconOverride.clear();
            NetworkHandler.instance().sendToServer(SwitchGuisPacket.openSubMenu(PriorityMenu.TYPE));
        }
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"))
    private void ae2cp$onUpdateBeforeRender(CallbackInfo ci) {
        if (this.ae2cp$priorityButton == null) {
            return;
        }
        if (!this.children().contains(this.ae2cp$priorityButton)) {
            this.addRenderableWidget(this.ae2cp$priorityButton);
        }
        this.ae2cp$priorityButton.setX(this.cancel.getX() - 4 - 16);
        this.ae2cp$priorityButton.setY(this.cancel.getY() + (this.cancel.getHeight() - 16) / 2);
    }
}
