package com.yuuhamu.ae2craftpriority.mixin.craft;

import com.yuuhamu.ae2craftpriority.menu.CraftPriorityStepMenu;
import com.yuuhamu.ae2craftpriority.priority.PendingCraftPriority;
import com.yuuhamu.ae2craftpriority.priority.PriorityHolder;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.stacks.AEKey;
import appeng.menu.locator.MenuLocator;
import appeng.menu.me.crafting.CraftConfirmMenu;

@Mixin(value = CraftConfirmMenu.class, remap = false)
public abstract class CraftConfirmMenuMixin implements PriorityHolder {

    @Unique
    private int ae2cp$priority = PriorityHolder.DEFAULT_PRIORITY;

    @Shadow
    public boolean autoStart;

    @Redirect(
            method = "goBack",
            at = @At(value = "INVOKE", target = "Lappeng/menu/me/crafting/CraftAmountMenu;"
                    + "open(Lnet/minecraft/server/level/ServerPlayer;Lappeng/menu/locator/MenuLocator;"
                    + "Lappeng/api/stacks/AEKey;I)V"))
    private void ae2cp$backToPriorityStep(ServerPlayer player, MenuLocator locator, AEKey whatToCraft, int amount) {
        CraftPriorityStepMenu.open(player, locator, whatToCraft, amount, this.autoStart, this.ae2cp$getPriority());
    }

    @Override
    public int ae2cp$getPriority() {
        return this.ae2cp$priority;
    }

    @Override
    public void ae2cp$setPriority(int priority) {
        this.ae2cp$priority = priority;
    }

    @Inject(method = "startJob", at = @At("HEAD"))
    private void ae2cp$onStartJobHead(CallbackInfo ci) {
        PendingCraftPriority.set(this.ae2cp$priority);
    }

    @Inject(method = "startJob", at = @At("RETURN"))
    private void ae2cp$onStartJobReturn(CallbackInfo ci) {
        PendingCraftPriority.clear();
    }
}
