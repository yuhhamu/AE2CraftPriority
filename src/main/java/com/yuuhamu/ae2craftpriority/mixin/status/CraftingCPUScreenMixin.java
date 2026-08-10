package com.yuuhamu.ae2craftpriority.mixin.status;

import com.yuuhamu.ae2craftpriority.client.PriorityBackIconOverride;
import com.yuuhamu.ae2craftpriority.priority.CraftingStatusPriorityControl;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.neoforged.neoforge.network.PacketDistributor;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.core.localization.GuiText;
import appeng.core.network.serverbound.SwitchGuisPacket;
import appeng.menu.implementations.PriorityMenu;
import appeng.menu.me.crafting.CraftingCPUMenu;

@Mixin(value = CraftingCPUScreen.class, remap = false)
public abstract class CraftingCPUScreenMixin extends AEBaseScreen<CraftingCPUMenu> {

    private CraftingCPUScreenMixin(CraftingCPUMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Unique
    private IconButton ae2cp$priorityButton;

    // NOTE: an earlier version of this injection used at = @At("HEAD"), assuming Mixin's HEAD
    // selector for a constructor means "right after the mandatory super()/this() call". That
    // assumption was wrong and caused a client-crashing InvalidInjectionException at runtime:
    // "@At(\"HEAD\") selector @Inject handler before super() invocation must be static" - Mixin's
    // HEAD for a constructor is literally the very start of the method, i.e. BEFORE the super()
    // call even runs, where `this` is not yet fully constructed, so a non-static injector is
    // rejected outright. The correct way to inject immediately after the super() call finishes
    // is to target that exact INVOKE instruction with shift = At.Shift.AFTER, as done below.
    //
    // This still runs before CraftingCPUScreen's own body (table renderer, scrollbar, cancel/
    // suspend buttons, and the conditional "CPU selection mode" toggle button that only appears
    // when menu.allowConfiguration() is true) and before AdvancedAE's QuantumComputerScreen
    // subclass adds its own selection-mode button after its super() call returns. That keeps our
    // priority button consistently the 2nd toolbar entry (right after AEBaseScreen's own "?"
    // help button) regardless of which of those conditional buttons ends up present, instead of
    // varying between 2nd and 3rd depending on CPU type as it did when injecting at TAIL.
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lappeng/client/gui/AEBaseScreen;<init>(Lappeng/menu/AEBaseMenu;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;Lappeng/client/gui/style/ScreenStyle;)V", shift = At.Shift.AFTER))
    private void ae2cp$onInit(CraftingCPUMenu menu, Inventory playerInventory, Component title, ScreenStyle style,
            CallbackInfo ci) {
        this.ae2cp$priorityButton = new IconButton(btn -> ae2cp$openPriority()) {
            @Override
            protected Icon getIcon() {
                return Icon.PRIORITY;
            }
        };
        this.ae2cp$priorityButton.setMessage(GuiText.Priority.text());
        this.addToLeftToolbar(this.ae2cp$priorityButton);
    }

    @Unique
    private void ae2cp$openPriority() {
        if (this.menu instanceof CraftingStatusPriorityControl control) {

            control.ae2cp$openPrioritySettings();
        } else {

            PriorityBackIconOverride.clear();
            PacketDistributor.sendToServer(SwitchGuisPacket.openSubMenu(PriorityMenu.TYPE));
        }
    }
}
